package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.dao.IChannelStateWorkerDao;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

import lnrpc.Rpc;

public class ChannelStateWorker implements IPluginBackground {

    public interface IDao extends IChannelStateWorkerDao{};

    private static final String TAG = "ChannelStateWorker";

    private IPluginServer server_;
    private IDao dao_;
    private ILightningDao lnd_;
    private IPluginBackgroundCallback engine_;
    private boolean started_;
    private boolean synched_;
    private boolean openSynched_;
    private boolean closedSynched_;
    private boolean pendingSynched_;
    private List<Data.ChannelEventUpdate> buffer_ = new ArrayList<>();

    @Override
    public String id() {
        return DefaultPlugins.CHANNEL_STATE_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        server_ = server;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
        engine_ = callback;
    }

    private WalletData.Channel.Builder getOrCreateChannel(String channelPoint) {
        WalletData.Channel c = dao_.getChannelByChannelPoint(channelPoint);
        WalletData.Channel.Builder b;
        if (c != null) {
            b = c.toBuilder();
        } else {
            // if we're not initiator then we won't have this channel
            // in our db
            b = WalletData.Channel.builder();
            b.setId(server_.getIdGenerator().generateId(WalletData.Channel.class));
        }
        return b;
    }

    private void update(WalletData.Channel.Builder b){
        dao_.updateChannel(b.build());
        engine_.onSignal(id(), DefaultTopics.CHANNEL_STATE, null);
    }

    private void onOpenChannel(Data.Channel co) {
        WalletData.Channel.Builder b = getOrCreateChannel(co.channelPoint);

        LightningCodec.ChannelConverter.decode(co, b);
        b.setState(WalletData.CHANNEL_STATE_OPEN);

        // FIXME also parse pendingHTLCs

        update(b);
    }

    private void onChannelClosed(Data.ChannelCloseSummary cc) {
        WalletData.Channel c = dao_.getChannelByChannelPoint(cc.channelPoint);
        if (c == null)
            throw new RuntimeException("Unknown channel closed");

        WalletData.Channel.Builder b = c.toBuilder();
        LightningCodec.ChannelCloseSummaryConverter.decode(cc, b);
        b.setState(WalletData.CHANNEL_STATE_CLOSED);

        // ensure
        if (c.closeTime() == 0)
            b.setCloseTime(System.currentTimeMillis());

        update(b);
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
                onOpenChannel(r.openChannel);
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

        Data.ChannelEventSubscription s = new Data.ChannelEventSubscription();
        lnd_.client().subscribeChannelEventsStream(s, new ILightningCallback<Data.ChannelEventUpdate>() {

            @Override
            public void onResponse(Data.ChannelEventUpdate d) {
                Log.i(TAG, "subscribe channels update");
                if (synched_)
                    onUpdate(d);
                else
                    buffer_.add(d);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "subscribe channels error "+i+" err "+s);
                throw new RuntimeException("SubscribeChannels failed");
            }
        });

        // initial sync protocol:
        // - subscribe to updates
        // - until synced_ == true, buffer any incoming updates
        // - call PendingChannels, ListChannels, ClosedChannels
        //  - order of calls doesn't matter as transitions during sync will be received and buffered
        //   - or maybe not! bcs actial subscribe may be delayed
        // - after opening channels are updated or turned to RETRY (if not found), set synched_=true
        //   and process all buffered subs

