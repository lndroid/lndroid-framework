package org.lndroid.framework.plugins;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.lnd.daemon.ILightningCallback;

import java.io.IOException;
import java.lang.reflect.Type;

public class DisconnectPeer extends
        LndActionBase<WalletData.DisconnectPeerRequest, lnrpc.Rpc.DisconnectPeerRequest,
                WalletData.Peer, lnrpc.Rpc.DisconnectPeerResponse>
{
    // plugin's Dao must extend this class
    public interface IDao extends ILndActionDao<WalletData.DisconnectPeerRequest, WalletData.Peer> {
        String getContactPubkey(long contactId);
        String getPeerPubkey(long peerId);
    };

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 600000; // 10 min

    @Override
    protected IDao dao() {
        return (IDao)super.dao();
    }

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    private String getPubkey(WalletData.DisconnectPeerRequest req) {
        String pubkey = req.pubkey();
        if (pubkey == null && req.id() != 0)
            pubkey = dao().getPeerPubkey(req.id());
        if (pubkey == null && req.contactId() != 0)
            pubkey = dao().getContactPubkey(req.contactId());
        return pubkey;
    }

    @Override
    protected lnrpc.Rpc.DisconnectPeerRequest createLndRequest(PluginContext ctx,
                                                               WalletData.DisconnectPeerRequest req) {

        String pubkey = getPubkey(req);
        if (pubkey == null)
            return null;

        lnrpc.Rpc.DisconnectPeerRequest.Builder r = lnrpc.Rpc.DisconnectPeerRequest.newBuilder();
//        LightningCodec.DisconnectPeerCodec.encode(req, r);
        r.setPubKey(pubkey);
        return r.build();
    }

    @Override
    protected WalletData.Peer createResponse(
            PluginContext ctx, WalletData.DisconnectPeerRequest req, long authUserId,
            lnrpc.Rpc.DisconnectPeerResponse r) {

        return WalletData.Peer.builder()
                .setId(server().getIdGenerator().generateId(WalletData.Peer.class))
                .setPubkey(getPubkey(req))
                .setDisabled(true)
                .setLastDisconnectTime(System.currentTimeMillis())
                .setOnline(false)
                .build();
    }

    @Override
    protected ILndActionDao.OnResponseMerge<WalletData.Peer> getMerger() {
        return new ILndActionDao.OnResponseMerge<WalletData.Peer> () {

            @Override
            public WalletData.Peer merge(WalletData.Peer old, WalletData.Peer cur) {
                return old.toBuilder()
                        .setOnline(false)
                        .setDisabled(true)
                        .setLastDisconnectTime(cur.lastDisconnectTime())
                        .build();
            }
        };
    }

    @Override
    protected void execute(lnrpc.Rpc.DisconnectPeerRequest r,
                           ILightningCallback<lnrpc.Rpc.DisconnectPeerResponse> cb) {
        lnd().client().disconnectPeer(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, WalletData.DisconnectPeerRequest req, WalletData.Peer rep) {
        engine().onSignal(id(), DefaultTopics.PEER_STATE, rep);
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.DisconnectPeerRequest> tx) {
        return !user.isApp() && !user.isAnonymous();
    }

    @Override
    protected WalletData.DisconnectPeerRequest getData(IPluginData in) {
        in.assignDataType(WalletData.DisconnectPeerRequest.class);
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
        return DefaultPlugins.DISCONNECT_PEER;
    }
}

