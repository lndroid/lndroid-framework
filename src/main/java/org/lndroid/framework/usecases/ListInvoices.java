package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

public class ListInvoices extends ListData<WalletData.ListInvoicesRequest, WalletData.Invoice> {
    public ListInvoices(IPluginClient client) {
        super(client, DefaultPlugins.LIST_INVOICES);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.Invoice> getData(IPluginData in) {
        in.assignDataType(WalletData.ListInvoicesResult.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListInvoicesRequest.class;
    }
}
