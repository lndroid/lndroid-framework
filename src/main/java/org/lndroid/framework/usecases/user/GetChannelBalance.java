package org.lndroid.framework.usecases.user;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.usecases.GetData;

public class GetChannelBalance extends GetData<WalletData.ChannelBalance, Long> {
    public GetChannelBalance(IPluginClient client){
        super(client, DefaultPlugins.GET_CHANNEL_BALANCE);
    }

    @Override
    protected WalletData.ChannelBalance getData(IPluginData in) {
        in.assignDataType(WalletData.ChannelBalance.class);
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
