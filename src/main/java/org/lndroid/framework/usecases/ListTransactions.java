package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class ListTransactions extends ListData<WalletData.ListTransactionsRequest, WalletData.Transaction> {
    public ListTransactions(IPluginClient client) {
        super(client, DefaultPlugins.LIST_TRANSACTIONS);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.Transaction> getData(IPluginData in) {
        in.assignDataType(WalletData.ListTransactionsResult.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListTransactionsResult.class;
    }
}

