package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;

public class ShareContact extends ActionBase<WalletData.ShareContactRequest, WalletData.ShareContactResponse> {

    private static int DEFAULT_TIMEOUT = 120000; // 2 min
    private static int MAX_TIMEOUT = 600000; // 10 min

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ShareContactRequest req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    protected WalletData.ShareContactResponse createResponse(PluginContext ctx, WalletData.ShareContactRequest req, long authUserId) {
        return WalletData.ShareContactResponse.builder()
                .build();
    }

    @Override
    protected void signal(WalletData.ShareContactResponse rep) {
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ShareContactResponse.class;
    }

    @Override
    protected boolean isValidUser(WalletData.User user) {
        return true;
    }

    @Override
    protected WalletData.ShareContactRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ShareContactRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    public ShareContact() {
    }

    @Override
    public String id() {
        return DefaultPlugins.SHARE_CONTACT;
    }

}