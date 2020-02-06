package org.lndroid.framework.plugins;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.ISendPaymentDao;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.lnd.LightningCodec;

// Job
public class SendPayment implements IPluginForeground {

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    ISendPaymentDao dao_;
    IPluginForegroundCallback engine_;

    public SendPayment() {
    }

    @Override
    public String id() {
        return DefaultPlugins.SEND_PAYMENT;
    }

    private boolean isUserPrivileged(WalletData.User user, WalletData.SendPaymentRequest req) {
        return user.isRoot() || dao_.hasPrivilege(req, user);
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        dao_ = (ISendPaymentDao) server.getDaoProvider().getPluginDao(id());
        engine_ = callback;

        // restore active transactions
        List<Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment>> txs = dao_.getTransactions();
        for(Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> tx: txs) {
            PluginContext ctx = new PluginContext();
            ctx.txId = tx.txId;
            ctx.deadline = tx.deadlineTime;

            // invalid? skip
            if (!engine_.onInit(id(), tx.userId, ctx))
                continue;

            // cache request in ctx
            ctx.request = tx.request;

            if (isUserPrivileged(ctx.user, tx.request)) {
                // - tx not executed, auth not required,
                commit(ctx, 0);
            } else if (ctx.authRequest != null) {
                // - tx not executed, auth required, auth request created
                // wait for auth
            } else {
                // - tx not executed, auth required, auth request not created
                engine_.onAuth(id(), ctx);
            }
        }
    }

    @Override
    public void work() {
        // noop
    }

    private WalletData.Payment createResponse(PluginContext ctx, WalletData.SendPaymentRequest req, int authUserId) {
        WalletData.Contact c = null;
        if (req.contactId() != 0)
            c = dao_.getContact(req.contactId());

        WalletData.SendPayment sp = WalletData.SendPayment.builder()
                .setUserId(ctx.user.id())
                .setTxId(ctx.txId)
                .setAuthUserId(authUserId)
                .setPurpose(req.purpose())
                .setInvoiceDescription(req.invoiceDescription())
                .setInvoiceDescriptionHashHex(req.invoiceDescriptionHashHex())
                .setInvoiceTimestamp(req.invoiceTimestamp())
                .setInvoiceFallbackAddr(req.invoiceFallbackAddr())
                .setMaxTryTime(req.expiry())
                .setMaxTries(req.maxTries())
                .setDestPubkey(req.destPubkey() != null
                        ? req.destPubkey()
                        : (c != null ? c.pubkey() : null)
                )
                .setValueMsat(req.valueSat() * 1000)
                .setPaymentHashHex(req.paymentHashHex())
                .setPaymentRequest(req.paymentRequest())
                .setFinalCltvDelta(req.finalCltvDelta())
                .setFeeLimitFixedMsat(req.feeLimitFixedMsat())
                .setFeeLimitPercent(req.feeLimitPercent())
                .setOutgoingChanId(req.outgoingChanId())
                .setCltvLimit(req.cltvLimit())
                .setCreateTime(System.currentTimeMillis())
                .setContactPubkey(c != null ? c.pubkey() : null)
                .setMessage(req.message())
                .setSenderPubkey(req.includeSenderPubkey() ? dao_.walletPubkey() : null)
                .setIsKeysend(req.isKeysend())
                .setRouteHints(req.routeHints())
                .setFeatures(req.features())
                .build();

        ImmutableMap.Builder<Long,WalletData.SendPayment> spb = ImmutableMap.builder();
        spb.put(0L, sp);

        return WalletData.Payment.builder()
                .setType(WalletData.PAYMENT_TYPE_SENDPAYMENT)
                .setUserId(sp.userId())
                .setTime(sp.createTime())
                .setSendPayments(spb.build())
                .setPeerPubkey(req.destPubkey())
                .setMessage(req.message())
                .build();
    }

    private void commit(PluginContext ctx, int authUserId) {
        // convert request to response
        WalletData.Payment p = createResponse(ctx, (WalletData.SendPaymentRequest)ctx.request, authUserId);

        // store response in finished tx
        p = dao_.commitTransaction(ctx.user.id(), ctx.txId, authUserId, p);

        // notify other plugins
        PluginData.PluginNotification n = new PluginData.PluginNotification();
        n.pluginId = id();
        n.entityId = p.sourceId();
        engine_.onSignal(id(), DefaultTopics.NEW_SEND_PAYMENT, n);
        engine_.onSignal(id(), DefaultTopics.SEND_PAYMENT_STATE, n);

        // response to client
        engine_.onReply(id(), ctx, p.sendPayments().entrySet().iterator().next().getValue(), WalletData.SendPayment.class);
        engine_.onDone(id(), ctx);
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {

        // recover if tx already executed
        Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> tx = dao_.getTransaction(ctx.user.id(), ctx.txId);
        if (tx != null){
            if (tx.doneTime != 0) {
                if (tx.response != null) {
                    engine_.onReply(id(), ctx, tx.response, WalletData.SendPayment.class);
                    engine_.onDone(id(), ctx);
                } else {
                    engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
                }
            } else {
                if (ctx.authRequest != null)
                    engine_.onAuth(id(), ctx);
            }
            return;
        }

        in.assignDataType(WalletData.SendPaymentRequest.class);
        WalletData.SendPaymentRequest req = null;
        try {
            req = in.getData();
        } catch (IOException e) {
        }
        if (req == null) {
            engine_.onError(id(), ctx, Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
            return;
        }

        if (req.paymentHashHex() != null && LightningCodec.hexToBytes(req.paymentHashHex()) == null) {
            engine_.onError(id(), ctx, Errors.PLUGIN_INPUT, Errors.errorMessage(Errors.PLUGIN_INPUT));
            return;
        }

        // create tx
        tx = new Transaction<>();
        tx.userId = ctx.user.id();
        tx.txId = ctx.txId;
        tx.request = req;
        tx.createTime = System.currentTimeMillis();
        int timeout = (int)ctx.timeout;
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        } else if (timeout > MAX_TIMEOUT) {
            timeout = MAX_TIMEOUT;
        }
        tx.deadlineTime = tx.createTime + timeout;

        // set ctx deadline for engine to enforce it
        ctx.deadline = tx.deadlineTime;

        // cache request in ctx
        ctx.request = tx.request;

        // store tx to be able to restore it
        dao_.startTransaction(tx);

        if (!isUserPrivileged(ctx.user, tx.request)) {
            if (req.noAuth()) {
                engine_.onError(id(), ctx, Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
            } else {
                // tell engine we need user auth
                engine_.onAuth(id(), ctx);
            }
            return;
        }

        // no need for auth, commit right now
        commit(ctx, 0);
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        throw new RuntimeException("Bad input");
    }

    @Override
    public void stop(PluginContext ctx) {
        throw new RuntimeException("Bad stop");
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse r) {

        if (r.authorized()) {
            // commit after authed
            commit(ctx, r.authUserId());
        } else {
            // finish tx
            dao_.rejectTransaction(ctx.user.id(), ctx.txId, r.authUserId());
            // authorized response
            engine_.onError(id(), ctx, Errors.REJECTED, Errors.errorMessage(Errors.REJECTED));
        }

        return null;
    }

    @Override
    public void timeout(PluginContext ctx) {
        // finish tx
        dao_.timeoutTransaction(ctx.user.id(), ctx.txId);
        // authorized response
        engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        return isUserPrivileged(user, dao_.getTransaction(ctx.user.id(), ctx.txId).request);
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        // noop
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        // noop
    }
}
