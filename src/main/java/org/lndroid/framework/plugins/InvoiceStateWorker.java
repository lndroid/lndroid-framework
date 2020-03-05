package org.lndroid.framework.plugins;

import android.util.Log;
import android.util.Pair;

import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class InvoiceStateWorker implements IPluginBackground {

    public interface IDao {
        WalletData.Invoice getInvoiceByHash(String hashHex);
        List<WalletData.InvoiceHTLC> getInvoiceHTLCs(long invoiceId);
        List<WalletData.Payment> getInvoicePayments(long invoiceId);
        long getMaxAddIndex();
        long getMaxSettleIndex();
        void updateInvoiceState(WalletData.Invoice invoice,
                                List<WalletData.InvoiceHTLC> htlcs,
                                List<WalletData.Payment> payments);
    }

    private static final String TAG = "InvoiceStateWorker";

    private IPluginServer server_;
    private IDao dao_;
    private ILightningDao lnd_;
    private IPluginBackgroundCallback engine_;
    private boolean started_;

    @Override
    public String id() {
        return DefaultPlugins.INVOICE_STATE_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        server_ = server;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
        engine_ = callback;
    }

    private void onUpdate(Data.Invoice r) {
        // get existing invoice from db
        WalletData.Invoice invoice = dao_.getInvoiceByHash(LightningCodec.bytesToHex(r.rHash));

        // no invoice? it probably was keysend, generate an invoice now
        if (invoice == null) {
            WalletData.Invoice.Builder b = WalletData.Invoice.builder();
            b.setId(server_.getIdGenerator().generateId(WalletData.Invoice.class));
            invoice = b.build();
        }

        // merge updates
        WalletData.Invoice.Builder b = invoice.toBuilder();
        LightningCodec.InvoiceConverter.decode(r, b);
        invoice = b.build();

        // collect existing htlcs
        List<WalletData.InvoiceHTLC> htlcs = dao_.getInvoiceHTLCs(invoice.id());
        Map<Pair<Long, Long>, WalletData.InvoiceHTLC> htlcsMap = new HashMap<>();
        for (WalletData.InvoiceHTLC htlc: htlcs) {
            htlcsMap.put(new Pair<>(htlc.chanId(), htlc.htlcIndex()), htlc);
        }
        htlcs.clear();

        // collect existing payments
        List<WalletData.Payment> payments = dao_.getInvoicePayments(invoice.id());
        Map<Long, WalletData.Payment> paymentsMap = new HashMap<>();
        for (WalletData.Payment p: payments) {
            paymentsMap.put(p.sourceHTLCId(), p);
        }
        payments.clear();

        // parse new htlcs
        for(Data.InvoiceHTLC rh: r.htlcs) {
            Pair<Long, Long> id = new Pair<>(rh.chanId, rh.htlcIndex);

            // htlc exists?
            WalletData.InvoiceHTLC htlc = htlcsMap.get(id);
            WalletData.Payment payment = null;

            // ensure htlc
            if (htlc == null) {
                // create new one
                htlc = WalletData.InvoiceHTLC.builder()
                        .setId(server_.getIdGenerator().generateId(WalletData.InvoiceHTLC.class))
                        .setInvoiceId(invoice.id())
                        .build();
                htlcsMap.put(id, htlc);
            } else {
                payment = paymentsMap.get(htlc.id());
            }

            // merge htlc update
            WalletData.InvoiceHTLC.Builder hb = htlc.toBuilder();
            LightningCodec.InvoiceHTLCConverter.decode(rh, hb);
            htlc = hb.build();

            // ensure htlc payment
            if (payment == null) {
                payment = WalletData.Payment.builder()
                        .setId(server_.getIdGenerator().generateId(WalletData.Payment.class))
                        .setType(WalletData.PAYMENT_TYPE_INVOICE)
                        .setSourceId(invoice.id())
                        .setSourceHTLCId(htlc.id())
                        .setUserId(invoice.userId())
                        .build();
                paymentsMap.put(payment.id(), payment);
            }

            // update payment
            payment = payment.toBuilder()
                    .setMessage(htlc.message())
                    .setPeerPubkey(htlc.senderPubkey())
                    .setTime(htlc.senderTime() != 0 ? htlc.senderTime() : htlc.acceptTime())
                    .build();

            // update invoice,
            // FIXME many messages per invoice are possible!
            if (htlc.message() != null || htlc.senderPubkey() != null) {
                invoice = invoice.toBuilder()
                        .setMessage(htlc.message())
                        .setSenderPubkey(htlc.senderPubkey())
                        .build();
            }

            // move back to list
            htlcs.add(htlc);
            payments.add(payment);
        }

        dao_.updateInvoiceState(invoice, htlcs, payments);

        engine_.onSignal(id(), DefaultTopics.INVOICE_STATE, null);
    }

    @Override
    public void work() {
        if (!lnd_.isRpcReady())
            return;

        if (started_)
            return;

        started_ = true;

        Data.InvoiceSubscription s = new Data.InvoiceSubscription();
        // FIXME current implementations will break if wallet is recovered from backup...
        s.addIndex = dao_.getMaxAddIndex();
        s.settleIndex = dao_.getMaxSettleIndex();
        lnd_.client().subscribeInvoicesStream(s, new ILightningCallback<Data.Invoice>() {

            @Override
            public void onResponse(Data.Invoice invoice) {
                Log.e(TAG, "subscribe invoices update ai "+invoice.addIndex+" si "+invoice.settleIndex);
                onUpdate(invoice);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "subscribe invoices error "+i+" err "+s);
                // FIXME might return 'retry later' if starting?
                throw new RuntimeException("SubscribeInvoices failed");
            }
        });
    }

    @Override
    public void auth(WalletData.AuthRequest ar, WalletData.AuthResponse r) {
        throw new RuntimeException("Unexpected auth");
    }

    @Override
    public boolean isUserPrivileged(WalletData.User user, String requestType) {
        throw new RuntimeException("Unexpected priv check");
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        // noop
    }

    @Override
    public void notify(String topic, Object data) {
        // noop
    }
}
