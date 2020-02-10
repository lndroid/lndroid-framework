package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class ActionDecodePayReq extends ActionUseCaseBase<String, WalletData.SendPayment> {
    public ActionDecodePayReq(IPluginClient client) {
        super(DefaultPlugins.DECODE_PAYREQ, client, "ActionDecodePayReq");
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
        return WalletData.SendPayment.class;
    }
}
