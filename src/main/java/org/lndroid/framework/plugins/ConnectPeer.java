package org.lndroid.framework.plugins;

import org.lndroid.framework.dao.ILndActionDao;
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
                WalletData.ConnectPeerResponse, Data.ConnectPeerResponse>
{
    // plugin's Dao must extend this class
    public interface IDao extends ILndActionDao<WalletData.ConnectPeerRequest, WalletData.ConnectPeerResponse> {};

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
    protected WalletData.ConnectPeerResponse createResponse(
            PluginContext ctx, WalletData.ConnectPeerRequest req, long authUserId, Data.ConnectPeerResponse r) {
        WalletData.ConnectPeerResponse.Builder b = WalletData.ConnectPeerResponse.builder();
        LightningCodec.ConnectPeerCodec.decode(r, b);
        return b.build();
    }

    @Override
    protected void execute(Data.ConnectPeerRequest r, ILightningCallback<Data.ConnectPeerResponse> cb) {
        lnd().client().connectPeer(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, WalletData.ConnectPeerRequest req, WalletData.ConnectPeerResponse rep) {
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.ConnectPeerRequest> tx) {
        return !user.isApp();
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
        return WalletData.ConnectPeerResponse.class;
    }

    @Override
    public String id() {
        return DefaultPlugins.CONNECT_PEER;
    }
}

