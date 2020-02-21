package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.dao.IWalletInfoDao;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class WalletInfoWorker implements IPluginBackground {

    public interface IDao extends IWalletInfoDao{};

    private static final String TAG = "WalletInfoWorker";
    private static final long UPDATE_INTERVAL = 10000; // 10 sec

    private IPluginBackgroundCallback engine_;
    private IDao dao_;
    private ILightningDao lnd_;
    private boolean updating_;
    private boolean refresh_;
    private long nextUpdateTime_;

    @Override
    public String id() {
        return DefaultPlugins.WALLET_INFO_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        engine_ = callback;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
    }

    private void reschedule() {
        nextUpdateTime_ = System.currentTimeMillis() + UPDATE_INTERVAL;
        updating_ = false;
    }

    private void onUpdate(WalletData.WalletInfo b) {
        dao_.update(b);
        engine_.onSignal(id(), DefaultTopics.WALLET_INFO, null);
    }

    @Override
    public void work() {
        // already executing
        if (updating_)
            return;

        // if timer hasn't expired and we're not forced to refresh
        if (!refresh_ && nextUpdateTime_ > System.currentTimeMillis())
            return;

        // lnd must be unlocked
       if (!lnd_.isRpcReady())
           return;

        // reset now, so that if new refresh comes while we're executing,
        // we would restart immediately
        refresh_ = false;

        // mark, to avoid sending multiple requests
        updating_ = true;

        Data.GetInfoRequest r = new Data.GetInfoRequest();
        lnd_.client().getInfo(r, new ILightningCallback<Data.GetInfoResponse>() {
            @Override
            public void onResponse(Data.GetInfoResponse r) {
                WalletData.WalletInfo.Builder b = WalletData.WalletInfo.builder();
                LightningCodec.WalletInfoConverter.decode(r, b);
                onUpdate(b.build());
                reschedule();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "Failed to get wallet info, code "+i+" err "+s);
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
        // we publish to this topic, and we also subscribe to it
        // so that operations that would cause wallet info update
        // could notify us
        topics.add(DefaultTopics.WALLET_INFO);
    }

    @Override
    public void notify(String topic, Object data) {
        // update immediately
        refresh_ = true;
    }
}
