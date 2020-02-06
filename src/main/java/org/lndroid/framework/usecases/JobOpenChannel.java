package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;

public class JobOpenChannel extends ActionUseCaseBase<WalletData.OpenChannelRequest, WalletData.Channel> {
    public JobOpenChannel(IPluginClient client) {
        super(DefaultPlugins.OPEN_CHANNEL, client, "JobOpenChannel");
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
        return WalletData.OpenChannelRequest.class;
    }
}
