package org.lndroid.framework.usecases.user;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.usecases.ActionUseCaseBase;

import java.io.IOException;
import java.lang.reflect.Type;

public class ActionAddContact extends ActionUseCaseBase<WalletData.AddContactRequest, WalletData.Contact> {

    public ActionAddContact(IPluginClient client) {
        super(DefaultPlugins.ADD_CONTACT, client, "ActionAddContact");
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
        return WalletData.AddContactRequest.class;
    }
}

