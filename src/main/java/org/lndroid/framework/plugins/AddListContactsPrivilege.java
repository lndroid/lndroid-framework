package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;

public class AddListContactsPrivilege extends ActionBase<WalletData.ListContactsPrivilege, WalletData.ListContactsPrivilege> {

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListContactsPrivilege req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    protected WalletData.ListContactsPrivilege createResponse(
            PluginContext ctx, WalletData.ListContactsPrivilege req, long authUserId) {

        return req.toBuilder()
                .setId(server().getIdGenerator().generateId(WalletData.ListContactsPrivilege.class))
                .setUserId(ctx.user.id())
                .setAuthUserId(authUserId)
                .setCreateTime(System.currentTimeMillis())
                .build();
    }

    @Override
    protected void signal(WalletData.ListContactsPrivilege p) {
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ListContactsPrivilege.class;
    }

    @Override
    protected boolean isValidUser(WalletData.User user) {
        return true;
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
    public String id() {
        return DefaultPlugins.ADD_LIST_CONTACTS_PRIVILEGE;
    }
}
