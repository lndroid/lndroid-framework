package org.lndroid.framework.plugins;

import java.lang.reflect.Type;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;

public abstract class GetBase<IdType> implements IPluginForeground {

    private String pluginId_;
    private String sourceTopic_;
    private IPluginForegroundCallback engine_;

    GetBase(String pluginId, String sourceTopic) {
        pluginId_ = pluginId;
        sourceTopic_ = sourceTopic;
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

    protected void onError(PluginContext ctx, String code) {
        onError(ctx, code, Errors.errorMessage(code));
    }

    protected void onError(PluginContext ctx, String code, String s) {
        engine_.onError(pluginId_, ctx, code, s);
    }

    protected void onAuth(PluginContext ctx) {
        engine_.onAuth(pluginId_, ctx);
    }

    protected void onReply(PluginContext ctx, Object data, Type type) {
        engine_.onReply(pluginId_, ctx, data, type);
    }

    protected void onDone(PluginContext ctx) {
        WalletDataDecl.GetRequestTmpl<IdType> req = (WalletDataDecl.GetRequestTmpl<IdType>)ctx.request;
        engine_.onDone(pluginId_, ctx, req.subscribe());
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        throw new RuntimeException("Bad input");
    }

    @Override
    public void timeout(PluginContext ctx) {
        // a rare case of subscription expiry
        onError(ctx, Errors.TX_TIMEOUT);
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(sourceTopic_);
    }

    // override these
    protected abstract long defaultTimeout();
    protected abstract boolean isUserPrivileged(PluginContext ctx, WalletDataDecl.GetRequestTmpl<IdType> req, WalletData.User user);
    protected abstract Object get(IdType id);
    protected abstract Type getType();

    protected abstract WalletDataDecl.GetRequestTmpl<IdType> getInputData(IPluginData in);

    @Override
    public void start(PluginContext ctx, IPluginData in) {
        WalletDataDecl.GetRequestTmpl<IdType> req = getInputData(in);
        if (req == null) {
            onError(ctx, Errors.PLUGIN_MESSAGE);
            return;
        }

        // store req and deadline in the context
        ctx.request = req;
        long timeout = ctx.timeout;
        if (timeout == 0) {
            timeout = defaultTimeout();
        }
        ctx.deadline = System.currentTimeMillis() + timeout;

        if (!isUserPrivileged(ctx, ctx.user)) {
            // tell engine we need user auth
            if (req.noAuth())
                onError(ctx, Errors.FORBIDDEN);
            else
                onAuth(ctx);
        } else {
            onReply(ctx, get(req.id()), getType());
            if (!req.subscribe())
                onDone(ctx);
        }
    }

    @Override
    public void stop(PluginContext ctx) {
        WalletDataDecl.GetRequestTmpl<IdType> req = (WalletDataDecl.GetRequestTmpl<IdType>)ctx.request;
        if (req.subscribe()) {
            // unsubscribed - drop the context
            onDone(ctx);
        } else {
            onError(ctx, Errors.PLUGIN_PROTOCOL);
        }
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse res) {
        WalletDataDecl.GetRequestTmpl<IdType> req = (WalletDataDecl.GetRequestTmpl<IdType>)ctx.request;

        if (res.authorized()) {
            // authorized response
            onReply(ctx, get(req.id()), getType());
            if (!req.subscribe())
                onDone(ctx);
        } else {
            // authorized response
            onError(ctx, Errors.REJECTED, Errors.errorMessage(Errors.REJECTED));
        }

        return null;
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        WalletDataDecl.GetRequestTmpl<IdType> req = (WalletDataDecl.GetRequestTmpl<IdType>)ctx.request;
        return isUserPrivileged(ctx, req, user);
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        WalletDataDecl.GetRequestTmpl<IdType> req = (WalletDataDecl.GetRequestTmpl<IdType>)ctx.request;
        if (req.subscribe() && ctx.authRequest == null) {
            onReply(ctx, get(req.id()), getType());
        }
    }

}
