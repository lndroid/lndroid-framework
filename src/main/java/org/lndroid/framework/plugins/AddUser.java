package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.PluginContext;

public class AddUser extends ActionBase<WalletData.AddUserRequest, WalletData.User> {

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
    protected boolean isUserPrivileged(WalletData.AddUserRequest req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    protected WalletData.User createResponse(PluginContext ctx, WalletData.AddUserRequest req, long authUserId) {
        final long userId = server().getIdGenerator().generateId(WalletData.User.class);
        final String pubkey = server().getKeyStore().generateUserKeyPair(userId, req.role());

        return WalletData.User.builder()
                .setId(userId)
                .setAuthUserId(authUserId)
                .setCreateTime(System.currentTimeMillis())
                .setPubkey(pubkey)
                .setRole(req.role())
                .setAppPackageName(req.appPackageName())
                .setAppLabel(req.appLabel())
                .setAppPubkey(req.appPubkey())
                .build();
    }

    @Override
    protected void signal(WalletData.User user) {
        engine().onSignal(id(), DefaultTopics.NEW_USER, user);
        engine().onSignal(id(), DefaultTopics.USER_STATE, user);
    }

    @Override
    protected Type getResponseType() {
        return WalletData.User.class;
    }

    @Override
    protected boolean isValidUser(WalletData.User user) {
        return !user.isApp();
    }

    @Override
    protected WalletData.AddUserRequest getData(IPluginData in) {
        in.assignDataType(WalletData.AddUserRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_USER;
    }
}

