package org.lndroid.framework.usecases;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;

public class ActionAddContactPaymentsPrivilege extends
        ActionUseCaseBase<WalletData.ContactPaymentsPrivilege, WalletData.ContactPaymentsPrivilege> {

    public ActionAddContactPaymentsPrivilege(IPluginClient client) {
        super(DefaultPlugins.ADD_CONTACT_PAYMENTS_PRIVILEGE, client, "ActionAddContactPaymentsPriv");
    }

    @Override
    protected WalletData.ContactPaymentsPrivilege getData(IPluginData in) {
        in.assignDataType(WalletData.ContactPaymentsPrivilege.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ContactPaymentsPrivilege.class;
    }
}

