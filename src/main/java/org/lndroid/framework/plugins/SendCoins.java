package org.lndroid.framework.plugins;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.lnd.LightningCodec;

import java.io.IOException;
import java.lang.reflect.Type;

public class SendCoins extends JobBase<WalletData.SendCoinsRequest, WalletData.Transaction> {

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    public SendCoins() {
        super(DefaultPlugins.SEND_COINS, DefaultTopics.NEW_TRANSACTION, DefaultTopics.TRANSACTION_STATE);
    }

    @Override
    protected boolean isUserPrivileged(
            WalletData.User user, Transaction<WalletData.SendCoinsRequest, WalletData.Transaction> tx) {
        return user.isRoot();
    }

    @Override
    protected WalletData.Transaction createResponse(
            PluginContext ctx, WalletData.SendCoinsRequest req, long authUserId) {
        long amount = 0;
        if (req.addrToAmount() != null) {
            for (long a : req.addrToAmount().values())
                amount += a;
        }

        return WalletData.Transaction.builder()
                .setId(server().getIdGenerator().generateId(WalletData.Transaction.class))
                .setUserId(ctx.user.id())
                .setTxId(ctx.txId)
                .setAuthUserId(authUserId)
                .setCreateTime(System.currentTimeMillis())
                .setPurpose(req.purpose())
                .setMaxTries(req.maxTries())
                .setMaxTryTime(req.maxTryTime())
                .setAddrToAmount(req.addrToAmount())
                .setTargetConf(req.targetConf())
                .setSatPerByte(req.satPerByte())
                .setSendAll(req.sendAll())
                .setAmount(amount)
                .build();
    }

    @Override
    protected boolean isValid(WalletData.SendCoinsRequest req) {
        if (!req.sendAll() && (req.addrToAmount() == null || req.addrToAmount().isEmpty()))
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
    protected WalletData.SendCoinsRequest getData(IPluginData in) {
        in.assignDataType(WalletData.SendCoinsRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.Transaction.class;
    }
}

