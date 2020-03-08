package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class GetPaymentPeerContact extends GetData<WalletData.Contact, Long> {
    public GetPaymentPeerContact(IPluginClient client){
        super(client, DefaultPlugins.GET_PAYMENT_PEER_CONTACT);
    }

    @Override
    protected WalletData.Contact getData(IPluginData in) {
        in.assignDataType(WalletData.Contact.class);
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
