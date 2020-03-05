package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.WalletData;
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

public class PeerStateWorker implements IPluginBackground {

    public interface IDao {
        WalletData.Peer getPeerByPubkey(String pubkey);
        void updatePeer(WalletData.Peer peer);
        void updatePeerOnline(String pubkey, boolean online);
    }

    private static final String TAG = "PeerStateWorker";
    private static final long LIST_INTERVAL = 10000; // 10sec

    private IPluginServer server_;
    private IDao dao_;
    private ILightningDao lnd_;
    private IPluginBackgroundCallback engine_;
    private boolean started_;
    private boolean listing_;
    private long nextListTime_;

    @Override
    public String id() {
        return DefaultPlugins.PEER_STATE_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        server_ = server;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
        engine_ = callback;
    }

    private void onUpdate(Rpc.Peer r) {
        // get existing peer from db
        WalletData.Peer peer = dao_.getPeerByPubkey(r.getPubKey());

        // no peer? it probably did inbound connection
        if (peer == null) {
            peer = WalletData.Peer.builder()
                    .setId(server_.getIdGenerator().generateId(WalletData.Invoice.class))
                    .setOnline(true)
                    .build();
        }

        // merge updates
        WalletData.Peer.Builder b = peer.toBuilder();
        LightningCodec.PeerConverter.decode(r, b);
        peer = b.build();

        dao_.updatePeer(peer);

        engine_.onSignal(id(), DefaultTopics.PEER_STATE, null);
    }

    private void onEvent(Rpc.PeerEvent r) {
        dao_.updatePeerOnline(r.getPubKey(), r.getTypeValue() == 0);
        engine_.onSignal(id(), DefaultTopics.PEER_STATE, null);
    }

    @Override
    public void work() {
        if (!lnd_.isRpcReady())
            return;

        if (!started_) {
            started_ = true;

            Rpc.PeerEventSubscription s = Rpc.PeerEventSubscription.newBuilder().build();
            lnd_.client().subscribePeerEventsStream(s, new ILightningCallback<Rpc.PeerEvent>() {

                @Override
                public void onResponse(Rpc.PeerEvent e) {
                    Log.i(TAG, "peer event "+e);
                    onEvent(e);
                }

                @Override
                public void onError(int i, String s) {
                    Log.e(TAG, "subscribe peers error " + i + " err " + s);
                    // FIXME might return 'retry later' if starting?
                    throw new RuntimeException("SubscribePeerEvents failed");
                }
            });
        }

        if (nextListTime_ > System.currentTimeMillis() || listing_)
            return;

        listing_ = true;
        Rpc.ListPeersRequest s = Rpc.ListPeersRequest.newBuilder().build();
        lnd_.client().listPeers(s, new ILightningCallback<Rpc.ListPeersResponse>() {

            @Override
            public void onResponse(Rpc.ListPeersResponse r) {
                Log.i(TAG, "list peers: "+r.getPeersCount());
                for(Rpc.Peer p:r.getPeersList())
                    onUpdate(p);

                listing_ = false;
                nextListTime_ = System.currentTimeMillis() + LIST_INTERVAL;
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "list peers error " + i + " err " + s);
                throw new RuntimeException("ListPeers failed");
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
