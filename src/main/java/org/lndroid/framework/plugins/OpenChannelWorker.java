package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.dao.IOpenChannelWorkerDao;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class OpenChannelWorker implements IPluginBackground {

    private static final String TAG = "OpenChannelWorker";
    private static final long DEFAULT_EXPIRY = 3600000; // 1h
    private static final long TRY_INTERVAL = 60000; // 1m
    private static final long WORK_INTERVAL = 10000; // 10sec

    private IPluginBackgroundCallback engine_;
    private IOpenChannelWorkerDao dao_;
    private ILightningDao lnd_;
    private boolean notified_;
    private long nextWorkTime_;

    @Override
    public String id() {
        return DefaultPlugins.OPEN_CHANNEL_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        engine_ = callback;
        dao_ = (IOpenChannelWorkerDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();

        // NOTE: start protocol to recover in-flight state:
        // - get all records w/ 'opening' state from db
        // - mark as Lost, let StateWorker sync w/ lnd and
        //   either update this channel to proper state, or set state to RETRY
        List<WalletData.Channel> openingChannels = dao_.getOpeningChannels();
        for (WalletData.Channel c : openingChannels) {
            dao_.updateChannel(c.toBuilder().setState(WalletData.CHANNEL_STATE_LOST).build());
        }
    }

    private void onUpdate(WalletData.Channel c) {
        dao_.updateChannel(c);
        engine_.onSignal(id(), DefaultTopics.CHANNEL_STATE, null);
    }

    private void onFailed(WalletData.Channel c, WalletData.Channel.Builder b) {
        // FIXME check if it's permanent failure or not

        int tries = c.tries();
        tries++;

        if (tries >= c.maxTries() || System.currentTimeMillis() > c.maxTryTime()) {
            b.setState(WalletData.CHANNEL_STATE_FAILED);
        } else {
            b.setNextTryTime(System.currentTimeMillis() + TRY_INTERVAL);
            b.setState(WalletData.CHANNEL_STATE_NEW);
        }
    }

    private void onLndError(WalletData.Channel uc, String message) {
        WalletData.Channel.Builder b = uc.toBuilder();
        b.setErrorCode(Errors.LND_ERROR);
        b.setErrorMessage(message);

        onFailed(uc, b);

        // write
        onUpdate(b.build());
    }

    private void openChannel(WalletData.Channel c) {

        WalletData.Channel.Builder b = c.toBuilder();

        // convert to lnd request
        Data.OpenChannelRequest r = new Data.OpenChannelRequest();

        // bad payment?
        if (!LightningCodec.OpenChannelCodec.encode(c, r)) {
            Log.e(TAG, "open channel error bad request");
            b.setErrorCode(Errors.PLUGIN_INPUT);
            b.setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT));
            b.setState(WalletData.CHANNEL_STATE_FAILED);
            onUpdate(b.build());
            return;
        }

        // mark as opening
        b.setState(WalletData.CHANNEL_STATE_OPENING);
        b.setLastTryTime(System.currentTimeMillis());

        // ensure deadline
        if (c.maxTryTime() == 0) {
            b.setMaxTryTime(c.lastTryTime() + DEFAULT_EXPIRY);
        }

        final WalletData.Channel uc = b.build();

        // write opening state
        onUpdate(uc);

        // send
        lnd_.client().openChannel(r, new ILightningCallback<Data.ChannelPoint>() {
            @Override
            public void onResponse(Data.ChannelPoint r) {
                Log.i(TAG, "open channel response "+r);
                WalletData.Channel.Builder b = uc.toBuilder();
                LightningCodec.OpenChannelCodec.decode(r, b);
                b.setState(WalletData.CHANNEL_STATE_PENDING_OPEN);
                b.setOpenTime(System.currentTimeMillis());
                onUpdate(b.build());
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "open channel error "+i+" err "+s);

                onLndError(uc, s);
            }
        });
    }

    @Override
    public void work() {

        // lnd must be ready
        if (!lnd_.isRpcReady())
            return;

        if (!notified_ && nextWorkTime_ > System.currentTimeMillis())
            return;

        // reset
        notified_ = false;

        // move 'retry' to 'new' or 'failed'
        List<WalletData.Channel> retryChannels = dao_.getRetryChannels();
        for (WalletData.Channel c: retryChannels) {
            onLndError(c, "Unknown lnd error");
        }

        // open/retry channels
        List<WalletData.Channel> newChannels = dao_.getNewChannels();
        for (WalletData.Channel c: newChannels) {
            openChannel(c);
        }

        nextWorkTime_ = System.currentTimeMillis() + WORK_INTERVAL;
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
        topics.add(DefaultTopics.NEW_CHANNEL);
    }

    @Override
    public void notify(String topic, Object data) {
        notified_ = true;
    }
}
