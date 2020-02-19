package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class GetUtxo extends GetData<WalletData.Utxo, Long> {
    public GetUtxo(IPluginClient client){
        super(client, DefaultPlugins.GET_UTXO);
    }

    @Override
    protected WalletData.Utxo getData(IPluginData in) {
        in.assignDataType(WalletData.Utxo.class);
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
