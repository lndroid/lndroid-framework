package org.lndroid.framework.engine;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.ICodec;
import org.lndroid.framework.common.ICodecProvider;
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.common.PluginUtils;
import org.lndroid.framework.dao.IAuthDao;
import org.lndroid.framework.dao.IAuthRequestDao;
import org.lndroid.framework.plugins.Transaction;

class PluginServer extends Handler implements IPluginServer, IPluginForegroundCallback, IPluginBackgroundCallback {

    private static final String TAG = "PluginServer";
    private static final int WORK_INTERVAL = 100; // ms

    private static class AuthSub {
        WeakReference<Messenger> client;
        int id;
        AuthSub(Messenger c, int id) {
            client = new WeakReference<>(c);
            this.id = id;
        }
    }

    private IPluginProvider pluginProvider_;
    private IDaoConfig daoConfig_;
    private IDaoProvider daoProvider_;
    private ICodecProvider ipcCodecProvider_;
    private ICodec<PluginData.PluginMessage> ipcPluginMessageCodec_;
    private IAuthComponentProvider authComponentProvider_;
    private IKeyStore keyStore_;
    private IIdGenerator idGenerator_;
    private IAuthDao authDao_;
    private IAuthRequestDao authRequestDao_;
    private Map<Long,Integer> authRequestHistory_ = new HashMap<>();
    private List<AuthSub> authRequestSubscribers_ = new ArrayList<>();
    private List<AuthSub> walletStateSubscribers_ = new ArrayList<>();
    private List<WalletData.AuthRequest> pendingAuthRequests_ = new ArrayList<>();
    private WalletData.WalletState walletState_;
    private boolean started_;

    private static class Context {
        // local clients are referenced weakly, as we shouldn't
        // hold them from being GCed
        WeakReference<Messenger> client;
        // remote clients are stubs that are only referenced by
        // this, and thus we should hold strong refs otherwise GC will
        // release them. this means that it's possible to DDOS the
        // server by creating many remote contexts each w/ new messenger
        // that will be holded indefinitely, so tx timeouts and
        // tx count limits must be strict.
        Messenger ipcClient;
        PluginContext ctx;
    }

    private static class Contexts {
        Map<String, Context> contexts = new HashMap<>();
    }

    private static class Caller {
        WalletData.User user;
        Map<String, Contexts> pluginContexts = new HashMap<>();

        Contexts getContexts(String pluginId) {
            Contexts ctxs = pluginContexts.get(pluginId);
            if (ctxs == null) {
                ctxs = new Contexts();
                pluginContexts.put(pluginId, ctxs);
            }
            return ctxs;
        }
    }

    private Map<Long, Caller> callers_ = new HashMap<>();
    private Map<String, Set<String>> subscribers_ = new HashMap<>();

    PluginServer(IPluginProvider pluginProvider,
                 IDaoConfig daoConfig,
                 IDaoProvider daoProvider,
                 ICodecProvider ipcCodecProvider,
                 IAuthComponentProvider authComponentProvider,
                 IKeyStore keyStore,
                 IIdGenerator idGenerator
    ){
        pluginProvider_ = pluginProvider;
        daoConfig_ = daoConfig;
        daoProvider_ = daoProvider;
        ipcCodecProvider_ = ipcCodecProvider;
        ipcPluginMessageCodec_ = ipcCodecProvider_.get(PluginData.PluginMessage.class);
        authComponentProvider_ = authComponentProvider;
        keyStore_ = keyStore;
        idGenerator_ = idGenerator;
    }

    @Override
    public IDaoConfig getDaoConfig() {
        return daoConfig_;
    }

    @Override
    public IDaoProvider getDaoProvider() {
        return daoProvider_;
    }

    @Override
    public IKeyStore getKeyStore() {
        return keyStore_;
    }

    @Override
    public IIdGenerator getIdGenerator() {
        return idGenerator_;
    }

    private void workTransactionDeadlines() {
        //FIXME use some time-based queue to avoid scanning the whole tree
        List<Pair<IPluginForeground, PluginContext>> expired = new ArrayList<>();
        for(Map.Entry<Long, Caller> e: callers_.entrySet()) {
            for (Map.Entry<String, Contexts> pe: e.getValue().pluginContexts.entrySet()) {
                IPluginForeground p = getPluginForeground(pe.getKey());
                for (Map.Entry<String, Context> ce: pe.getValue().contexts.entrySet()) {
                    Context ctx = ce.getValue();
                    if (ctx.ctx.deadline < System.currentTimeMillis()) {
                        expired.add(new Pair<>(p, ctx.ctx));

                        // rearm in case plugin can't cancel immediately,
                        // but add pause to avoid retryig too fast
                        ctx.ctx.deadline += 1000;
                    }
                }
            }
        }

        for(Pair<IPluginForeground, PluginContext> p: expired)
            p.first.timeout(p.second);
    }

