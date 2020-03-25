package org.lndroid.framework.plugins;

import android.util.Log;

import java.io.IOException;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public class SubscribeBackgroundInfo implements IPluginForeground {

    public interface IDao {
        long getPendingChannelCount();
        List<Job> getActiveJobs();
    }

    private static final String TAG = "SubscribeBackgroundInfo";

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IPluginForegroundCallback engine_;
    private IDao dao_;

    @Override
    public String id() {
        return DefaultPlugins.SUBSCRIBE_BACKGROUND_INFO;
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        engine_ = callback;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void work() {
        // noop
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {
        in.assignDataType(WalletData.SubscribeRequest.class);
        WalletData.SubscribeRequest req = null;
        try {
            req = in.getData();
        } catch (IOException e) {}
        if (req == null) {
            engine_.onError(id(), ctx, Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
            return;
        }

        // store req and deadline in the context
        ctx.request = req;
        long timeout = ctx.timeout;
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        ctx.deadline = System.currentTimeMillis() + timeout;

        if (!isUserPrivileged(ctx, ctx.user)) {
            // tell engine we need user auth
            if (req.noAuth())
                engine_.onError(id(), ctx, Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
            else
                engine_.onAuth(id(), ctx);
        } else {
            sendInfo(ctx);
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
        engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        // NOTE: anonym is allowed!
        return !user.isApp();
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.SEND_PAYMENT_STATE);
        topics.add(DefaultTopics.TRANSACTION_STATE);
        topics.add(DefaultTopics.CHANNEL_STATE);
    }

    private void sendInfo(PluginContext ctx) {
        final long pendingChannels = dao_.getPendingChannelCount();
        List<Job> jobs = dao_.getActiveJobs();
        long sendPayment = 0;
        long openChannel = 0;
        long closeChannel = 0;
        long sendCoins = 0;
        for(Job job: jobs) {
            if (DefaultPlugins.SEND_PAYMENT.equals(job.pluginId))
                sendPayment++;
            else if (DefaultPlugins.SEND_COINS.equals(job.pluginId))
                sendCoins++;
            else if (DefaultPlugins.OPEN_CHANNEL.equals(job.pluginId))
                openChannel++;
            else if (DefaultPlugins.CLOSE_CHANNEL.equals(job.pluginId))
                closeChannel++;
            else
                Log.e(TAG, "unknown job plugin "+job);
        }

        boolean active = false
                // this might take weeks, no need to watch it so closely
//                || pendingChannels != 0
                || openChannel != 0
                || closeChannel != 0
                || sendCoins != 0
                || sendPayment != 0;

        WalletData.BackgroundInfo info = WalletData.BackgroundInfo.builder()
                .setIsActive(active)
                .setPendingChannelCount(pendingChannels)
                .setActiveOpenChannelCount(openChannel)
                .setActiveCloseChannelCount(closeChannel)
                .setActiveSendCoinCount(sendCoins)
                .setActiveSendPaymentCount(sendPayment)
                .build();

        engine_.onReply(id(), ctx, info, WalletData.BackgroundInfo.class);
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        sendInfo(ctx);
    }
}
