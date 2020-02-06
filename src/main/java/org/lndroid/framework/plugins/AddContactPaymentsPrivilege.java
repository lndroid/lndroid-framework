package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;

public class AddContactPaymentsPrivilege extends ActionBase<WalletData.ContactPaymentsPrivilege, WalletData.ContactPaymentsPrivilege> {

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
    protected boolean isUserPrivileged(WalletData.ContactPaymentsPrivilege req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    protected WalletData.ContactPaymentsPrivilege createResponse(
            PluginContext ctx, WalletData.ContactPaymentsPrivilege req, int authUserId) {

        return req.toBuilder()
                .setUserId(ctx.user.id())
                .setAuthUserId(authUserId)
                .setCreateTime(System.currentTimeMillis())
                .build();
    }

    @Override
    protected void signal(WalletData.ContactPaymentsPrivilege p) {
        // FIXME notify other plugins
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ContactPaymentsPrivilege.class;
    }

    @Override
    protected boolean isValidUser(WalletData.User user) {
        return true;
    }

    @Override
    protected WalletData.ContactPaymentsPrivilege getData(IPluginData in) {
        in.assignDataType(WalletData.ContactPaymentsPrivilege.class);
        try {
            // FIXME validate input
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_CONTACT_PAYMENTS_PRIVILEGE;
    }
}