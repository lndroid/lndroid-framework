package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class ActionDisconnectPeer extends ActionUseCaseBase<WalletData.DisconnectPeerRequest, WalletData.Peer> {
    public ActionDisconnectPeer(IPluginClient client) {
        super(DefaultPlugins.DISCONNECT_PEER, client, "ActionDisconnectPeer");
    }

    @Override
    protected WalletData.Peer getData(IPluginData in) {
        in.assignDataType(WalletData.Peer.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.DisconnectPeerRequest.class;
    }
}
