package org.lndroid.framework.plugins;

import android.content.ComponentName;
import android.util.Log;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

import java.io.IOException;
import java.util.List;

public class SubscribeNewPaidInvoices implements IPluginForeground {

    public interface IDao {
        WalletData.Payment getPayment(String protocol, long invoiceId);
        List<WalletData.Payment> getPayments(String protocol);

        // get all non-committed sessions
        List<Transaction<WalletData.SubscribeNewPaidInvoicesRequest>> getTransactions();

        // start tx
        void startTransaction(Transaction<WalletData.SubscribeNewPaidInvoicesRequest> t);

        // mark as rejected
        void rejectTransaction(long txUserId, String txId, long txAuthUserId);

        // mark as timed out
        void timeoutTransaction(long txUserId, String txId);

    }

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IPluginForegroundCallback engine_;
    private IDao dao_;

    @Override
    public String id() {
        return DefaultPlugins.SUBSCRIBE_NEW_PAID_INVOICES;
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        engine_ = callback;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());

        // restore active transactions
        List<Transaction<WalletData.SubscribeNewPaidInvoicesRequest>> txs = dao_.getTransactions();
        for(Transaction<WalletData.SubscribeNewPaidInvoicesRequest> tx: txs) {
            PluginContext ctx = new PluginContext();
            ctx.txId = tx.tx.txId;
            ctx.deadline = tx.tx.deadlineTime;
            ctx.request = tx.request;
            if (tx.request.componentClassName() != null && tx.request.componentPackageName() != null) {
                ctx.broadcastComponent = new ComponentName(
                        tx.request.componentPackageName(), tx.request.componentClassName());
            }

            // invalid? skip
            if (!engine_.onInit(id(), tx.tx.userId, ctx))
                continue;

            // recover tx state
            if (ctx.authRequest != null) {
                // - tx not executed, auth request created
                // wait for auth
            } else if (isUserPrivileged(ctx, ctx.user)) {
                // - tx not executed, auth not required,
                sendAll(ctx, tx.request);
            } else if (tx.request.noAuth()) {
                dao_.rejectTransaction(ctx.user.id(), ctx.txId, 0);
                engine_.onError(id(), ctx, Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
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

    private void sendAll(PluginContext ctx, WalletData.SubscribeNewPaidInvoicesRequest req) {
        List<WalletData.Payment> ps = dao_.getPayments(req.protocolExtension());
        for (WalletData.Payment p: ps)
            engine_.onReply(id(), ctx, p, WalletData.Payment.class);
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {

        // we don't try to reuse existing tx and simply update one if
        // it exists as this read operation is already idempotent and
        // the only reason we write a tx is to restore it on restart
        // and not forger the broadcast receiver component of the client

        in.assignDataType(WalletData.SubscribeNewPaidInvoicesRequest.class);
        WalletData.SubscribeNewPaidInvoicesRequest req = null;
        try {
            req = in.getData();
        } catch (IOException e) {}
        if (req == null) {
            engine_.onError(id(), ctx, Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
            return;
        }


        Transaction<WalletData.SubscribeNewPaidInvoicesRequest> tx = new Transaction<>();
        tx.tx = new Transaction.TransactionData(id(), ctx.user.id(), ctx.txId);
        tx.request = req;
        tx.tx.createTime = System.currentTimeMillis();
        long timeout = (int)ctx.timeout;
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        tx.tx.deadlineTime = tx.tx.createTime + timeout;

        // store req and deadline in the context
        ctx.request = req;
        ctx.deadline = tx.tx.deadlineTime;

        // set broadcast receiver to the context so that server could wake the client up
        if (req.componentClassName() != null && req.componentPackageName() != null) {
            ctx.broadcastComponent = new ComponentName(
                    req.componentPackageName(), req.componentClassName());
        }

        // store tx to be able to restore it
        dao_.startTransaction(tx);

        if (!isUserPrivileged(ctx, ctx.user)) {
            // tell engine we need user auth
            if (req.noAuth())
                engine_.onError(id(), ctx, Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
            else
                engine_.onAuth(id(), ctx);
        } else {
            // send all active payments upon subscription
            sendAll(ctx, req);
        }
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        engine_.onError(id(), ctx, Errors.PLUGIN_PROTOCOL, Errors.errorMessage(Errors.PLUGIN_PROTOCOL));
    }

    @Override
    public void stop(PluginContext ctx) {
        engine_.onDone(id(), ctx, true);
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse r) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void timeout(PluginContext ctx) {
        // reject
        dao_.timeoutTransaction(ctx.user.id(), ctx.txId);

        engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        WalletData.SubscribeNewPaidInvoicesRequest req = (WalletData.SubscribeNewPaidInvoicesRequest)ctx.request;
        // FIXME only messages are supported atm
        return user.isRoot() || WalletData.PROTOCOL_MESSAGES.equals(req.protocolExtension());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.INVOICE_STATE);
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        WalletData.Invoice in = (WalletData.Invoice)data;
        if (in == null)
            return;

        WalletData.SubscribeNewPaidInvoicesRequest req = (WalletData.SubscribeNewPaidInvoicesRequest)ctx.request;
        WalletData.Payment p = dao_.getPayment(req.protocolExtension(), in.id());
        if (p == null)
            return;

        engine_.onReply(id(), ctx, p, WalletData.Payment.class);
    }
}

