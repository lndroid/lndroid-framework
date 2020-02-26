package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class ListPeers extends ListData<WalletData.ListPeersRequest, WalletData.Peer> {
    public ListPeers(IPluginClient client) {
        super(client, DefaultPlugins.LIST_PEERS);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.Peer> getData(IPluginData in) {
        in.assignDataType(WalletData.ListPeersResult.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListPeersRequest.class;
    }
}
