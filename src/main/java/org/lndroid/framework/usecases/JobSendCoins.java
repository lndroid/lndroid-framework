package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class JobSendCoins extends ActionUseCaseBase<WalletData.SendCoinsRequest, WalletData.Transaction> {
    public JobSendCoins(IPluginClient client) {
        super(DefaultPlugins.SEND_COINS, client, "JobSendCoins");
    }

    @Override
    protected WalletData.Transaction getData(IPluginData in) {
        in.assignDataType(WalletData.Transaction.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.SendCoinsRequest.class;
    }
}

