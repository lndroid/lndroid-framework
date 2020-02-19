package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class ListUtxo extends ListData<WalletData.ListUtxoRequest, WalletData.Utxo> {
    public ListUtxo(IPluginClient client) {
        super(client, DefaultPlugins.LIST_UTXO);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.Utxo> getData(IPluginData in) {
        in.assignDataType(WalletData.ListInvoicesResult.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListUtxoRequest.class;
    }
}
