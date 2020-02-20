package org.lndroid.framework.plugins;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.PluginContext;

import java.io.IOException;
import java.lang.reflect.Type;

public class CloseChannel extends JobBase<WalletData.CloseChannelRequest, WalletData.Channel> {

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    public CloseChannel() {
        super(DefaultPlugins.CLOSE_CHANNEL, DefaultTopics.NEW_CHANNEL, DefaultTopics.CHANNEL_STATE);
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.CloseChannelRequest> tx) {
        return user.isRoot();
    }

    @Override
    protected WalletData.Channel createResponse(PluginContext ctx, WalletData.CloseChannelRequest req, long authUserId) {
        // FIXME or channelPoint?
        return dao().getResponse(req.channelId());
    }

    @Override
    protected boolean isValid(WalletData.CloseChannelRequest req) {
        // FIXME check that channel exists!
        return true;
    }

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected WalletData.CloseChannelRequest getRequestData(IPluginData in) {
        in.assignDataType(WalletData.CloseChannelRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.Channel.class;
    }
}
