package org.lndroid.framework.client;

import android.content.Context;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IResponseCallback;

public interface IPluginClient {
    // create a new transaction to interact with a foreground plugin
    IPluginTransaction createTransaction(String pluginId, String txId, IPluginTransactionCallback cb);

    void setOnError(IResponseCallback<WalletData.Error> cb);

    // connect to server over IPC
    void connect(Context ctx);

    // disconnect over IPC when plugin client is no longer needed
    void disconnect(Context ctx);

    // whether client has active token assigned
    boolean haveSessionToken();

    // set token to be used by the following createTransaction calls
    void setSessionToken(String token);

}
