package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class GetTransaction extends GetData<WalletData.Transaction, Long> {
    public GetTransaction(IPluginClient client){
        super(client, DefaultPlugins.GET_TRANSACTION);
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
        return WalletData.GetRequestLong.class;
    }
}

