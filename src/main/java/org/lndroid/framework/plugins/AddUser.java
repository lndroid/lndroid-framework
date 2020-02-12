package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.PluginUtils;
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
        final String nonce = server().getKeyStore().generatePasswordKeyNonce();
        WalletData.User u = WalletData.User.builder()
                .setId(userId)
                .setAuthUserId(authUserId)
                .setCreateTime(System.currentTimeMillis())
                .setRole(req.role())
                .setNonce(nonce)
                .setAuthType(req.authType())
                .setAppPackageName(req.appPackageName())
                .setAppLabel(req.appLabel())
                .setAppPubkey(req.appPubkey())
                .build();

        final String pubkey = server().getKeyStore().generateKeyPair(
                PluginUtils.userKeyAlias(u.id()),
                u.authType(),
                u.nonce(),
                req.password());
        return u.toBuilder()
                .setPubkey(pubkey)
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

            WalletData.AddUserRequest r = in.getData();
            if (r == null || r.role() == null)
                return null;

            switch(r.role()) {
                case WalletData.USER_ROLE_ROOT:
                case WalletData.USER_ROLE_USER:
                    if (r.authType() == null)
                        return null;
                    switch(r.authType()) {
                        case WalletData.AUTH_TYPE_NONE:
                            break;

                        case WalletData.AUTH_TYPE_PASSWORD:
                            if (r.password() == null || r.password().length() < WalletData.MIN_PASSWORD_LEN)
                                return null;
                            break;

                        case WalletData.AUTH_TYPE_SCREEN_LOCK:
                        case WalletData.AUTH_TYPE_DEVICE_SECURITY:
                            if (!server().getKeyStore().isDeviceSecure())
                                return null;
                            break;

                        case WalletData.AUTH_TYPE_BIO:
                            if (!server().getKeyStore().isBiometricsAvailable())
                                return null;
                            break;

                        default:
                            return null;
                    }

                case WalletData.USER_ROLE_APP:
                    if (r.appPubkey() == null || r.appPubkey().equals("")
                        || r.appPackageName() == null || r.appPackageName().equals("")
                        || r.appLabel() == null && r.appLabel().equals(""))
                        return null;

                    // fall through
                case WalletData.USER_ROLE_BG:
                    if (r.authType() != null && !r.authType().equals(WalletData.AUTH_TYPE_NONE))
                        return null;

                    break;

                default:
                    return null;
            }

            return r;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_USER;
    }
}

