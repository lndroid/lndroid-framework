package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class ActionAddContactInvoice extends
        ActionUseCaseBase<WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse> {

    public ActionAddContactInvoice(IPluginClient client) {
        super(DefaultPlugins.ADD_CONTACT_INVOICE, client, "ActionAddContactInvoice");
    }

    @Override
    protected WalletData.AddContactInvoiceResponse getData(IPluginData in) {
        in.assignDataType(WalletData.AddContactInvoiceResponse.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.AddContactInvoiceRequest.class;
    }
}
