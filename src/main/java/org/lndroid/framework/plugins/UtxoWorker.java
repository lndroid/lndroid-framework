package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IUtxoWorkerDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;
import org.lndroid.lnd.daemon.ILightningCallback;

import java.util.List;

import lnrpc.Rpc;

public class UtxoWorker implements IPluginBackground {

    private static final String TAG = "UtxoWorker";
    private static final long UPDATE_INTERVAL = 60000; // 60 sec

    private IPluginServer server_;
    private IPluginBackgroundCallback engine_;
    private IUtxoWorkerDao dao_;
    private ILightningDao lnd_;
    private boolean updating_;
    private boolean refresh_;
    private long nextUpdateTime_;

    @Override
    public String id() {
        return DefaultPlugins.UTXO_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        server_ = server;
        engine_ = callback;
        dao_ = (IUtxoWorkerDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
    }

    private void reschedule() {
        nextUpdateTime_ = System.currentTimeMillis() + UPDATE_INTERVAL;
        updating_ = false;
    }

    private void onUpdate(WalletData.Utxo b) {
    }

    @Override
    public void work() {

        // already executing
        if (updating_)
            return;

        // if timer hasn't expired and we're not forced to refresh
        if (!refresh_ && nextUpdateTime_ > System.currentTimeMillis())
            return;

        // lnd must be ready
        if (!lnd_.isRpcReady())
            return;

        // reset now, so that if new refresh comes while we're executing,
        // we would restart immediately
        refresh_ = false;

        // mark, to avoid sending multiple requests
        updating_ = true;

        Rpc.ListUnspentRequest r = Rpc.ListUnspentRequest.newBuilder().build();
        lnd_.client().listUnspent(r, new ILightningCallback<Rpc.ListUnspentResponse>() {
            @Override
            public void onResponse(Rpc.ListUnspentResponse r) {
                Log.i(TAG, "list unspent update "+r);
                boolean updated = false;
                for(Rpc.Utxo u: r.getUtxosList()) {
                    WalletData.Utxo.Builder b = WalletData.Utxo.builder();
                    LightningCodec.UtxoConverter.decode(u, b);
                    WalletData.Utxo existing = dao_.getByOutpoint(b.txidHex(), b.outputIndex());

                    if (existing == null) {
                        b.setId(server_.getIdGenerator().generateId(WalletData.Utxo.class));
                    } else {
                        // make sure ids match
                        b.setId(existing.id());
                    }

                    // build
                    WalletData.Utxo utxo = b.build();

                    // number of confirmations might change
                    if (existing == null || !existing.equals(utxo)) {
                        dao_.update(utxo);
                        updated = true;
                    }
                }

                if (updated)
                    engine_.onSignal(id(), DefaultTopics.UTXO_STATE, null);

                reschedule();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "Failed to get utxo list, code "+i+" err "+s);
                reschedule();
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
        topics.add(DefaultTopics.TRANSACTION_STATE);
    }

    @Override
    public void notify(String topic, Object data) {
        refresh_ = true;
    }
}

