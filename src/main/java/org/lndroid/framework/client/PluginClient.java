package org.lndroid.framework.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.ICodec;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.common.IVerifier;
import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.common.ICodecProvider;
import org.lndroid.framework.common.PluginUtils;

class PluginClient extends Handler implements IPluginClient {

    private static final String TAG = "PluginClient";

    private Messenger self_;
    private Messenger server_;
    private WalletData.UserIdentity userId_;
    private boolean ipc_;
    private ICodecProvider ipcCodecProvider_;
    private ICodec<PluginData.PluginMessage> ipcPluginMessageCodec_;
    private ISigner signer_;
    private IVerifier verifier_;
    private IResponseCallback<WalletData.Error> onError_;

    private String servicePackageName_;
    private String serviceClassName_;
    private String servicePubkey_;

    private boolean bound_;
    private Map<String, Plugin> plugins_ = new HashMap<>();
    private String sessionToken_;

    private ServiceConnection connection_;
    private Queue<Pair<WeakReference<PluginTransaction>, PluginData.PluginMessage>> queue_ = new LinkedList<>();


    class Plugin {

        private String pluginId_;
        private PluginClient client_;
        private Map<String, WeakReference<PluginTransaction>> transactions_ = new HashMap<>();

        Plugin(String pluginId, PluginClient client) {
            pluginId_ = pluginId;
            client_ = client;
        }

        PluginTransaction createTransaction(String txId, IPluginTransactionCallback cb) {
            if (txId.isEmpty()) // FIXME use GUID?
                txId = pluginId_ + "_"+System.currentTimeMillis();

            PluginTransaction t = new PluginTransaction(pluginId_, txId, userId_, cb, client_);
            transactions_.put(txId, new WeakReference<PluginTransaction>(t));

            return t;
        }

        PluginTransaction getTransaction(String txId) {
            WeakReference<PluginTransaction> ref = transactions_.get(txId);
            return ref != null ? ref.get() : null;
        }

        void releaseTransaction(String id) {
            Log.i(TAG, "transaction released "+id);
            transactions_.remove(id);
        }
    }

    public PluginClient(WalletData.UserIdentity userId, Messenger server, boolean ipc,
                        ICodecProvider ipcCodecProvider,
                        ISigner signer,
                        IVerifier verifier,
                        String servicePackageName, String serviceClassName, String servicePubkey) {
        userId_ = userId;
        self_ = new Messenger(this);
        server_ = server;
        ipc_ = ipc;
        ipcCodecProvider_ = ipcCodecProvider;
        ipcPluginMessageCodec_ = ipcCodecProvider_.get(PluginData.PluginMessage.class);
        signer_ = signer;
        verifier_ = verifier;
        servicePackageName_ = servicePackageName;
        serviceClassName_ = serviceClassName;
        servicePubkey_ = servicePubkey;

        if (ipc_) {
            connection_ = createConnection();
        }
    }

    @Override
    public boolean haveSessionToken() {
        // FIXME clear token if we get server error of 'bad token'
        return sessionToken_ != null;
    }

    @Override
    public void setSessionToken(String token) {
        sessionToken_ = token;
        sendQueuedMessages();
    }

    private boolean canSend() {
        return (ipc_ && bound_)
                || (!ipc_ && sessionToken_ != null);
    }

    private void sendQueuedMessages() {
        while(canSend()) {
            Pair<WeakReference<PluginTransaction>, PluginData.PluginMessage> p = queue_.poll();
            if (p == null)
                break;

            // tx might be already gone
            PluginTransaction tx = p.first.get();
            if (tx != null) {
                if (!send(tx, p.second))
                    break;
            }
        }
    }

