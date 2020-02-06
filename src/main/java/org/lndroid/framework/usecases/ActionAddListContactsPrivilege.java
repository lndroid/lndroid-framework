package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class ActionAddListContactsPrivilege extends
        ActionUseCaseBase<WalletData.ListContactsPrivilege, WalletData.ListContactsPrivilege> {

    public ActionAddListContactsPrivilege(IPluginClient client) {
        super(DefaultPlugins.ADD_LIST_CONTACTS_PRIVILEGE, client, "ActionAddListContactsPrivilege");
    }

    @Override
    protected WalletData.ListContactsPrivilege getData(IPluginData in) {
        in.assignDataType(WalletData.ListContactsPrivilege.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListContactsPrivilege.class;
    }
}

