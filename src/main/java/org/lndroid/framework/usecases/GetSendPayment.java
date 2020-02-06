package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;

public class GetSendPayment extends GetData<WalletData.SendPayment, Long> {
    public GetSendPayment(IPluginClient client) {
        super(client, DefaultPlugins.GET_SEND_PAYMENT);
    }

    @Override
    protected WalletData.SendPayment getData(IPluginData in) {
        in.assignDataType(WalletData.SendPayment.class);
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