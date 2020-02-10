package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class ActionConnectPeer extends ActionUseCaseBase<WalletData.ConnectPeerRequest, WalletData.ConnectPeerResponse> {
    public ActionConnectPeer(IPluginClient client) {
        super(DefaultPlugins.CONNECT_PEER, client, "ActionConnectPeer");
    }

    @Override
    protected WalletData.ConnectPeerResponse getData(IPluginData in) {
        in.assignDataType(WalletData.ConnectPeerResponse.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ConnectPeerRequest.class;
    }
}

