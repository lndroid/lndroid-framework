package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;

public class JobSendPayment extends ActionUseCaseBase<WalletData.SendPaymentRequest, WalletData.SendPayment> {
    public JobSendPayment(IPluginClient client) {
        super(DefaultPlugins.SEND_PAYMENT, client, "JobSendPayment");
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
        return WalletData.SendPaymentRequest.class;
    }
}
