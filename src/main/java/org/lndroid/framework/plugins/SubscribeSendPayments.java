package org.lndroid.framework.plugins;

import java.io.IOException;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.ISubscribeSendPaymentsDao;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.common.PluginData;

public class SubscribeSendPayments implements IPluginForeground {

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IPluginForegroundCallback engine_;
    private ISubscribeSendPaymentsDao dao_;

    @Override
    public String id() {
        return DefaultPlugins.SUBSCRIBE_SEND_PAYMENTS;
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        engine_ = callback;
        dao_ = (ISubscribeSendPaymentsDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void work() {
        // noop
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {
        in.assignDataType(WalletData.SubscribeRequest.class);
        WalletData.SubscribeRequest req = null;
        try {
            req = in.getData();
        } catch (IOException e) {}
        if (req == null) {
            engine_.onError(id(), ctx, Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
            return;
        }

        // store req and deadline in the context
        ctx.request = req;
        long timeout = ctx.timeout;
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        ctx.deadline = System.currentTimeMillis() + timeout;

        if (!isUserPrivileged(ctx, ctx.user)) {
            // tell engine we need user auth
            if (req.noAuth())
                engine_.onError(id(), ctx, Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
            else
                engine_.onAuth(id(), ctx);
        } else {
            // send all active payments upon subscription
            List<WalletData.SendPayment> payments = dao_.getActivePayments(req.onlyOwn() ? ctx.user.id() : 0);
            for (WalletData.SendPayment p: payments)
                engine_.onReply(id(), ctx, p, WalletData.SendPayment.class);
        }
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        engine_.onError(id(), ctx, Errors.PLUGIN_PROTOCOL, Errors.errorMessage(Errors.PLUGIN_PROTOCOL));
    }

    @Override
    public void stop(PluginContext ctx) {
        engine_.onDone(id(), ctx);
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse r) {
        // FIXME
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void timeout(PluginContext ctx) {
        engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        WalletData.SubscribeRequest req = (WalletData.SubscribeRequest)ctx.request;
        return user.isRoot() || req.onlyOwn();
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.SEND_PAYMENT_STATE);
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        PluginData.PluginNotification n = (PluginData.PluginNotification)data;
        if (n == null)
            return;

        WalletData.SendPayment p = dao_.getPayment(n.entityId);
        if (p == null)
            return;

        WalletData.SubscribeRequest req = (WalletData.SubscribeRequest)ctx.request;
        if (req.onlyOwn() && p.userId() != ctx.user.id())
            return;

        engine_.onReply(id(), ctx, p, WalletData.SendPayment.class);
    }
}
