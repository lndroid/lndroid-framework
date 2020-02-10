package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

public class ActionNewAddress extends ActionUseCaseBase<WalletData.NewAddressRequest, WalletData.NewAddress> {
    public ActionNewAddress(IPluginClient client) {
        super(DefaultPlugins.NEW_ADDRESS, client, "ActionNewAddress");
    }

    @Override
    protected WalletData.NewAddress getData(IPluginData in) {
        in.assignDataType(WalletData.NewAddress.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.NewAddressRequest.class;
    }
}
