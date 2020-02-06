package org.lndroid.framework.usecases.user;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.usecases.GetData;

public class GetWalletBalance extends GetData<WalletData.WalletBalance, Long> {
    public GetWalletBalance(IPluginClient client){
        super(client, DefaultPlugins.GET_WALLET_BALANCE);
    }

    @Override
    protected WalletData.WalletBalance getData(IPluginData in) {
        in.assignDataType(WalletData.WalletBalance.class);
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
