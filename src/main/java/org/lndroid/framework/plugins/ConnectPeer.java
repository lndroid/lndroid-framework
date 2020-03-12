package org.lndroid.framework.plugins;

import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.lnd.LightningCodec;

public class ConnectPeer extends
        LndActionBase<WalletData.ConnectPeerRequest, Data.ConnectPeerRequest,
                WalletData.Peer, Data.ConnectPeerResponse>
{
    // plugin's Dao must extend this class
    public interface IDao extends ILndActionDao<WalletData.ConnectPeerRequest, WalletData.Peer> {};

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 600000; // 10 min

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected Data.ConnectPeerRequest createLndRequest(PluginContext ctx, WalletData.ConnectPeerRequest req) {
        Data.ConnectPeerRequest r = new Data.ConnectPeerRequest();
        LightningCodec.ConnectPeerCodec.encode(req, r);
        return r;
    }

    @Override
    protected WalletData.Peer createResponse(
            PluginContext ctx, WalletData.ConnectPeerRequest req, long authUserId, Data.ConnectPeerResponse r) {
        return WalletData.Peer.builder()
                .setId(server().getIdGenerator().generateId(WalletData.Peer.class))
                .setPubkey(req.pubkey())
                .setAddress(req.address())
                .setPerm(req.perm())
                .setDisabled(false)
                // don't set 'online' as connection is not yet established
//                .setOnline(true)
                .setLastConnectTime(System.currentTimeMillis())
                .build();
    }

    @Override
    protected ILndActionDao.OnResponseMerge<WalletData.Peer> getMerger() {
        return new ILndActionDao.OnResponseMerge<WalletData.Peer> () {

            @Override
            public WalletData.Peer merge(WalletData.Peer old, WalletData.Peer cur) {
                return old.toBuilder()
                        // don't set 'online' as connection is not yet established
//                        .setOnline(true)
                        .setDisabled(false)
                        .setPerm(cur.perm())
                        .setAddress(cur.address())
                        .setInbound(false)
                        .setLastConnectTime(cur.lastConnectTime())
                        .build();
            }
        };
    }

    @Override
    protected void execute(Data.ConnectPeerRequest r, ILightningCallback<Data.ConnectPeerResponse> cb) {
        lnd().client().connectPeer(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, WalletData.ConnectPeerRequest req, WalletData.Peer rep) {
        engine().onSignal(id(), DefaultTopics.PEER_STATE, rep);
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.ConnectPeerRequest> tx) {
        return !user.isApp() && !user.isAnonymous();
    }

    @Override
    protected WalletData.ConnectPeerRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ConnectPeerRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.Peer.class;
    }

    @Override
    public String id() {
        return DefaultPlugins.CONNECT_PEER;
    }
}

