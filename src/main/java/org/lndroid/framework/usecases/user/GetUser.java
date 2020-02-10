package org.lndroid.framework.usecases.user;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.usecases.GetData;

public class GetUser extends GetData<WalletData.User, Long> {
    public GetUser(IPluginClient client) {
        super(client, DefaultPlugins.GET_USER);
    }

    @Override
    protected WalletData.User getData(IPluginData in) {
        in.assignDataType(WalletData.User.class);
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