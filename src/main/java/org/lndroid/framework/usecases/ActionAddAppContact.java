package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class ActionAddAppContact extends ActionUseCaseBase<WalletData.AddAppContactRequest, WalletData.Contact> {

    public ActionAddAppContact(IPluginClient client) {
        super(DefaultPlugins.ADD_CONTACT_APP, client, "ActionAddAppContact");
    }

    @Override
    protected WalletData.Contact getData(IPluginData in) {
        in.assignDataType(WalletData.Contact.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.AddAppContactRequest.class;
    }
}
