package org.lndroid.framework.plugins;

import com.google.common.collect.ImmutableList;

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
import java.util.ArrayList;
import java.util.List;

public class SubscribePaidInvoicesEvents implements IPluginForeground {

    public interface IDao {
        List<WalletData.Invoice> getNewPaidInvoices();
    }

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IPluginForegroundCallback engine_;
    private IDao dao_;

    @Override
    public String id() {
        return DefaultPlugins.SUBSCRIBE_PAID_INVOICES_EVENTS;
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
        in.assignDataType(WalletData.SubscribePaidInvoicesEventsRequest.class);
        WalletData.SubscribePaidInvoicesEventsRequest req = null;
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
            // auth not supported
            engine_.onError(id(), ctx, Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
        } else {
            sendEvent(ctx);
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
        return !user.isApp();
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.INVOICE_STATE);
    }

    private void sendEvent(PluginContext ctx) {
        List<WalletData.Invoice> list = dao_.getNewPaidInvoices();
        if (list.isEmpty())
            return;

        long sats = 0;
        long count = 0;
        ImmutableList.Builder<Long> ib = ImmutableList.builder();
        for(WalletData.Invoice i: list) {
            // FIXME message payments are notified by the Messenger,
            // instead of his hard-coded shit we should rely on privileges!
            if (i.message() != null)
                continue;
            ib.add(i.id());
            sats += i.amountPaidMsat() / 1000;
            count++;
        }
        // even if we did get some invoices settled, user probably doesn't care unless
        // those had sats
        if (sats == 0)
            return;

        WalletData.PaidInvoicesEvent e = WalletData.PaidInvoicesEvent.builder()
                .setInvoiceIds(ib.build())
                .setSatsReceived(sats)
                .setInvoicesCount(count)
                .build();
        engine_.onReply(id(), ctx, e, WalletData.PaidInvoicesEvent.class);
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        sendEvent(ctx);
    }
}

