package org.lndroid.framework.client;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;

public interface IPluginTransactionCallback {
    // for remote calls,
    void onResponse(IPluginData r);
    void onAuth(WalletData.AuthRequest r); // called if authorization is required
    void onAuthed(WalletData.AuthResponse r); // might be used by input streams as an indication to start streaming
    void onError(String code, String message); // on error or if server terminated the tx
}
