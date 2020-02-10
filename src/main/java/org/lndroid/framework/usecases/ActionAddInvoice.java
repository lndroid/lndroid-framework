package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

public class ActionAddInvoice extends ActionUseCaseBase<WalletData.AddInvoiceRequest, WalletData.Invoice> {
    public ActionAddInvoice(IPluginClient client) {
        super(DefaultPlugins.ADD_INVOICE, client, "ActionAddInvoice");
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
        return WalletData.AddInvoiceRequest.class;
    }
}
