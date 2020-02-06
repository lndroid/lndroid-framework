package org.lndroid.framework.usecases.user;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.usecases.GetData;

public class GetWalletInfo extends GetData<WalletData.WalletInfo, Long> {
    public GetWalletInfo(IPluginClient client){
        super(client, DefaultPlugins.GET_WALLET_INFO);
    }

    @Override
    protected WalletData.WalletInfo getData(IPluginData in) {
        in.assignDataType(WalletData.WalletInfo.class);
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
