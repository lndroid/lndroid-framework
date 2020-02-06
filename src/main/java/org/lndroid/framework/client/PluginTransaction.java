package org.lndroid.framework.client;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.common.Errors;

// implementation of IPluginTransaction,
// closely tied to PluginClient
class PluginTransaction implements IPluginTransaction {

    private String pluginId_;
    private WalletData.UserIdentity userIdentity_;
    private PluginClient client_;
    private String txId_;
    private IPluginTransactionCallback cb_;
    private boolean started_;

    PluginTransaction(String pluginId,
                      WalletData.UserIdentity userId,
                      String txId,
                      IPluginTransactionCallback cb,
                      PluginClient client) {
        pluginId_ = pluginId;
        userIdentity_ = userId;
        client_ = client;
        txId_ = txId;
        cb_ = cb;
    }

    @Override
    public String pluginId() {
        return pluginId_;
    }

    @Override
    public String id() {
        return txId_;
    }

    @Override
    public boolean isActive() {
        return started_;
    }

    public void onIpcError() {
        cb_.onError(Errors.IPC_ERROR, Errors.errorMessage(Errors.IPC_ERROR));
        started_ = false;
    }

    private void send(PluginData.PluginMessage pm) {
        client_.send(this, pm);
    }

    @Override
    public void start(Object r, Type type) {
        start(r, type,0);
    }

    @Override
    public void start(Object r, Type type, long timeout) {
        if (started_)
            throw new RuntimeException("Tx already started");

        started_ = true;

        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_START)
                .setPluginId(pluginId_)
                .setTxId(txId_)
                .setUserIdentity(userIdentity_)
                .setTimeout(timeout)
                .build();
        pm.assignData(r, type);

        send(pm);
    }

    @Override
    public void send(Object r, Type type) {
        if (!started_)
            throw new RuntimeException("Tx not started");

        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_REQUEST)
                .setPluginId(pluginId_)
                .setTxId(txId_)
                .setUserIdentity(userIdentity_)
                .build();
        pm.assignData(r, type);

        send(pm);
    }

    @Override
    public void stop() {
        if (!started_)
            throw new RuntimeException("Tx not started");

        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_STOP)
                .setPluginId(pluginId_)
                .setTxId(txId_)
                .setUserIdentity(userIdentity_)
                .build();

        send(pm);

        // tx not usable any more
        started_ = false;
    }

    private <T> T getData(PluginData.PluginMessage pm, Class<T> cls) {
        pm.assignDataType(cls);
        try {
            return pm.getData();
        } catch (IOException e) {
            cb_.onError(Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
            return null;
        }
    }

    void handleMessage(PluginData.PluginMessage pm) {
        switch(pm.type()) {
            case PluginData.MESSAGE_TYPE_REPLY:
                cb_.onResponse(pm);
                break;

            case PluginData.MESSAGE_TYPE_ERROR:
                // tx not usable any more
                started_ = false;
                cb_.onError(pm.code(), pm.error());
                break;

            case PluginData.MESSAGE_TYPE_DONE:
                // tx not usable any more
                started_ = false;
                cb_.onError(Errors.TX_DONE, Errors.errorMessage(Errors.TX_DONE));
                break;

            case PluginData.MESSAGE_TYPE_AUTH: {
                WalletData.AuthRequest ar = getData(pm, WalletData.AuthRequest.class);
                if (ar != null)
                    cb_.onAuth(ar);
                break;
            }

            case PluginData.MESSAGE_TYPE_AUTHED: {
                WalletData.AuthResponse ar = getData(pm, WalletData.AuthResponse.class);
                if (ar != null)
                    cb_.onAuthed(ar);
                break;
            }

            default:
                throw new RuntimeException("Bad plugin response");
        }
    }

    @Override
    public void destroy() {
        client_.releaseTransaction(this);
    }

}