    private boolean sendAuthMessage(AuthData.AuthMessage pm, Messenger client) {
        try {
            client.send(this.obtainMessage(PluginData.MESSAGE_WHAT_AUTH, pm));
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Auth client disconnected: "+e);
            return false;
        }
    }

    private boolean replyAuthRequest(Messenger client, int id, WalletData.AuthRequest ar) {
        AuthData.AuthMessage pm = AuthData.AuthMessage.builder()
                .setId(id)
                .setType(AuthData.MESSAGE_TYPE_AUTH_SUB)
                .setAuthId(ar.id())
                .setData(ar)
                .build();
        return sendAuthMessage(pm, client);
    }

    private boolean replyWalletState(Messenger client, int id) {
        AuthData.AuthMessage pm = AuthData.AuthMessage.builder()
                .setId(id)
                .setType(AuthData.MESSAGE_TYPE_WALLET_STATE_SUB)
                .setData(walletState_)
                .build();

        return sendAuthMessage(pm, client);
    }

    private void notifyWalletState() {
        Log.i(TAG, "notify wallet state "+walletState_.state());
        ListIterator<AuthSub> i = walletStateSubscribers_.listIterator();
        while(i.hasNext()) {
            AuthSub sub = i.next();
            Messenger client = sub.client.get();
            boolean ok = client != null && replyWalletState(client, sub.id);
            if (!ok) {
                i.remove();
            }
        }
    }

    private void notifyAuthRequest(WalletData.AuthRequest ar) {
        ListIterator<AuthSub> i = authRequestSubscribers_.listIterator();
        while(i.hasNext()) {
            AuthSub sub = i.next();
            Messenger client = sub.client.get();
            boolean ok = client != null && replyAuthRequest(client, sub.id, ar);
            if (!ok) {
                i.remove();
            }
        }
    }

    private void workAuthRequest() {

        for(WalletData.AuthRequest ar: pendingAuthRequests_)
            notifyAuthRequest(ar);

        pendingAuthRequests_.clear();
    }

    private void workBackroundPlugins() {
        for(String pluginId: pluginProvider_.getPluginIds()) {
            IPlugin p = getPlugin(pluginId);
            if (p.getBackground() != null)
                p.getBackground().work();
        }
    }

    private void workForegroundPlugins() {
        for(String pluginId: pluginProvider_.getPluginIds()) {
            IPlugin p = getPlugin(pluginId);
            if (p.getForeground() != null)
                p.getForeground().work();
        }
    }

    private void work() {
        // don't touch plugins until wallet state is ok
        if (walletState_.state() != WalletData.WALLET_STATE_OK)
            return;

        // check deadlines
        workTransactionDeadlines();

        // notify auth clients
        workAuthRequest();

        // let bg plugins work
        workBackroundPlugins();

        // let fg plugins work
        workForegroundPlugins();
    }

    class WorkRunnable implements Runnable {

        @Override
        public void run() {
            work();
            postDelayed(this, WORK_INTERVAL);
        }
    }

    private void start() {

        // can we proceed?
        switch (walletState_.state()) {
            default:
                throw new RuntimeException("Unknown dao provider state "+ walletState_);

            case WalletData.WALLET_STATE_AUTH:
            case WalletData.WALLET_STATE_INIT:
            case WalletData.WALLET_STATE_ERROR:
                return;

            case WalletData.WALLET_STATE_OK:
                // NOTE: this is the only case that we proceed
                break;
        }

        Log.i(TAG, "starting");

        authDao_ = daoProvider_.getAuthDao();
        authRequestDao_ = daoProvider_.getAuthRequestDao();

        authDao_.init();
        authRequestDao_.init();
        daoProvider_.getRawQueryDao();
        for(String pluginId: pluginProvider_.getPluginIds()) {
            daoProvider_.getPluginDao(pluginId).init();
        }

        // init id generator after all daos are initialized
        idGenerator_.init();

        // clean up bg requests, bg plugins should re-generate them
        authRequestDao_.deleteBackgroundRequests();

        // init all plugins
        pluginProvider_.init();
        for(String pluginId: pluginProvider_.getPluginIds()) {
            IPlugin p = pluginProvider_.getPlugin(pluginId);
            p.init(this,this, this);
        }

        // subscribe plugins to topics
        List<String> topics = new ArrayList<>();
        for(String pluginId: pluginProvider_.getPluginIds()) {
            IPlugin p = pluginProvider_.getPlugin(pluginId);

            if (p.getForeground() != null) {
                p.getForeground().getSubscriptions(topics);
            }
            if (p.getBackground() != null) {
                p.getBackground().getSubscriptions(topics);
            }

            for(String t: topics) {
                if (t != null && !t.isEmpty()) {
                    Set<String > plugins = subscribers_.get(t);
                    if (plugins == null) {
                        plugins = new HashSet<>();
                        subscribers_.put(t, plugins);
                    }
                    plugins.add(pluginId);
                    Log.i(TAG, "subscribe p "+pluginId+" topic "+t);
                }
            }
            topics.clear();
        }

        // FIXME check all active foreground auth requests,
        // delete ones that were not attached to a recovered tx

        // notify clients about existing auth requests
        // NO! only bg requests are sent to AuthClient, and those are not persistent
//        for(WalletData.AuthRequest ar: authRequestDao_.getRequests())
//            notifyAuthRequest(ar);

        // schedule bg work
        postDelayed(new WorkRunnable(), WORK_INTERVAL);

        // we only do it once
        started_ = true;
    }

