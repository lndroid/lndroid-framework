package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class ActionEstimateFee extends ActionUseCaseBase<WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse> {
    public ActionEstimateFee(IPluginClient client) {
        super(DefaultPlugins.ESTIMATE_FEE, client, "ActionEstimateFee");
    }

    @Override
    protected WalletData.EstimateFeeResponse getData(IPluginData in) {
        in.assignDataType(WalletData.EstimateFeeResponse.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.EstimateFeeRequest.class;
    }
}
