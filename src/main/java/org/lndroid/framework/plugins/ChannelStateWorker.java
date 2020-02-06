package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.IChannelStateWorkerDao;
import org.lndroid.framework.engine.IDaoProvider;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class ChannelStateWorker implements IPluginBackground {

    private static final String TAG = "ChannelStateWorker";

    private IChannelStateWorkerDao dao_;
    private ILightningDao lnd_;
    private IPluginBackgroundCallback engine_;
    private boolean started_;

    @Override
    public String id() {
        return DefaultPlugins.CHANNEL_STATE_WORKER;
    }

    @Override
    public void init(IDaoProvider dp, IPluginBackgroundCallback engine) {
        dao_ = (IChannelStateWorkerDao)dp.getPluginDao(id());
        lnd_ = dp.getLightningDao();
        engine_ = engine;
    }

    private void onChannelOpen(Data.Channel co) {
        WalletData.Channel c = dao_.getChannelByChannelPoint(co.channelPoint);
        WalletData.Channel.Builder b;
        if (c != null) {
            b = c.toBuilder();
        } else {
            // if we're not initiator then we won't have this channel
            // in our db
            b = WalletData.Channel.builder();
        }

        LightningCodec.ChannelConverter.decode(co, b);
        // FIXME also parse pendingHTLCs
        dao_.updateChannel(b.build());

        engine_.onSignal(id(), DefaultTopics.CHANNEL_STATE, null);
    }

    private void onChannelClosed(Data.ChannelCloseSummary cc) {
        WalletData.Channel c = dao_.getChannelByChannelPoint(cc.channelPoint);
        if (c == null)
            throw new RuntimeException("Unknown channel closed");

        WalletData.Channel.Builder b = c.toBuilder();
        LightningCodec.ChannelCloseSummaryConverter.decode(cc, b);

        // ensure
        if (c.closeTime() == 0)
            b.setCloseTime(System.currentTimeMillis());

        dao_.updateChannel(b.build());
        engine_.onSignal(id(), DefaultTopics.CHANNEL_STATE, null);
    }

    private void onChannelActivate(Data.ChannelPoint cp, boolean active) {
        dao_.setChannelActive(LightningCodec.channelPointToString(cp), active);
        engine_.onSignal(id(), DefaultTopics.CHANNEL_ACTIVE, null);
    }

    private void onUpdate(Data.ChannelEventUpdate r) {
        switch(r.type) {
            case Data.CHANNEL_EVENT_ACTIVE_CHANNEL:
                onChannelActivate(r.activeChannel, true);
                break;
            case Data.CHANNEL_EVENT_INACTIVE_CHANNEL:
                onChannelActivate(r.inactiveChannel, false);
                break;
            case Data.CHANNEL_EVENT_OPEN_CHANNEL:
                onChannelOpen(r.openChannel);
                break;
            case Data.CHANNEL_EVENT_CLOSED_CHANNEL:
                onChannelClosed(r.closedChannel);
                break;
            default:
                throw new RuntimeException("Unknown channel update type "+r.type);
        }
    }

    @Override
    public void work() {
        if (!lnd_.isRpcReady())
            return;

        if (started_)
            return;

        started_ = true;

        // FIXME initial sync protocol:
        // NO! actually it seems like subscription will return the current state on the start,
        // which means we don't need to call listchannels...
        // - subscribe to updates
        // - until synced_ == true, buffer any incoming updates
        // - load opening channels,
        // - if not empty - start sync sequence
        // - call PendingChannels, then ListChannels, then ClosedChannels
        //  - in this order, one by one, otherwise we might end up missing some
        //    channel state transition
        // - after opening channels are updated or turned to RETRY, set synched_=true
        // and process all buffered subs


        Data.ChannelEventSubscription s = new Data.ChannelEventSubscription();
        lnd_.client().subscribeChannelEventsStream(s, new ILightningCallback<Data.ChannelEventUpdate>() {

            @Override
            public void onResponse(Data.ChannelEventUpdate d) {
                Log.i(TAG, "subscribe channels update");
                onUpdate(d);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "subscribe channels error "+i+" err "+s);
                throw new RuntimeException("SubscribeChannels failed");
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
