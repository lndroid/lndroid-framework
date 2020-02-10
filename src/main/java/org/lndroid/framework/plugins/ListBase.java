package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;

public abstract class ListBase<Request extends WalletData.ListRequestBase, Response> implements IPluginForeground {

    private static final String TAG = "ListBase";
    private static final long DEFAULT_TIMEOUT = 3600000; // 1h
    private static final int DEFAULT_COUNT = 10;
    private static final int MAX_COUNT = 100;

    private String pluginId_;
    private IPluginForegroundCallback engine_;

    ListBase(String pluginId) {
        pluginId_ = pluginId;
    }

    @Override
    final public String id() {
        return pluginId_;
    }

    public void init(IPluginForegroundCallback engine) {
        engine_ = engine;
    }

    @Override
    public void work() {
        // noop
    }

    private void onError(PluginContext ctx, String code) {
        onError(ctx, code, Errors.errorMessage(code));
    }

    private void onError(PluginContext ctx, String code, String s) {
        engine_.onError(id(), ctx, code, s);
    }

    private void onAuth(PluginContext ctx) {
        engine_.onAuth(id(), ctx);
    }

    private void onReply(PluginContext ctx, Object data, Type type) {
        engine_.onReply(id(), ctx, data, type);
    }

    private void onDone(PluginContext ctx) {
        engine_.onDone(id(), ctx);
    }

    protected abstract WalletDataDecl.ListResultTmpl<Response> listEntities(Request req, WalletData.ListPage page, WalletData.User user);
    protected abstract Request getData(IPluginData in);
    protected abstract Type getResponseType();

    private void sendResults(PluginContext ctx, Request req, WalletData.ListPage page) {

        if (page.count() == 0)
            page = page.toBuilder().setCount(DEFAULT_COUNT).build();
        else if (page.count() > MAX_COUNT)
            page = page.toBuilder().setCount(MAX_COUNT).build();

        long callerUserId = 0;
        if (req.onlyOwn())
            callerUserId = ctx.user.id();

        WalletDataDecl.ListResultTmpl<Response> results = listEntities(req, page, ctx.user);
        onReply(ctx, results, getResponseType());
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {
        Request req = getData(in);
        if (req == null) {
            onError(ctx, Errors.PLUGIN_MESSAGE);
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
                onError(ctx, Errors.FORBIDDEN);
            else
                onAuth(ctx);
        } else {
            sendResults(ctx, req, req.page());
            if (!req.enablePaging())
                onDone(ctx);
        }
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        Request req = (Request)ctx.request;
        if (req == null || !req.enablePaging()) {
            onError(ctx, Errors.PLUGIN_PROTOCOL);
            return;
        }

        in.assignDataType(WalletData.ListPage.class);
        WalletData.ListPage page = null;
        try {
            page = in.getData();
        } catch (IOException e) {}
        if (page == null) {
            onError(ctx, Errors.PLUGIN_MESSAGE);
            return;
        }

        sendResults(ctx, req, page);
    }

    @Override
    public void stop(PluginContext ctx) {
        Request req = (Request)ctx.request;
        if (req != null && req.enablePaging()) {
            // unsubscribed - drop the context
            onDone(ctx);
        } else {
            onError(ctx, Errors.PLUGIN_PROTOCOL);
        }
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse res) {
        Request req = (Request)ctx.request;

        if (res.authorized()) {
            // authorized response
            sendResults(ctx, req, req.page());
            if (!req.enablePaging())
                onDone(ctx);
        } else {
            // authorized response
            onError(ctx, Errors.REJECTED, Errors.errorMessage(Errors.REJECTED));
        }

        return null;
    }

    @Override
    public void timeout(PluginContext ctx) {
        // a rare case of subscription expiry
        onError(ctx, Errors.TX_TIMEOUT);
    }

    protected abstract boolean isUserPrivileged(Request req, WalletData.User user);

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        Request req = (Request)ctx.request;
        return isUserPrivileged(req, user);
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        Request req = (Request)ctx.request;
        if (req.enablePaging() && ctx.authRequest == null) {
            onError(ctx, Errors.TX_INVALIDATE);
        }
    }
}
