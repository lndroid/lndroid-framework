package org.lndroid.framework.engine;

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.Map;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.PluginData;

public class AuthClient extends Handler implements IAuthClient {

    private static class Callback<T> {
        int id;
        IResponseCallback<T> cb;
        Callback(int id, IResponseCallback<T> cb) {
            this.id = id;
            this.cb = cb;
        }
    }

    private Messenger self_;
    private Messenger server_;
    private int nextCallbackId_ = 1;
    private Map<Integer, Callback<?>> callbacks_ = new HashMap<>();

/*    private Map<Integer, IResponseCallback<Boolean>> isUserPrivilegedCallbacks_ = new HashMap<>();
    private Map<Integer, IResponseCallback<Boolean>> authorizedCallbacks_ = new HashMap<>();
    private List<IAuthRequestCallback> authRequestCallbacks_ = new ArrayList<>();
    private List<IWalletStateCallback> walletStateCallbacks_ = new ArrayList<>();
    private IResponseCallback<WalletData.GenSeedResponse> genSeedCallback_;
    private IResponseCallback<WalletData.InitWalletResponse> initWalletCallback_;
    private IResponseCallback<WalletData.UnlockWalletResponse> unlockWalletCallback_;

 */

    public AuthClient(Messenger server) {
        self_ = new Messenger(this);
        server_ = server;
    }

    private void onCallback(AuthData.AuthMessage pm) {
        Callback cb = callbacks_.get(pm.id());
        if (cb == null)
            throw new RuntimeException("Auth callback not found");

        if (pm.code() != null)
            cb.cb.onError(pm.code(), pm.error());
        else
            cb.cb.onResponse(pm.data());
    }

    @Override
    public void handleMessage(Message msg) {
        AuthData.AuthMessage pm = (AuthData.AuthMessage)msg.obj;

        switch(pm.type()) {
            case AuthData.MESSAGE_TYPE_PRIV:
            case AuthData.MESSAGE_TYPE_AUTHED:
            case AuthData.MESSAGE_TYPE_AUTH_SUB:
            case AuthData.MESSAGE_TYPE_WALLET_STATE_SUB:
            case AuthData.MESSAGE_TYPE_GEN_SEED:
            case AuthData.MESSAGE_TYPE_INIT_WALLET:
            case AuthData.MESSAGE_TYPE_UNLOCK_WALLET:
            case AuthData.MESSAGE_TYPE_GET:
            case AuthData.MESSAGE_TYPE_GET_TX:
            case AuthData.MESSAGE_TYPE_USER_AUTH_INFO:
            case AuthData.MESSAGE_TYPE_CREATE_ROOT:
                onCallback(pm);
                break;

            default:
                throw new RuntimeException("Unexpected auth client response");
        }
    }

    private void send(AuthData.AuthMessage pm) {

        Message m = this.obtainMessage(PluginData.MESSAGE_WHAT_AUTH, pm);

        // set self as client
        m.replyTo = self_;

        try {
            server_.send(m);
        } catch (RemoteException e) {
            throw new RuntimeException("Server is gone");
        }
    }

    private <T> void send(AuthData.AuthMessage.Builder b, IResponseCallback<T> cb) {
        final int id = nextCallbackId_;
        nextCallbackId_++;

        callbacks_.put(id, new Callback<T>(id, cb));

        b.setId(id);

        send(b.build());
    }

    @Override
    public void isUserPrivileged(String pluginId, long authUserId, long authId, IResponseCallback<Boolean> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_PRIV)
                .setAuthId(authId)
                .setUserId(authUserId);
        send(b, cb);
    }

    @Override
    public void subscribeBackgroundAuthRequests(IResponseCallback<WalletData.AuthRequest> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_AUTH_SUB);

        send(b, cb);
    }

    @Override
    public void subscribeWalletState(IResponseCallback<WalletData.WalletState> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_WALLET_STATE_SUB);
        send(b, cb);
    }

    private <T> void sendWalletMessage(int type, Object r, IResponseCallback<T> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(type)
                .setData(r);
        send(b, cb);
    }

    @Override
    public void getUserAuthInfo(long userId, IResponseCallback<WalletData.User> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_USER_AUTH_INFO)
                .setUserId(userId);
        send(b, cb);
    }

    @Override
    public void genSeed(WalletData.GenSeedRequest r, IResponseCallback<WalletData.GenSeedResponse> cb) {
        sendWalletMessage(AuthData.MESSAGE_TYPE_GEN_SEED, r, cb);
    }

    @Override
    public void initWallet(WalletData.InitWalletRequest r, IResponseCallback<WalletData.InitWalletResponse> cb) {
        sendWalletMessage(AuthData.MESSAGE_TYPE_INIT_WALLET, r, cb);
    }

    @Override
    public void unlockWallet(WalletData.UnlockWalletRequest r, IResponseCallback<WalletData.UnlockWalletResponse> cb) {
        sendWalletMessage(AuthData.MESSAGE_TYPE_UNLOCK_WALLET, r, cb);
    }

    @Override
    public void authorize(WalletData.AuthResponse res, IResponseCallback<Boolean> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_AUTHED)
                .setAuthId(res.authId())
                .setUserId(res.authUserId())
                .setData(res);

        send(b, cb);
    }

    @Override
    public void getAuthRequest(long id, IResponseCallback<WalletData.AuthRequest> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_GET)
                .setAuthId(id);

        send(b, cb);
    }

    @Override
    public <T> void getAuthTransactionRequest(long authId, Class<T> cls, IResponseCallback<T> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_GET_TX)
                .setAuthId(authId)
                .setData(cls);

        send(b, cb);
    }


    @Override
    public void createRoot(WalletData.AddUserRequest req, IResponseCallback<WalletData.User> cb) {
        AuthData.AuthMessage.Builder b = AuthData.AuthMessage.builder()
                .setType(AuthData.MESSAGE_TYPE_CREATE_ROOT)
                .setData(req);

        send(b, cb);
    }
}
