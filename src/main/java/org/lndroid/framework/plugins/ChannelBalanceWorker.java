package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.dao.IChannelBalanceDao;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class ChannelBalanceWorker implements IPluginBackground {

    private static final String TAG = "ChannelBalanceWorker";
    private static final long UPDATE_INTERVAL = 10000; // 10 sec

    // lnd doesn't update it's balance immediately after payments
    // are processed, so we add this delay to signal processing for
    // smoother UX
    private static final long SYNC_DELAY = 1000;

    private IPluginBackgroundCallback engine_;
    private IChannelBalanceDao dao_;
    private ILightningDao lnd_;
    private boolean updating_;
    private boolean refresh_;
    private long nextUpdateTime_;

    @Override
    public String id() {
        return DefaultPlugins.CHANNEL_BALANCE_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        engine_ = callback;
        dao_ = (IChannelBalanceDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
    }

    private void reschedule() {
        nextUpdateTime_ = System.currentTimeMillis() + (refresh_ ? SYNC_DELAY : UPDATE_INTERVAL);
        updating_ = false;
    }

    private void onUpdate(WalletData.ChannelBalance b) {
        Log.i(TAG, "channel balance update "+b);
        dao_.update(b);
        engine_.onSignal(id(), DefaultTopics.CHANNEL_BALANCE, null);
    }

    @Override
    public void work() {

        // already executing
        if (updating_)
            return;

        // if timer hasn't expired and we're not forced to refresh
        if (nextUpdateTime_ > System.currentTimeMillis())
            return;

        // lnd must be unlocked
        if (!lnd_.isRpcReady())
            return;

        // reset now, so that if new refresh comes while we're executing,
        // we would restart immediately
        refresh_ = false;

        // mark, to avoid sending multiple requests
        updating_ = true;

        Data.ChannelBalanceRequest r = new Data.ChannelBalanceRequest();
        lnd_.client().channelBalance(r, new ILightningCallback<Data.ChannelBalanceResponse>() {
            @Override
            public void onResponse(Data.ChannelBalanceResponse r) {
                Log.i(TAG, "channel balance update "+r);
                WalletData.ChannelBalance.Builder b = WalletData.ChannelBalance.builder();
                LightningCodec.ChannelBalanceConverter.decode(r, b);
                onUpdate(b.build());
                reschedule();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "Failed to get channel balance, code "+i+" err "+s);
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
        topics.add(DefaultTopics.CHANNEL_BALANCE);
        topics.add(DefaultTopics.INVOICE_STATE);
        topics.add(DefaultTopics.SEND_PAYMENT_STATE);
    }

    @Override
    public void notify(String topic, Object data) {
        refresh_ = true;
        // give lnd some time to sync channel balance after recent activity
        nextUpdateTime_ = System.currentTimeMillis() + SYNC_DELAY;
    }
}
