package org.lndroid.framework.engine;

import java.lang.reflect.Type;

// used by new plugins to talk to the engine
public interface IPluginForegroundCallback {

    // called by plugin when it restores the
    // active transactions from db,
    // if 'false' is returned, transaction must be
    // discarded (user disabled, etc)
    boolean onInit(String pluginId, long userId, PluginContext ctx);

    // called when plugin wants to send response,
    // engine finds the client's messenger
    // and sends the message, returns false if client
    // disconnected, which means plugin should buffer
    // the response and wait until client tries to recover.

    // Problem is: how to we differentiate if client is
    // recovering or is creating a new session?
    // If client reuses the tx then he is obviously recovering?!?
    // If so, then we should RECOVER the tx response even if
    // we already sent it and assumed it was done, bcs client
    // might have failed to process the response and needs it again.
    // So... do we let client recover any tx w/o time limit?
    // YES! And tx timeout is only for Auth, if user doesn't
    // auth within time limit then tx is aborted, all w/o
    // races btw client and caller.
    // So, committed tx is tx whose result is known. Committed
    // tx results are written to db first and then sent to
    // client. If client fails to process it starts new tx
    // w/ same id, server should recover tx response and send it
    // again. Wallet may restrict depth of stored txs to
    // T time (weeks), so it's not a way for client to query tx
    // history.
    // Also, if tx was interrupted by wallet restart, on
    // restart tx should proceed (restart), and result stored
    // for client.
    void onReply(String pluginId, PluginContext ctx, Object r, Type type);

    // called if plugin caller user does not have enough privilege
    // and requires authorization from higher-privileged user.
    // engine must create a 'authorization request' in db,
    // and set it to ctx.authRequest, caller should pass auth request to
    // Auth activity to confirm it
    void onAuth(String pluginId, PluginContext ctx);

    // called when plugin returns an error to client
    void onError(String pluginId, PluginContext ctx, String code, String message);

    // called when tx resources can be released
    void onDone(String pluginId, PluginContext ctx);

    // notify other plugins about some change
    void onSignal(String pluginId, String topic, Object data);
}