    public void init() {
        keyStore_.init();

        daoProvider_.subscribeWalletState(new IDaoProvider.IWalletStateCallback() {
            @Override
            public void onWalletState(WalletData.WalletState state) {
                Log.i(TAG, "wallet state "+state.state());
                walletState_ = state;
                if (!started_)
                    start();
                
                notifyWalletState();
            }
        });

        // start async dao init which results
        // in a wallet state change that will trigger
        // 'start' above
        daoProvider_.init();
    }

    private IPlugin getPlugin(String pluginId) {
        IPlugin p = pluginProvider_.getPlugin(pluginId);
        if (p == null)
            throw new RuntimeException("Unknown plugin");
        return p;
    }

    private IPluginForeground getPluginForeground(String pluginId) {
        IPluginForeground p = getPlugin(pluginId).getForeground();
        if (p == null)
            throw new RuntimeException("Unknown foreground plugin");
        return p;
    }

    private IPluginBackground getPluginBackground(String pluginId) {
        IPluginBackground p = getPlugin(pluginId).getBackground();
        if (p == null)
            throw new RuntimeException("Unknown background plugin");
        return p;
    }

    private void sendTxError(PluginData.PluginMessage src, boolean ipc, String error,
                             Messenger client) {
        PluginData.PluginMessage r = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_ERROR)
                .setPluginId(src.pluginId())
                .setTxId(src.txId())
                .setCode(error)
                .setError(Errors.errorMessage(error))
                .build();

