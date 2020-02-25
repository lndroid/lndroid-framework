package org.lndroid.framework.plugins;

import java.lang.reflect.Type;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public abstract class ActionBase<Request, Response> implements IPluginForeground {

    private IPluginServer server_;
    private IActionDao<Request, Response> dao_;
    private IPluginForegroundCallback engine_;

    protected abstract int defaultTimeout();
    protected abstract int maxTimeout();
    protected abstract boolean isUserPrivileged(Request req, WalletData.User user);
    protected abstract Response createResponse(PluginContext ctx, Request req, long authUserId);
    protected abstract void signal(Response response);
    protected abstract Type getResponseType();
    protected abstract boolean isValidUser(WalletData.User user);
    protected abstract Request getRequestData(IPluginData in);

    protected IPluginForegroundCallback engine() { return engine_; };
    protected IPluginServer server() { return server_; }

    public ActionBase() {
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback engine) {
        server_ = server;
        dao_ = (IActionDao<Request, Response>) server.getDaoProvider().getPluginDao(id());
        engine_ = engine;

        // restore active transactions
        List<Transaction<Request>> txs = dao_.getTransactions();
        for(Transaction<Request> tx: txs) {
            PluginContext ctx = new PluginContext();
            ctx.txId = tx.tx.txId;
            ctx.deadline = tx.tx.deadlineTime;
            ctx.request = tx.request;

            // invalid? skip
            if (!engine_.onInit(id(), tx.tx.userId, ctx))
                continue;

            // recover tx state
            if (ctx.authRequest != null) {
                // - tx not executed, auth request created
                // wait for auth
            } else if (isUserPrivileged(tx.request, ctx.user)) {
                // - tx not executed, auth not required,
                commit(tx.request, ctx, 0);
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

    private void commit(Request req, PluginContext ctx, long authUserId) {
        // commit
        Response response = createResponse(ctx, req, authUserId);

        // returned entity has the id assigned
        response = dao_.commitTransaction(ctx.user.id(), ctx.txId, authUserId, response);

        // first inform other plugins
        signal(response);
        engine_.onSignal(id(), DefaultTopics.NEW_CONTACT, null);
        engine_.onSignal(id(), DefaultTopics.CONTACT_STATE, null);

        // send response
        engine_.onReply(id(), ctx, response, getResponseType());
        engine_.onDone(id(), ctx, true);
    }

    @Override
    public void start(PluginContext ctx, IPluginData in) {

        if (!isValidUser(ctx.user)) {
            engine_.onError(id(), ctx, Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
            return;
        }

        // recover if tx already executed
        Transaction<Request> tx = dao_.getTransaction(ctx.user.id(), ctx.txId);
        if (tx != null){

            // if tx exists then client is retrying,
            // and we either have a finished tx (response ready or timeout),
            // in which case we response to client, or active tx
            // (waiting for auth), in which case client has
            // re-attached to this context and we should
            // inform it that auth is required
            if (tx.tx.doneTime != 0) {
                if (tx.tx.errorCode != null) {
                    engine_.onError(id(), ctx, tx.tx.errorCode, tx.tx.errorMessage);
                } else if (tx.tx.responseId != 0) {
                    Response r = dao_.getResponse(tx.tx.responseId);
                    if (r == null)
                        throw new RuntimeException("Response entity not found");
                    engine_.onReply(id(), ctx, r, getResponseType());
                    engine_.onDone(id(), ctx, true);
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

        // create tx
        tx = new Transaction<>();
        tx.tx = new Transaction.TransactionData(id(), ctx.user.id(), ctx.txId);
        tx.request = req;
        tx.tx.createTime = System.currentTimeMillis();
        int timeout = (int)ctx.timeout;
        if (timeout == 0) {
            timeout = defaultTimeout();
        } else if (timeout > maxTimeout()) {
            timeout = maxTimeout();
        }
        tx.tx.deadlineTime = tx.tx.createTime + timeout;

        // set ctx deadline for engine to enforce it
        ctx.deadline = tx.tx.deadlineTime;
        ctx.request = tx.request;

        // store tx to be able to restore it
        dao_.startTransaction(tx);

        // do we need auth?
        if (!isUserPrivileged(tx.request, ctx.user)) {
            // tell engine we need user auth
            engine_.onAuth(id(), ctx);
            return;
        }

        // commit
        commit(req, ctx, 0);
    }

    @Override
    public void receive(PluginContext ctx, IPluginData in) {
        throw new RuntimeException("Bad input");
    }

    @Override
    public void stop(PluginContext ctx) {
        throw new RuntimeException("Bad stop");
    }

    @Override
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse res) {
        if (res.authorized()) {
            Request req = (Request)ctx.request;

            if (res.data() != null) {
                req = (Request) res.data();
                ctx.request = req;
            }

            // create user and reply to client
            commit(req, ctx, res.authUserId());
        } else {
            // reject
            dao_.rejectTransaction(ctx.user.id(), ctx.txId, res.authUserId());

            // authorized response
            engine_.onError(id(), ctx, Errors.REJECTED, Errors.errorMessage(Errors.REJECTED));
        }

        return null;
    }

    @Override
    public void timeout(PluginContext ctx) {
        // reject
        dao_.timeoutTransaction(ctx.user.id(), ctx.txId);

        // notify client
        engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public boolean isUserPrivileged(PluginContext ctx, WalletData.User user) {
        return isUserPrivileged((Request)ctx.request, user);
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
