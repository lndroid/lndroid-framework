package org.lndroid.framework.engine;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;

public interface IPluginForeground {
    String id();
    // set the engine
    void init(IDaoProvider dp, IPluginForegroundCallback cb);

    // let plugin do internal work, like GC etc
    void work();

    // must do initial writes to db (if needed),
    // and then check user privileges, and if need confirmation - calls onAuth,
    // when reply is ready call onResponse, when done or error call onDone
    void start(PluginContext ctx, IPluginData in);
    // called when further input is received
    void receive(PluginContext ctx, IPluginData in);
    // when client decided to stop subscription or terminate input stream
    void stop(PluginContext ctx);
    // informs the plugin that this tx was confirmed/rejected,
    // plugin might return an error if provided response ('data' in particular)
    // are invalid
    WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse r);
    // called when tx is timed-out by server and so user will
    // no longer be able to Auth this tx (if Auth was in fact requested),
    // txs that simply take long time due to async nature are not affected
    // and will complete properly.... Will they? currently we commit tx
    // w/ 'timeout' state, we probably shouldn't do it if tx
    // is actually executing
    void timeout(PluginContext ctx);

    // check if supplied userIdentity has enough priviledge
    // to auth the tx, should return true
    // if user can actually confirm/reject this request,
    // should always return 'true' for userIdentity==1 (root)
    // NOTE: this call must not rely on any plugin state and
    // should fetch all data from dao, bcs it might be called during
    // plugin.init() and thus 'init' phase might not have completed yet!
    boolean isUserPrivileged(PluginContext ctx, WalletData.User user);

    // add list of topics to subscribe to changes generated by other plugins
    void getSubscriptions(List<String> topics);

    // notify context about topic update
    void notify(PluginContext ctx, String topic, Object data);
}