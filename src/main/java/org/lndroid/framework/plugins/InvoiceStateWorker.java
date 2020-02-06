package org.lndroid.framework.plugins;

import android.util.Log;
import android.util.Pair;

import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.IInvoiceStateWorkerDao;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class InvoiceStateWorker implements IPluginBackground {
    private static final String TAG = "InvoiceStateWorker";

    private IInvoiceStateWorkerDao dao_;
    private ILightningDao lnd_;
    private IPluginBackgroundCallback engine_;
    private boolean started_;

    @Override
    public String id() {
        return DefaultPlugins.INVOICE_STATE_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        dao_ = (IInvoiceStateWorkerDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
        engine_ = callback;
    }

    private void onUpdate(Data.Invoice r) {
        // get existing invoice from db
        WalletData.Invoice invoice = dao_.getInvoiceByHash(LightningCodec.bytesToHex(r.rHash));

        // no invoice? it probably was keysend, generate an invoice now
        if (invoice == null) {
            WalletData.Invoice.Builder b = WalletData.Invoice.builder();
            LightningCodec.InvoiceConverter.decode(r, b);
            invoice = b.build();
            final long invoiceId = dao_.insertInvoice(invoice);
            invoice = invoice.toBuilder().setId(invoiceId).build();
        } else {
            WalletData.Invoice.Builder b = invoice.toBuilder();
            LightningCodec.InvoiceConverter.decode(r, b);
            invoice = b.build();
        }

        // collect existing htlcs
        List<WalletData.InvoiceHTLC> htlcs = dao_.getInvoiceHTLCs(invoice.id());
        Map<Pair<Long, Long>, WalletData.InvoiceHTLC> map = new HashMap<>();
        for (WalletData.InvoiceHTLC htlc: htlcs) {
            map.put(new Pair<>(htlc.chanId(), htlc.htlcIndex()), htlc);
        }
        htlcs.clear();

        // parse new htlcs
        for(Data.InvoiceHTLC rh: r.htlcs) {
            Pair<Long, Long> id = new Pair<>(rh.chanId, rh.htlcIndex);

            // htlc exists?
            WalletData.InvoiceHTLC htlc = map.get(id);
            if (htlc == null) {
                // create new one
                htlc = WalletData.InvoiceHTLC.builder()
                        .setInvoiceId(invoice.id()).build();
                map.put(id, htlc);
            }

            // merge htlc update
            WalletData.InvoiceHTLC.Builder hb = htlc.toBuilder();
            LightningCodec.InvoiceHTLCConverter.decode(rh, hb);

            // move back to list
            htlcs.add(hb.build());
        }

        if (invoice.state() == WalletData.INVOICE_STATE_SETTLED) {
            // create a payment template to be used for
            // populating a Payment per HTLC
            WalletData.Payment p = WalletData.Payment.builder()
                    .setType(WalletData.PAYMENT_TYPE_INVOICE)
                    .setSourceId(invoice.id())
                    .setUserId(invoice.userId())
                    .build();
            dao_.settleInvoice(invoice, htlcs, p);
        } else {
            dao_.updateInvoiceState(invoice, htlcs);
        }

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
