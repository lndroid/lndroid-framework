package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

import java.io.IOException;
import java.lang.reflect.Type;

public class GetChannel extends GetData<WalletData.Channel, Long> {
    public GetChannel(IPluginClient client){
        super(client, DefaultPlugins.GET_CHANNEL);
    }

    @Override
    protected WalletData.Channel getData(IPluginData in) {
        in.assignDataType(WalletData.Channel.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.GetRequestLong.class;
    }
}

