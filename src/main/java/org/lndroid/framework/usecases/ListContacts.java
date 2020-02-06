package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;


public class ListContacts extends ListData<WalletData.ListContactsRequest, WalletData.Contact> {
    public ListContacts(IPluginClient client) {
        super(client, DefaultPlugins.LIST_CONTACTS);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.Contact> getData(IPluginData in) {
        in.assignDataType(WalletData.ListContactsResult.class);
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
