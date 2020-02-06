package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.lnd.LightningCodec;

public class OpenChannel extends JobBase<WalletData.OpenChannelRequest, WalletData.Channel> {

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    public OpenChannel() {
        super(DefaultPlugins.OPEN_CHANNEL, DefaultTopics.NEW_CHANNEL, DefaultTopics.CHANNEL_STATE);
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.OpenChannelRequest, WalletData.Channel> tx) {
        // FIXME
        return user.isRoot();
    }

    @Override
    protected WalletData.Channel createResponse(PluginContext ctx, WalletData.OpenChannelRequest req, int authUserId) {
        return WalletData.Channel.builder()
                .setUserId(ctx.user.id())
                .setTxId(ctx.txId)
                .setAuthUserId(authUserId)
                .setDescription(req.description())
                .setRemotePubkey(req.nodePubkey())
                // FIXME is this right?
                .setCapacity(req.localFundingAmount())
                .setLocalBalance(req.localFundingAmount() - req.pushSat())
                .setRemoteBalance(req.pushSat())
                .setTargetConf(req.targetConf())
                .setSatPerByte(req.satPerByte())
                .setIsPrivate(req.isPrivate())
                .setInitiator(true)
                .setMinHtlcMsat(req.minHtlcMsat())
                .setMinConfs(req.minConfs())
                .setSpendUnconfirmed(req.spendUnconfirmed())
                // FIXME is this right?
                .setCsvDelay(req.remoteCsvDelay())
                .setCreateTime(System.currentTimeMillis())
                .build();
    }

    @Override
    protected boolean isValid(WalletData.OpenChannelRequest req) {
        // FIXME move to req.isValid
        if (req.nodePubkey() == null || LightningCodec.hexToBytes(req.nodePubkey()) == null)
            return false;
        if (req.localFundingAmount() <= 0)
            return false;
        if (req.pushSat() < 0)
            return false;
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
    protected WalletData.OpenChannelRequest getData(IPluginData in) {
        in.assignDataType(WalletData.OpenChannelRequest.class);
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