    private ServiceConnection createConnection() {
        return new ServiceConnection() {
            @Override
            public void onNullBinding(ComponentName componentName) {
                Log.e(TAG, "wallet service returned null binding");
                throw new RuntimeException("Wallet service does not support binding");
            }
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.i(TAG, "connected");
                server_ = new Messenger(service);
                bound_ = true;
                sendQueuedMessages();
            }
            @Override
            public void onServiceDisconnected(ComponentName className) {
                server_ = null;
                bound_ = false;
                Log.i(TAG, "disconnected");
            }
        };
    }

    @Override
    public IPluginTransaction createTransaction(String pluginId, String txId, IPluginTransactionCallback cb) {
        Plugin p = plugins_.get(pluginId);
        if (p == null) {
            p = new Plugin(pluginId, this);
            plugins_.put(pluginId, p);
        }
        return p.createTransaction(txId, cb);
    }

    @Override
    public void setOnError(IResponseCallback<WalletData.Error> cb) {
        onError_ = cb;
    }

    public void releaseTransaction(IPluginTransaction tx) {
        Plugin p = plugins_.get(tx.pluginId());
        if (p == null)
            throw new RuntimeException("Unknown plugin");
        p.releaseTransaction(tx.id());
    }

    public void handlePluginMessage(PluginData.PluginMessage pm) {
        if (PluginData.MESSAGE_TYPE_ERROR.equals(pm.type()))
            Log.e(TAG, "received error tx "+pm.txId()+" e "+pm.code()+" plugin "+pm.pluginId());
        else
            Log.i(TAG, "received tx "+pm.txId()+" type "+pm.type()+" plugin "+pm.pluginId());

        if (PluginData.MESSAGE_TYPE_ERROR.equals(pm.type())) {

            if (Errors.IPC_ERROR.equals(pm.code())) {
                // FIXME reconnect!
            }

            if (Errors.IPC_IDENTITY_ERROR.equals(pm.code())
                    || Errors.MESSAGE_AUTH.equals(pm.code())
                    || Errors.DEVICE_LOCKED.equals(pm.code())
            ) {
                // notify UI:
                //  - identity error should cause UI to restart connect-to-wallet flow
                //  - message_auth error should force requesting a new token
                //  - device_locked error should hint that subscribed read plugins need to restart
                if (onError_ != null) {
                    onError_.onResponse(WalletData.Error.builder()
                        .setCode(pm.code())
                        .setMessage(pm.error())
                        .build());
                }
            }
        }

        Plugin p = plugins_.get(pm.pluginId());
        PluginTransaction tx = p.getTransaction(pm.txId());
        if (tx != null) {

            // reset client's session token if server said it's no longer valid
            if (PluginData.MESSAGE_TYPE_ERROR.equals(pm.type())
                    && Errors.MESSAGE_AUTH.equals(pm.code())
                    && (tx.sessionToken() == null
                        || tx.sessionToken().equals(sessionToken_)
                    )
                )
                sessionToken_ = null;

            tx.handleMessage(pm);
        } else {
            Log.i(TAG, "message for unknown tx "+pm.txId()+" plugin "+pm.pluginId()+" dropped");
            p.releaseTransaction(pm.txId());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        PluginData.PluginMessage pm = (PluginData.PluginMessage) msg.obj;
        if (ipc_) {

            String code = PluginUtils.checkPluginMessageIpc(
                    msg.getData(), servicePubkey_, verifier_);
            if (code != null) {
                Log.e(TAG, "bad server message "+code+" expected pubkey "+servicePubkey_);
                Plugin p = plugins_.get(pm.pluginId());
                PluginTransaction tx = p.getTransaction(pm.txId());
                tx.onIpcIdentityError();
                return;
            }

            pm = PluginUtils.decodePluginMessageIpc(msg.getData(), ipcPluginMessageCodec_);
            if (pm != null)
                pm.assignCodecProvider(ipcCodecProvider_);
        }

        if (pm != null)
            handlePluginMessage(pm);
        else
            Log.i(TAG, "empty plugin client message received");
    }

    public boolean send(PluginTransaction tx, Message m) {
        try {
            m.replyTo = self_;
            server_.send(m);
            return true;
        } catch (RemoteException e) {
            // inform client, caller should try to restore the tx
            Log.e(TAG, "send failed: "+e);
            return false;
        }
    }

    private boolean sendLocal(PluginTransaction tx, PluginData.PluginMessage msg) {
        if (msg.sessionToken() != null || sessionToken_ != null) {
            if (msg.sessionToken() == null)
                msg.assignSessionToken(sessionToken_);

            Message m = this.obtainMessage(PluginData.MESSAGE_WHAT_LOCAL_TX, msg);
            send(tx, m);
            return true;
        } else {
            queue_.add(Pair.create(new WeakReference<>(tx), msg));
            return false;
        }
    }

    private boolean sendIpc(PluginTransaction tx, PluginData.PluginMessage msg) {

        if (bound_) {
            Bundle b = PluginUtils.encodePluginMessageIpc(msg, ipcCodecProvider_, ipcPluginMessageCodec_, signer_);

            // prepare message with the bundle
            Message m = this.obtainMessage(PluginData.MESSAGE_WHAT_IPC_TX);
            m.setData(b);

            // send message over IPC
            return send(tx, m);
        } else {
            queue_.add(Pair.create(new WeakReference<>(tx), msg));
            return false;
        }
    }

    public boolean send(PluginTransaction tx, PluginData.PluginMessage msg) {
        Log.i(TAG, "sending tx "+tx.id()+" type "+msg.type()+" plugin "+msg.pluginId());
        if (ipc_)
            return sendIpc(tx, msg);
        else
            return sendLocal(tx, msg);
    }

    @Override
    public void connect(Context ctx) {
        ComponentName comp = new ComponentName(servicePackageName_, serviceClassName_);
        Intent intent = new Intent();
        intent.setComponent(comp);
        final boolean ok = ctx.bindService(intent, connection_, Context.BIND_AUTO_CREATE);
        if (!ok)
            throw new RuntimeException("No permission to bind to wallet service, or service not found");
    }

    @Override
    public void disconnect(Context ctx) {
        if (connection_ != null)
            ctx.unbindService(connection_);
        connection_ = null;
        bound_ = false;
    }
}