        syncPendingChannels();
        syncClosedChannels();
        syncOpenChannels();
    }

    private void maybeSynched() {
        if (!openSynched_ || !pendingSynched_ || !closedSynched_)
            return;

        // now we're synched and ready to process new events
        synched_ = true;

        // mark all non-updated channels as 'retry'
        List<WalletData.Channel> cs = dao_.getOpeningChannels();
        for(WalletData.Channel c: cs) {
            dao_.updateChannel(c.toBuilder().setState(WalletData.CHANNEL_STATE_RETRY).build());
        }

        // process buffered new events
        for(Data.ChannelEventUpdate u: buffer_)
            onUpdate(u);
    }

    private void syncOpenChannels() {

        Data.ListChannelsRequest s = new Data.ListChannelsRequest();
        lnd_.client().listChannels(s, new ILightningCallback<Data.ListChannelsResponse>() {

            @Override
            public void onResponse(Data.ListChannelsResponse d) {
                Log.i(TAG, "list channels reply "+d.channels.size());

                // we might have missed some updates, so now we have
                // to update all known txs :(
                for(Data.Channel c: d.channels)
                    onOpenChannel(c);

                openSynched_ = true;
                maybeSynched();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "get transactions error "+i+" err "+s);
                throw new RuntimeException("GetTransactions failed");
            }
        });
    }

    private void onPendingOpenChannel(Rpc.PendingChannelsResponse.PendingOpenChannel poc) {
        WalletData.Channel.Builder b = getOrCreateChannel(poc.getChannel().getChannelPoint());

        LightningCodec.PendingChannelConverter.decode(poc, b);
        b.setState(WalletData.CHANNEL_STATE_PENDING_OPEN);

        update(b);
    }

    private void onPendingClosingChannel(Rpc.PendingChannelsResponse.ClosedChannel cc) {
        WalletData.Channel.Builder b = getOrCreateChannel(cc.getChannel().getChannelPoint());

        LightningCodec.PendingChannelConverter.decode(cc, b);
        b.setState(WalletData.CHANNEL_STATE_PENDING_CLOSE);

        update(b);
    }

    private void onPendingForceClosingChannel(Rpc.PendingChannelsResponse.ForceClosedChannel fcc) {
        WalletData.Channel.Builder b = getOrCreateChannel(fcc.getChannel().getChannelPoint());

        LightningCodec.PendingChannelConverter.decode(fcc, b);
        b.setState(WalletData.CHANNEL_STATE_PENDING_FORCE_CLOSE);

        update(b);
    }

    private void onWaitingCloseChannel(Rpc.PendingChannelsResponse.WaitingCloseChannel wcc) {
        WalletData.Channel.Builder b = getOrCreateChannel(wcc.getChannel().getChannelPoint());

        LightningCodec.PendingChannelConverter.decode(wcc, b);
        b.setState(WalletData.CHANNEL_STATE_WAITING_CLOSE);

        update(b);
    }

    private void syncPendingChannels() {

        Rpc.PendingChannelsRequest s = Rpc.PendingChannelsRequest.newBuilder().build();
        lnd_.client().pendingChannels(s, new ILightningCallback<Rpc.PendingChannelsResponse>() {

            @Override
            public void onResponse(Rpc.PendingChannelsResponse d) {
                Log.i(TAG, "pending channels reply "+d);

                for(Rpc.PendingChannelsResponse.PendingOpenChannel c: d.getPendingOpenChannelsList())
                    onPendingOpenChannel(c);
                for(Rpc.PendingChannelsResponse.ClosedChannel c: d.getPendingClosingChannelsList())
                    onPendingClosingChannel(c);
                for(Rpc.PendingChannelsResponse.ForceClosedChannel c: d.getPendingForceClosingChannelsList())
                    onPendingForceClosingChannel(c);
                for(Rpc.PendingChannelsResponse.WaitingCloseChannel c: d.getWaitingCloseChannelsList())
                    onWaitingCloseChannel(c);

                pendingSynched_ = true;
                maybeSynched();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "PendingChannels error "+i+" err "+s);
                throw new RuntimeException("PendingChannels failed");
            }
        });
    }

    private void onClosedChannel(Rpc.ChannelCloseSummary cc) {
        // NOTE: this is same as w/ Rpc.ChannelCloseSummary,
        // merge these methods when lndroid-daemon is fully moved to Rpc instead of Data
        WalletData.Channel c = dao_.getChannelByChannelPoint(cc.getChannelPoint());
        if (c == null)
            throw new RuntimeException("Unknown channel closed");

        WalletData.Channel.Builder b = c.toBuilder();
        LightningCodec.ChannelCloseSummaryConverter.decode(cc, b);
        b.setState(WalletData.CHANNEL_STATE_CLOSED);

        // ensure
        if (c.closeTime() == 0)
            b.setCloseTime(System.currentTimeMillis());

        update(b);
    }

    private void syncClosedChannels() {

        Rpc.ClosedChannelsRequest s = Rpc.ClosedChannelsRequest.newBuilder().build();
        lnd_.client().closedChannels(s, new ILightningCallback<Rpc.ClosedChannelsResponse>() {

            @Override
            public void onResponse(Rpc.ClosedChannelsResponse d) {
                Log.i(TAG, "closed channels reply "+d);

                for(Rpc.ChannelCloseSummary s: d.getChannelsList())
                    onClosedChannel(s);

                closedSynched_ = true;
                maybeSynched();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "ClosedChannels error "+i+" err "+s);
                throw new RuntimeException("ClosedChannels failed");
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
