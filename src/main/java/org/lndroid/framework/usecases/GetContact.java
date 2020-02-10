package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class GetContact extends GetData<WalletData.Contact, Long> {
    public GetContact(IPluginClient client){
        super(client, DefaultPlugins.GET_CONTACT);
    }

    @Override
    protected WalletData.Contact getData(IPluginData in) {
        in.assignDataType(WalletData.Invoice.class);
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
