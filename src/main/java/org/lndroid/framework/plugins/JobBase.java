package org.lndroid.framework.plugins;

import java.lang.reflect.Type;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IJobDao;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public abstract class JobBase<Request, Response> implements IPluginForeground {

    private String pluginId_;
    private String newTopic_;
    private String stateTopic_;
    private IPluginServer server_;
    private IJobDao<Request, Response> dao_;
    private IPluginForegroundCallback engine_;

    JobBase(String pluginId, String newTopic, String stateTopic) {
        pluginId_ = pluginId;
        newTopic_ = newTopic;
        stateTopic_ = stateTopic;
    }

    @Override
    public String id() {
        return pluginId_;
    }

    protected IPluginServer server() { return server_; }
    protected abstract boolean isUserPrivileged(WalletData.User user, Transaction<Request> tx);
    protected IJobDao<Request, Response> dao() { return dao_; };

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        server_ = server;
        dao_ = (IJobDao<Request, Response>) server.getDaoProvider().getPluginDao(id());
        engine_ = callback;

        // restore active transactions
        List<Transaction<Request>> txs = dao_.getTransactions();
        for(Transaction<Request> tx: txs) {
            PluginContext ctx = new PluginContext();
            ctx.txId = tx.txId;
            ctx.deadline = tx.deadlineTime;

            // invalid? skip
            if (!engine_.onInit(id(), tx.userId, ctx))
                continue;

            // cache request in ctx
            ctx.request = tx.request;

            if (isUserPrivileged(ctx.user, tx)) {
                // - tx not executed, auth not required,
                commit(ctx, 0);
            } else if (ctx.authRequest != null) {
                // - tx not executed, auth required, auth request created
                // wait for auth
            } else {
                // - tx not executed, auth required, auth request not created
                engine_.onAuth(id(), ctx);
            }
        }
    }

    @Override
    public void work() {
        // noop
    }

    protected abstract Response createResponse(PluginContext ctx, Request req, long authUserId);
    protected abstract boolean isValid(Request req);
    protected abstract int defaultTimeout();
    protected abstract int maxTimeout();
    protected abstract Request getRequestData(IPluginData in);
    protected abstract Type getResponseType();

    private void commit(PluginContext ctx, long authUserId) {
        // convert request to response
        Response rep = createResponse(ctx, (Request) ctx.request, authUserId);

        // store response in finished tx
        rep = dao_.commitTransaction(ctx.user.id(), ctx.txId, authUserId, rep);

        // notify other plugins if needed
        engine_.onSignal(id(), newTopic_, rep);
        engine_.onSignal(id(), stateTopic_, rep);

        // response to client
        engine_.onReply(id(), ctx, rep, getResponseType());
        engine_.onDone(id(), ctx);
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {

        // recover if tx already executed
        Transaction<Request> tx = dao_.getTransaction(ctx.user.id(), ctx.txId);
        if (tx != null){
            if (tx.doneTime != 0) {
                if (tx.errorCode != null) {
                    engine_.onError(id(), ctx, tx.errorCode, "Transaction failed");
                } else if (tx.responseId != 0) {
                    Response r = dao_.getResponse(tx.responseId);
                    if (r == null)
                        throw new RuntimeException("Response entity not found");
                    engine_.onReply(id(), ctx, r, getResponseType());
                    engine_.onDone(id(), ctx);
                } else {
                    engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
                }
            } else {
                if (ctx.authRequest != null)
                    engine_.onAuth(id(), ctx);
            }
            return;
        }

        Request req = getRequestData(in);
        if (req == null) {
            engine_.onError(id(), ctx, Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
            return;
        }
        if (!isValid(req)) {
            engine_.onError(id(), ctx, Errors.PLUGIN_INPUT, Errors.errorMessage(Errors.PLUGIN_INPUT));
            return;
        }

        // create tx
        tx = new Transaction<>();
        tx.userId = ctx.user.id();
        tx.txId = ctx.txId;
        tx.request = req;
        tx.createTime = System.currentTimeMillis();
        int timeout = (int)ctx.timeout;
        if (timeout == 0) {
            timeout = defaultTimeout();
        } else if (timeout > maxTimeout()) {
            timeout = maxTimeout();
        }
        tx.deadlineTime = tx.createTime + timeout;

        // set ctx deadline for engine to enforce it
        ctx.deadline = tx.deadlineTime;

        // cache request in ctx
        ctx.request = tx.request;

        // store tx to be able to restore it
        dao_.startTransaction(tx);

        if (!isUserPrivileged(ctx.user, tx)) {
            // tell engine we need user auth
            engine_.onAuth(id(), ctx);
            return;
        }

        // no need for auth, commit right now
        commit(ctx, 0);
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        engine_.onError(id(), ctx, Errors.PLUGIN_PROTOCOL, Errors.errorMessage(Errors.PLUGIN_PROTOCOL));
    }

    @Override
    public void stop(PluginContext ctx) {
        engine_.onError(id(), ctx, Errors.PLUGIN_PROTOCOL, Errors.errorMessage(Errors.PLUGIN_PROTOCOL));
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse r) {

        if (r.authorized()) {
            // commit after authed
            commit(ctx, r.authUserId());
        } else {
            // finish tx
            dao_.rejectTransaction(ctx.user.id(), ctx.txId, r.authUserId());
            // authorized response
            engine_.onError(id(), ctx, Errors.REJECTED, Errors.errorMessage(Errors.REJECTED));
        }

        return null;
    }

    @Override
    public void timeout(PluginContext ctx) {
        // finish tx
        dao_.timeoutTransaction(ctx.user.id(), ctx.txId);
        // authorized response
        engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        return isUserPrivileged(user, dao_.getTransaction(ctx.user.id(), ctx.txId));
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        // noop
    }

    @Override
    public void notify(PluginContext ctx, String topic, Object data) {
        // noop
    }
}
