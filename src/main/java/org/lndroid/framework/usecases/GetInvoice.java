package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;

public class GetInvoice extends GetData<WalletData.Invoice, Long> {
    public GetInvoice(IPluginClient client){
        super(client, DefaultPlugins.GET_INVOICE);
    }

    @Override
    protected WalletData.Invoice getData(IPluginData in) {
        in.assignDataType(WalletData.Invoice.class);
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
