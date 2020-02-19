package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class ListChannels extends ListData<WalletData.ListChannelsRequest, WalletData.Channel> {
    public ListChannels(IPluginClient client) {
        super(client, DefaultPlugins.LIST_CHANNELS);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.Channel> getData(IPluginData in) {
        in.assignDataType(WalletData.ListContactsResult.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListChannelsResult.class;
    }
}
