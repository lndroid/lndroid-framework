package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;

public class ListPayments extends ListData<WalletData.ListPaymentsRequest, WalletData.Payment> {
    public ListPayments(IPluginClient client) {
        super(client, DefaultPlugins.LIST_PAYMENTS);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.Payment> getData(IPluginData in) {
        in.assignDataType(WalletData.ListPaymentsResult.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListPaymentsRequest.class;
    }
}
