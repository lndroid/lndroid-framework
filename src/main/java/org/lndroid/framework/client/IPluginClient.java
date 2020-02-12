package org.lndroid.framework.client;

import android.content.Context;

public interface IPluginClient {
    // create a new transaction to interact with a foreground plugin
    IPluginTransaction createTransaction(String pluginId, String txId, IPluginTransactionCallback cb);

    // connect to server over IPC
    void connect(Context ctx);

    // set token to be used by the following createTransaction calls
    void setSessionToken(String token);

}
