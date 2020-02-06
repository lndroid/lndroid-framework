package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.INodeInfoDao;
import org.lndroid.framework.engine.IDaoProvider;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class NodeInfoWorker implements IPluginBackground {

    private static final String TAG = "NodeInfoWorker";
    private static final long UPDATE_INTERVAL = 10000; // 10 sec

    private IPluginBackgroundCallback engine_;
    private INodeInfoDao dao_;
    private ILightningDao lnd_;
    private boolean updating_;
    private boolean refresh_;
    private long nextUpdateTime_;

    @Override
    public String id() {
        return DefaultPlugins.NODE_INFO_WORKER;
    }

    @Override
    public void init(IDaoProvider dp, IPluginBackgroundCallback engine) {
        engine_ = engine;
        dao_ = (INodeInfoDao)dp.getPluginDao(id());
        lnd_ = dp.getLightningDao();
    }

    private void reschedule() {
        nextUpdateTime_ = System.currentTimeMillis() + UPDATE_INTERVAL;
        updating_ = false;
    }

    private void onUpdate(WalletData.LightningNode node, List<WalletData.ChannelEdge> channels) {
        dao_.updateNode(node);
        dao_.updateChannels(channels);
        engine_.onSignal(id(), DefaultTopics.NODE_INFO_SELF, null);
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

        Data.NodeInfoRequest r = new Data.NodeInfoRequest();
        r.pubKey = dao_.getWalletPubkey();
        r.includeChannels = true;

        if (r.pubKey == null || r.pubKey.isEmpty ())
            return;

        // mark, to avoid sending multiple requests
        updating_ = true;

        lnd_.client().getNodeInfo(r, new ILightningCallback<Data.NodeInfo>() {
            @Override
            public void onResponse(Data.NodeInfo r) {
                Log.i(TAG, "node info update "+r);
                WalletData.LightningNode.Builder b = WalletData.LightningNode.builder();
                LightningCodec.LightningNodeConverter.decode(r.node, b);

                List<WalletData.ChannelEdge> channels = null;
                if (r.channels != null) {
                    channels = new ArrayList<>();
                    for(Data.ChannelEdge c: r.channels) {
                        WalletData.ChannelEdge.Builder cb = WalletData.ChannelEdge.builder();
                        LightningCodec.ChannelEdgeConverter.decode(c, cb);
                        channels.add(cb.build());
                    }
                }

                onUpdate(b.build(), channels);
                reschedule();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "Failed to get node info, code "+i+" err "+s);
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
        topics.add(DefaultTopics.CHANNEL_STATE);
    }

    @Override
    public void notify(String topic, Object data) {
        refresh_ = true;
    }
}