        sendTxMessage(r, ipc, client, null);
    }

    private String checkAuthenticPluginMessage(
            WalletData.User user, Message msg, PluginData.PluginMessage pm, boolean ipc) {

        if (ipc && !user.isApp()) {
            Log.e(TAG, "ipc message " + msg + " from non-app " + user);
            return Errors.FORBIDDEN;
        }

        if (!ipc && user.isApp()) {
            Log.e(TAG, "local message " + msg + " from app " + user);
            return Errors.FORBIDDEN;
        }

        if (ipc) {
            String code = PluginUtils.checkPluginMessageIpc(
                    msg.getData(), user.appPubkey(), keyStore_.getVerifier());

            if (code != null) {
                Log.e(TAG, "bad ipc message " + msg + " from "+user+" code " + code);
                return code;
            }
        } else {
            // bg role should be able to run in locked device!
//            if (!WalletData.USER_ROLE_BG.equals(user.role()) && keyStore_.isDeviceLocked()){
//                Log.e(TAG, "bad local message " + msg + " from "+user+" on locked device");
//                return Errors.DEVICE_LOCKED;
//            }

            String code = PluginUtilsLocal.checkPluginMessageLocal(
                    pm, user.pubkey(), keyStore_.getVerifier());

            if (code != null) {
                Log.e(TAG, "bad local message " + msg + " from "+user+" code " + code);
                return code;
            }
        }

        return null;
    }

    private boolean isAuthenticAuthMessage(
            WalletData.User user, AuthData.AuthMessage pm) {
        if (user.isApp())
            return false;

        // FIXME shall we use session tokens too?
        return true;
    }

    private boolean checkLocalPluginMessage(Message msg, PluginData.PluginMessage pm) {
        if (pm == null)
            throw new RuntimeException("Message not provided");

        if (msg.replyTo == null)
            throw new RuntimeException("Client not provided");

        if (pm.userIdentity() == null)
            throw new RuntimeException("User identity not provided");

        if (pm.userIdentity().userId() == 0)
            throw new RuntimeException("User id not provided");

        if (pm.sessionToken() == null)
            throw new RuntimeException("User session not provided");

        return true;
    }

    private boolean checkIPCPluginMessage(Message msg, PluginData.PluginMessage pm) {

        if (pm == null)
            return false;

        if (msg.replyTo == null)
            return false;

        if (pm.userIdentity() == null) {
            sendTxError(pm, true, Errors.MESSAGE_FORMAT, msg.replyTo);
            return false;
        }

        if (pm.userIdentity().appPubkey() == null) {
            sendTxError(pm, true, Errors.MESSAGE_FORMAT, msg.replyTo);
            return false;
        }

        // FIXME get caller package name (packageManager().getNameForUID(msg.ui)) and compare to the one specified
        //  in the message to drop invalid messages (like when someone stole the app keys but couldn't
        //  fake his UID identity on OS level)

        // protect from replays (only applicable for IPC where the full message is signed)
        final long MAX_TTL = 10000; // ms
        final long now = System.currentTimeMillis();
        if (pm.timestamp() > now || pm.timestamp() < (now - MAX_TTL)) {
            sendTxError(pm, true, Errors.MESSAGE_FORMAT, msg.replyTo);
            return false;
        }

        return true;
    }


    private void onPluginMessage(Message msg) {

        PluginData.PluginMessage pm = null;

        // make sure obj is of proper type, to protect from IPC attackers
        // that set obj to some auto-serializeable class like String
        if (msg.obj instanceof PluginData.PluginMessage)
            pm = (PluginData.PluginMessage)msg.obj;

        final boolean ipc = pm == null;
        if (ipc) {
            // deserialize IPC message
            pm = PluginUtils.decodePluginMessageIpc(msg.getData(), ipcPluginMessageCodec_);
        }

        // basic checks
        if (ipc) {
            if (!checkIPCPluginMessage(msg, pm))
                return;
        } else {
            if (!checkLocalPluginMessage(msg, pm))
                return;
        }

        // assign codecs
        if (ipc)
            pm.assignCodecProvider(ipcCodecProvider_);

        Log.i(TAG, "Plugin server message plugin "+pm.pluginId()+" type "+pm.type());

        switch (walletState_.state()) {
            case WalletData.WALLET_STATE_ERROR:
                sendTxError(pm, ipc, Errors.WALLET_ERROR, msg.replyTo);
                return;
            case WalletData.WALLET_STATE_INIT:
                sendTxError(pm, ipc, Errors.NO_WALLET, msg.replyTo);
                return;
            case WalletData.WALLET_STATE_AUTH:
                sendTxError(pm, ipc, Errors.WALLET_LOCKED, msg.replyTo);
                return;
            case WalletData.WALLET_STATE_OK:
                break;
            default:
                throw new RuntimeException("Unknown dao state");
        }

        IPluginForeground p = getPluginForeground(pm.pluginId());

        WalletData.User user = ipc
                ? authDao_.getByAppPubkey(pm.userIdentity().appPubkey())
                : authDao_.get(pm.userIdentity().userId());
        if (user == null) {
            sendTxError(pm, ipc, Errors.UNKNOWN_CALLER, msg.replyTo);
            return;
        }

        final String authCode = checkAuthenticPluginMessage(user, msg, pm, ipc);
        if (authCode != null) {
            sendTxError(pm, ipc, authCode, msg.replyTo);
            return;
        }

        // ensure caller
        Caller c = callers_.get(user.id());
        if (c == null) {
            c = new Caller();
            c.user = user;
            callers_.put(user.id(), c);
        }

        // get context
        Contexts ctxs = c.getContexts(p.id());
        Context ctx = ctxs.contexts.get(pm.txId());

        // ensure context
        if (ctx == null) {
            if (PluginData.MESSAGE_TYPE_START.equals(pm.type())) {
                ctx = new Context();
                ctx.ctx = new PluginContext();
                ctx.ctx.txId = pm.txId();
                ctx.ctx.timeout = pm.timeout() != null ? pm.timeout() : 0;
                ctx.ctx.user = c.user;

                ctxs.contexts.put(pm.txId(), ctx);
            } else {
                // this case is a result of a common race condition
                // where server has replied w/ done/error while client was sending
                // new messages, and thus server already released this tx,
                // and these tail messages must be ignored now.
                Log.i(TAG, "message for done tx type "+pm.type()+" plugin "+pm.pluginId()+" user "+c.user.id()+" tx "+pm.txId());
                return;
            }
        }

        // make sure restored context gets proper
        // ipc flag when client retries
        ctx.ctx.ipc = ipc;

        // by holding an additional strong ref to ipc clients
        // which are local stubs owned by this, we avoid
        // clients being GCed prematurely
        if (ipc)
            ctx.ipcClient = msg.replyTo;

        // store client using weakref
        ctx.client = new WeakReference<>(msg.replyTo);

        // process tx messages
        switch(pm.type()) {
            case PluginData.MESSAGE_TYPE_START:
                p.start(ctx.ctx, pm);
                break;

            case PluginData.MESSAGE_TYPE_REQUEST:
                p.receive(ctx.ctx, pm);
                break;

            case PluginData.MESSAGE_TYPE_STOP:
                p.stop(ctx.ctx);
                break;

            default:
                throw new RuntimeException("Bad transaction request");
        }
    }

    private void onAuthMessage(Message msg) {
        AuthData.AuthMessage pm = null;

        if (msg.obj instanceof AuthData.AuthMessage)
            pm = (AuthData.AuthMessage)msg.obj;

        if (pm == null) {
            Log.w(TAG, "Auth message w/ empty payload received");
            return;
        }

        // FIXME check caller uid (somehow) to make sure it's not remote call!

        if (msg.replyTo == null)
            throw new RuntimeException("Client not provided");

        Log.i(TAG, "Auth message authId " + pm.authId() + " type " + pm.type());

        switch (pm.type()) {
            case AuthData.MESSAGE_TYPE_AUTHED:
            case AuthData.MESSAGE_TYPE_PRIV:
                onAuthRequestMessage(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_AUTH_SUB:
                onAuthSubscribeMessage(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_WALLET_STATE_SUB:
                onWalletStateSubscribeMessage(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_GEN_SEED:
                onGenSeedMessage(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_INIT_WALLET:
                onInitWalletMessage(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_UNLOCK_WALLET:
                onUnlockWalletMessage(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_GET:
                onGetAuthRequest(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_GET_TX:
                onGetAuthTxRequest(pm, msg.replyTo);
                break;

            case AuthData.MESSAGE_TYPE_USER_AUTH_INFO:
                onGetUserAuthTypeRequest(pm, msg.replyTo);
                break;

            default:
                throw new RuntimeException("Bad plugin request");
        }
    }

    private void replyAuthError(AuthData.AuthMessage src, String code, Messenger client) {
        replyAuthError(src, code, code != null ? Errors.errorMessage(code) : null, client);
    }

    private void replyAuthError(AuthData.AuthMessage src, String code, String message, Messenger client) {
        AuthData.AuthMessage m = AuthData.AuthMessage.builder()
                .setId(src.id())
                .setType(src.type())
                .setCode(code)
                .setError(message)
                .setAuthId(src.authId())
                .build();

        sendAuthMessage(m, client);
    }

    private void onAuthedMessage(AuthData.AuthMessage pm, WalletData.AuthRequest ar, Messenger client) {
        IPlugin p = getPlugin(ar.pluginId());

        WalletData.AuthResponse r = (WalletData.AuthResponse)pm.data();
        if (r == null)
            throw new RuntimeException("Auth response not specified");

        WalletData.Error e = null;
        if (ar.background())
            authBackground(p.getBackground(), ar, r);
        else
            e = authForeground(p.getForeground(), ar, r);

        if (e != null)
            replyAuthError(pm, e.code(), e.message(), client);
        else
            replyAuthError(pm, null, client);
    }

    private void onPrivMessage(AuthData.AuthMessage pm, WalletData.AuthRequest ar, WalletData.User user, Messenger client) {

        // all auth requests must refer to active tx, which
        // means we must have active PluginContext, and if not
        // then tx has expired

        Caller c = callers_.get(ar.userId());
        Contexts ctxs = null;
        Context ctx = null;

        if (c != null)
            ctxs = c.pluginContexts.get(ar.pluginId());
        if (ctxs != null)
            ctx = ctxs.contexts.get(ar.txId());

        AuthData.AuthMessage.Builder builder = AuthData.AuthMessage.builder()
                .setId(pm.id())
                .setType(AuthData.MESSAGE_TYPE_PRIV)
                .setPluginId(ar.pluginId())
                .setAuthId(ar.id());

        if (ctx != null) {
            IPlugin p = getPlugin(ar.pluginId());
            builder.setData(new Boolean(ar.background()
                    ? p.getBackground().isUserPrivileged(user, ar.type())
                    : p.getForeground().isUserPrivileged(ctx.ctx, user)));
        } else {
            // the only reason tx is absent is if it has timed out
            builder
                    .setCode(Errors.TX_TIMEOUT)
                    .setError(Errors.errorMessage(Errors.TX_TIMEOUT));
        }

        sendAuthMessage(builder.build(), client);
    }

    private void onGetUserAuthTypeRequest(AuthData.AuthMessage pm, Messenger client) {
        WalletData.User u = authDao_.getAuthInfo(pm.userId());
        AuthData.AuthMessage m = AuthData.AuthMessage.builder()
                .setId(pm.id())
                .setType(pm.type())
                .setData(u)
                .build();

        sendAuthMessage(m, client);
    }

    private void onGetAuthRequest(AuthData.AuthMessage pm, Messenger client) {
        WalletData.AuthRequest ar = authRequestDao_.get(pm.authId());
        if (ar == null) {
            Log.e(TAG, "unknown auth request "+pm.authId());
            replyAuthError(pm, Errors.AUTH_INPUT, client);
            return;
        }

        replyAuthRequest(client, pm.id(), ar);
    }

    private void onGetAuthTxRequest(AuthData.AuthMessage src, Messenger client) {
        Object o = authRequestDao_.getTransactionRequest(src.userId(), src.txId(), (Class<?>)src.data());
        if (o == null) {
            Log.e(TAG, "unknown auth request transaction u "+src.userId()+" tx "+src.txId());
            replyAuthError(src, Errors.AUTH_INPUT, client);
            return;
        }

        AuthData.AuthMessage m = AuthData.AuthMessage.builder()
                .setId(src.id())
                .setType(src.type())
                .setData(o)
                .build();

        sendAuthMessage(m, client);
    }

    private void onAuthRequestMessage(AuthData.AuthMessage pm, Messenger client) {
        switch (walletState_.state()) {
            case WalletData.WALLET_STATE_ERROR:
                replyAuthError(pm, Errors.WALLET_ERROR, client);
                return;
            case WalletData.WALLET_STATE_INIT:
                replyAuthError(pm, Errors.NO_WALLET, client);
                return;
            case WalletData.WALLET_STATE_AUTH:
                replyAuthError(pm, Errors.WALLET_LOCKED, client);
                return;
            case WalletData.WALLET_STATE_OK:
                break;
            default:
                throw new RuntimeException("Unknown dao state");
        }

        // authForeground is only supported for Root/User roles, thus appUid is not used
        WalletData.User user = authDao_.get(pm.userId());
        if (user == null) {
            replyAuthError(pm, Errors.UNKNOWN_CALLER, client);
            return;
        }

        if (!isAuthenticAuthMessage(user, pm)) {
            replyAuthError(pm, Errors.FORBIDDEN, client);
            return;
        }

        WalletData.AuthRequest ar = authRequestDao_.get(pm.authId());
        if (ar == null) {
            Log.e(TAG, "unknown auth request "+pm.authId());
            Integer state = authRequestHistory_.get(pm.authId());
            if (state == null)
                state = 0;

            switch(state) {
                case Transaction.TX_STATE_TIMEDOUT:
                    replyAuthError(pm, Errors.TX_TIMEOUT, client);
                    break;
                    // FIXME maybe also report 'already confirmed' etc
                default:
                    replyAuthError(pm, Errors.AUTH_INPUT, client);
                    break;
            }
            return;
        }

        // messages from AuthClient
        switch (pm.type()) {
            case AuthData.MESSAGE_TYPE_AUTHED:
                onAuthedMessage(pm, ar, client);
                break;

            case AuthData.MESSAGE_TYPE_PRIV:
                onPrivMessage(pm, ar, user, client);
                break;

            default:
                throw new RuntimeException("Bad authForeground request");
        }
    }

    private void onAuthSubscribeMessage(AuthData.AuthMessage pm, Messenger client) {
        authRequestSubscribers_.add(new AuthSub(client, pm.id()));

        // don't touch daos until we're ok
        if (walletState_.state() != WalletData.WALLET_STATE_OK)
            return;

        // notify new client about all active auth requests
        for(WalletData.AuthRequest ar: authRequestDao_.getBackgroundRequests()) {
            boolean ok = replyAuthRequest(client, pm.id(), ar);
            if (!ok) {
                authRequestSubscribers_.remove(authRequestSubscribers_.size() - 1);
                break;
            }
        }
    }

    private void onWalletStateSubscribeMessage(AuthData.AuthMessage pm, Messenger client) {
        walletStateSubscribers_.add(new AuthSub(client, pm.id()));
        boolean ok = replyWalletState(client, pm.id());
        if (!ok) {
            walletStateSubscribers_.remove(walletStateSubscribers_.size() - 1);
        }
    }

    private void replyWalletError(String code, String err, AuthData.AuthMessage src, Messenger client) {
        AuthData.AuthMessage m = AuthData.AuthMessage.builder()
                .setId(src.id())
                .setType(src.type())
                .setCode(code)
                .setError(err)
                .build();
        sendAuthMessage(m, client);
    }

    private void replyWalletData(Object data, AuthData.AuthMessage src, Messenger client) {
        AuthData.AuthMessage m = AuthData.AuthMessage.builder()
                .setId(src.id())
                .setType(src.type())
                .setData(data)
                .build();
        sendAuthMessage(m, client);
    }

    private void onGenSeedMessage(final AuthData.AuthMessage pm, final Messenger client) {
        WalletData.GenSeedRequest r = (WalletData.GenSeedRequest)pm.data();
        if (r == null)
            throw new RuntimeException("Gen seed request not specified");

        daoProvider_.getLightningDao().genSeed(r, new IResponseCallback<WalletData.GenSeedResponse>() {
            @Override
            public void onResponse(WalletData.GenSeedResponse r) {
                replyWalletData(r, pm, client);
            }

            @Override
            public void onError(String code, String err) {
                replyWalletError(code, err, pm, client);
            }
        });
    }

    private void onInitWalletMessage(final AuthData.AuthMessage pm, final Messenger client) {
        WalletData.InitWalletRequest req = (WalletData.InitWalletRequest)pm.data();
        if (req == null)
            throw new RuntimeException("Init wallet request not specified");

        daoProvider_.initWallet(req, new IResponseCallback<WalletData.InitWalletResponse>() {
            @Override
            public void onResponse(WalletData.InitWalletResponse r) {
                // FIXME it's not the right place for it, is it?
                daoProvider_.init();

                replyWalletData(r, pm, client);
            }

            @Override
            public void onError(String code, String err) {
                replyWalletError(code, err, pm, client);
            }
        });
    }

    private void onUnlockWalletMessage(final AuthData.AuthMessage pm, final Messenger client) {
        WalletData.UnlockWalletRequest r = (WalletData.UnlockWalletRequest)pm.data();
        if (r == null)
            throw new RuntimeException("Unlock wallet request not specified");

        daoProvider_.unlockWallet(r, new IResponseCallback<WalletData.UnlockWalletResponse>() {
            @Override
            public void onResponse(WalletData.UnlockWalletResponse r) {

                // FIXME it's not the right place for it, is it?
                daoProvider_.init();

                replyWalletData(r, pm, client);
            }

            @Override
            public void onError(String code, String err) {
                replyWalletError(code, err, pm, client);
            }
        });
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case PluginData.MESSAGE_WHAT_LOCAL_TX:
            case PluginData.MESSAGE_WHAT_IPC_TX:
                onPluginMessage(msg);
                break;

            case PluginData.MESSAGE_WHAT_AUTH:
                onAuthMessage(msg);
                break;

            default:
                throw new RuntimeException("Bad plugin message 'what'");
        }
    }

    // helper, called after Auth activity is finished
    private WalletData.Error authForeground(IPluginForeground p, WalletData.AuthRequest ar, WalletData.AuthResponse r) {

        // read context of authed tx
        Caller c = callers_.get(ar.userId());
        if (c == null)
            throw new RuntimeException("Auth caller not found");

        Contexts ctxs = c.pluginContexts.get(p.id());
        Context ctx = ctxs.contexts.get(ar.txId());

        if (ctx.ctx.deadline < System.currentTimeMillis()) {
            p.timeout(ctx.ctx);
            return WalletData.Error.builder()
                    .setCode(Errors.TX_TIMEOUT)
                    .setMessage(Errors.errorMessage(Errors.TX_TIMEOUT))
                    .build();
        }

        // notify plugin
        WalletData.Error e = p.auth(ctx.ctx, r);
        if (e != null)
            return e;

        // notify that tx is authed
        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_AUTHED)
                .setPluginId(p.id())
                .setTxId(ctx.ctx.txId)
                .setAuthId(r.authId())
                .build();
        pm.assignData(r, WalletData.AuthResponse.class);
        sendTxReply(ctx.ctx, pm);

        // drop auth request, it's done now
        authRequestDao_.delete(ar.id());

        // remember
        authRequestHistory_.put(ar.id(), r.authorized()
                ? Transaction.TX_STATE_COMMITTED
                : Transaction.TX_STATE_REJECTED);

        // reset auth request of this context
        ctx.ctx.authRequest = null;

        // inform auth client that auth response was accepted
        return null;
    }

    private void authBackground(IPluginBackground p, WalletData.AuthRequest ar, WalletData.AuthResponse r) {
        p.auth(ar, r);
    }

    private boolean sendTxMessage(PluginData.PluginMessage pm, boolean ipc, Messenger client,
                                  WalletData.User user) {
        try {
            if (ipc) {

                ISigner signer = null;
                if (user != null)
                    signer = keyStore_.getKeySigner(PluginUtils.userKeyAlias(user.id()));
                if (signer != null && !signer.getPublicKey().equals(user.pubkey())) {
                    signer = null;
                    Log.e(TAG, "keystore signer not available for "+user);
                }

                Bundle b = PluginUtils.encodePluginMessageIpc(
                        pm,
                        ipcCodecProvider_,
                        ipcPluginMessageCodec_,
                        signer);

                Message m = this.obtainMessage(PluginData.MESSAGE_WHAT_IPC_TX);
                m.setData(b);
                client.send(m);
            } else {
                client.send(this.obtainMessage(PluginData.MESSAGE_WHAT_LOCAL_TX, pm));
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "client disconnected "+pm.pluginId()+" e "+e.getMessage());
            return false;
        }
    }

    // helper to send response to clients
    private void sendTxReply(PluginContext pc, PluginData.PluginMessage pm) {
        Caller c = callers_.get(pc.user.id());
        Contexts ctxs = c.pluginContexts.get(pm.pluginId());
        Context ctx = ctxs.contexts.get(pc.txId);

            // restored timed-out sessions might have empty client
        if (ctx != null && ctx.client != null) {
            Messenger client = ctx.client.get();
            // client might be GCed if caller was closed by OS
            if (client != null) {
                sendTxMessage(pm, ctx.ctx.ipc, client, ctx.ctx.user);
            } else {
                Log.i(TAG, "client lost due to GC");
            }
        } else {
            Log.i(TAG, "client not attached to recovered tx");
        }
    }

    @Override
    public boolean onInit(String pluginId, long userId, PluginContext pc) {
        Log.i(TAG, "onInit plugin "+pluginId+" user "+userId+" tx "+pc.txId);
        Caller c = callers_.get(userId);
        if (c == null) {
            WalletData.User user = authDao_.get(userId);
            if (user == null)
                return false;

            c = new Caller();
            c.user = user;
            callers_.put(c.user.id(), c);
        }

        Contexts ctxs = c.getContexts(pluginId);
        if (ctxs.contexts.get(pc.txId) != null) {
            throw new RuntimeException("Context already exists");
        }

        pc.user = c.user;
        pc.authRequest = authRequestDao_.get(pc.user.id(), pc.txId);

        Context ctx = new Context();
        ctx.ctx = pc;
        ctxs.contexts.put(pc.txId, ctx);

        return true;
    }

    @Override
    public void onReply(String pluginId, PluginContext ctx, Object r, Type type) {
        Log.i(TAG, "onResponse plugin "+pluginId+" user "+ctx.user.id()+" tx "+ctx.txId);

        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_REPLY)
                .setPluginId(pluginId)
                .setTxId(ctx.txId)
                .build();
        pm.assignData(r, type);

        sendTxReply(ctx, pm);
    }

    @Override
    public void onAuth(String pluginId, PluginContext ctx) {
        Log.i(TAG, "onAuth plugin "+pluginId+" user "+ctx.user.id()+" tx "+ctx.txId);

        // onAuth might be called multiple times if
        // client tries to recover the tx
        if (ctx.authRequest == null) {
            WalletData.AuthRequest.Builder b = WalletData.AuthRequest.builder()
                    .setId(idGenerator_.generateId(WalletData.AuthRequest.class))
                    .setPluginId(pluginId)
                    .setTxId(ctx.txId)
                    .setUserId(ctx.user.id())
                    .setCreateTime(System.currentTimeMillis());

            // let wallet code provide auth component
            authComponentProvider_.assignAuthComponent(b);

            // store the request, w/ assigned id
            ctx.authRequest = authRequestDao_.insert(b.build());
        }

        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_AUTH)
                .setPluginId(pluginId)
                .setTxId(ctx.txId)
                .build();

        pm.assignData(ctx.authRequest, WalletData.AuthRequest.class);

        sendTxReply(ctx, pm);
    }

    @Override
    public long onAuthBackground(String pluginId, String type) {
        Log.i(TAG, "onAuthBackground plugin "+pluginId+" type "+type);
        WalletData.AuthRequest ar = WalletData.AuthRequest.builder()
                .setId(idGenerator_.generateId(WalletData.AuthRequest.class))
                .setPluginId(pluginId)
                .setBackground(true)
                .setType(type)
                .setCreateTime(System.currentTimeMillis())
                .build();

        // returned object has assigned 'id'
        ar = authRequestDao_.insert(ar);

        // notify later, to let plugin attach additional
        // data to this auth request id before clients discover it
        pendingAuthRequests_.add(ar);

        return ar.id();
    }

    private void releaseContext(String pluginId, PluginContext ctx) {
        Caller c = callers_.get(ctx.user.id());
        Contexts ctxs = c.pluginContexts.get(pluginId);
        ctxs.contexts.remove(ctx.txId);

        if (ctx.authRequest != null) {
            authRequestDao_.delete(ctx.authRequest.id());

            // remember
            authRequestHistory_.put(ctx.authRequest.id(), Transaction.TX_STATE_TIMEDOUT);
        }
        ctx.authRequest = null;

        // mark as deleted, so that all plugins holding
        // references to this context in their async callbacks
        // could discard their replies
        ctx.deleted = true;
    }

    @Override
    public void onError(String pluginId, PluginContext ctx, String code, String message) {
        Log.i(TAG, "onError plugin "+pluginId+" user "+ctx.user.id()+" tx "+ctx.txId+" c "+code+" msg "+message);
        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_ERROR)
                .setPluginId(pluginId)
                .setTxId(ctx.txId)
                .setCode(code)
                .setError(message)
                .build();

        sendTxReply(ctx, pm);

        releaseContext(pluginId, ctx);
    }

    @Override
    public void onDone(String pluginId, PluginContext ctx) {
        Log.i(TAG, "onDone plugin "+pluginId+" user "+ctx.user.id()+" tx "+ctx.txId);
        PluginData.PluginMessage pm = PluginData.PluginMessage.builder()
                .setType(PluginData.MESSAGE_TYPE_DONE)
                .setPluginId(pluginId)
                .setTxId(ctx.txId)
                .build();

        sendTxReply(ctx, pm);

        releaseContext(pluginId, ctx);
    }

    @Override
    public void onSignal(String pluginId, String topic, Object data) {
        Log.i(TAG, "onSignal p "+pluginId+" topic "+topic+" request " + data);
        // FIXME this is probably slow, build some index
        Set<String> plugins = subscribers_.get(topic);
        if (plugins == null)
            return;

        for (String pid: plugins) {
            if (pid.equals(pluginId))
                continue;

            IPlugin p = getPlugin(pid);
            Log.i(TAG, "notify p "+pid+" topic "+topic);
            if (p.getBackground() != null)
                p.getBackground().notify(topic, data);

            for (Caller c : callers_.values()) {
                Contexts ctxs = c.pluginContexts.get(pid);
                Log.i(TAG, "notify contexts "+pid+" ctxs "+(ctxs == null ? 0 : ctxs.contexts.size()));
                if (ctxs == null)
                    continue;

                // since notifying a plugin might trigger
                // termination of the context and thus modification of
                // the ctx.contexts, we first copy the list of
                // contexts to be nofitied
                List<Context> buffer = new ArrayList<>(ctxs.contexts.values());
                for (Context ctx: buffer) {
                    p.getForeground().notify(ctx.ctx, topic, data);
                }
            }
        }
    }
}
