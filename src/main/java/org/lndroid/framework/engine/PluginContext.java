package org.lndroid.framework.engine;

import org.lndroid.framework.WalletData;

// passed to plugin as calling transaction context,
// plugins store their request in 'request' field for convenience
public class PluginContext {

    // user.id+txId must be unique

    // txId of tx
    public String txId;

    // user identity of the caller
    public WalletData.User user;

    // timeout requested by client, plugin might use different value
    public long timeout;

    // auth request is auth is required
    public WalletData.AuthRequest authRequest;

    // auth respose if auth was performed...
    public WalletData.AuthRequest authResponse;

    // deadline calculated by plugin
    public long deadline;

    // reading plugins can use it to store client requests (which do not require persistence),
    // writing plugins shouldn't store tx here as tx is written to db
    // and is persistent.
    public Object request;

    // context marked as deleted by engine (timeout, disconnect, etc)
    // so that plugins' async callbacks could check if
    // replies should be discarded
    public boolean deleted;

    // true if client is communicating over IPC
    public boolean ipc;
}
