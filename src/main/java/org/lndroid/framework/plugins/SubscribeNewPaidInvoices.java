package org.lndroid.framework.plugins;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

import java.io.IOException;
import java.util.List;

public class SubscribeNewPaidInvoices implements IPluginForeground {

    public interface IDao {
        WalletData.Payment getPayment(String protocol, long invoiceId);
        List<WalletData.Payment> getPayments(String protocol);
    }

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IPluginForegroundCallback engine_;
    private IDao dao_;

    @Override
    public String id() {
        return DefaultPlugins.SUBSCRIBE_NEW_PAID_INVOICES;
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        engine_ = callback;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void work() {
        // noop
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {
        in.assignDataType(WalletData.SubscribeNewPaidInvoices.class);
        WalletData.SubscribeNewPaidInvoices req = null;
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
            List<WalletData.Payment> ps = dao_.getPayments(req.protocolExtension());
            for (WalletData.Payment p: ps)
                engine_.onReply(id(), ctx, p, WalletData.Payment.class);
        }
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        engine_.onError(id(), ctx, Errors.PLUGIN_PROTOCOL, Errors.errorMessage(Errors.PLUGIN_PROTOCOL));
    }

    @Override
    public void stop(PluginContext ctx) {
        engine_.onDone(id(), ctx, true);
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse r) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void timeout(PluginContext ctx) {
        engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        WalletData.SubscribeNewPaidInvoices req = (WalletData.SubscribeNewPaidInvoices)ctx.request;
        // FIXME only messages are supported atm
        return user.isRoot() || !WalletData.PROTOCOL_MESSAGES.equals(req.protocolExtension());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.INVOICE_STATE);
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        WalletData.Invoice in = (WalletData.Invoice)data;
        if (in == null)
            return;

        WalletData.SubscribeNewPaidInvoices req = (WalletData.SubscribeNewPaidInvoices)ctx.request;
        WalletData.Payment p = dao_.getPayment(req.protocolExtension(), in.id());
        if (p == null)
            return;

        engine_.onReply(id(), ctx, p, WalletData.Payment.class);
    }
}

