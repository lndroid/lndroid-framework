package org.lndroid.framework.client;

import android.content.Context;

public interface IPluginClient {
    // create a new transaction to interact with a foreground plugin
    IPluginTransaction createTransaction(String pluginId, String txId, IPluginTransactionCallback cb);

    void connect(Context ctx);

}
