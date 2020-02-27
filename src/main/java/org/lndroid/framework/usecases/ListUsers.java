package org.lndroid.framework.usecases;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.lang.reflect.Type;

public class ListUsers extends ListData<WalletData.ListUsersRequest, WalletData.User> {
    public ListUsers(IPluginClient client) {
        super(client, DefaultPlugins.LIST_USERS);
    }

    @Override
    protected WalletDataDecl.ListResultTmpl<WalletData.User> getData(IPluginData in) {
        in.assignDataType(WalletData.ListUsersResult.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getRequestType() {
        return WalletData.ListUsersRequest.class;
    }
}

