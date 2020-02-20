package org.lndroid.framework.plugins;

import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.lnd.ILightningDao;

public abstract class LndActionBase<Request, LndRequest, Response, LndResponse> implements IPluginForeground {

    private IPluginServer server_;
    private ILndActionDao<Request, Response> dao_;
    private ILightningDao lnd_;
    private IPluginForegroundCallback engine_;

    private class TxData {
        PluginContext ctx;
        Request request;
        long authUserId;
    }
    private List<TxData> queue_ = new ArrayList<>();

    // override these
    protected abstract int defaultTimeout();
    protected abstract int maxTimeout();
    protected abstract LndRequest createLndRequest(PluginContext ctx, Request req);
    protected abstract Response createResponse(PluginContext ctx, Request req, long authUserId, LndResponse r);
    protected abstract void execute(LndRequest r, ILightningCallback<LndResponse> cb);
    protected abstract void signal(PluginContext ctx, Request req, Response rep);
    protected abstract boolean isUserPrivileged(WalletData.User user, Transaction<Request> tx);
    protected abstract Request getData(IPluginData in);
    protected abstract Type getResponseType();
    protected Object convertResponse(Response r) { return r; };

    protected IPluginServer server() { return server_; }
    protected ILightningDao lnd() { return lnd_; }
    protected IPluginForegroundCallback engine() { return engine_; }
    protected ILndActionDao<Request, Response> dao() { return dao_; }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        dao_ = (ILndActionDao<Request, Response>) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
        engine_ = callback;
        server_ = server;

        // restore active transactions
        List<Transaction<Request>> txs = dao_.getTransactions();
        for(Transaction<Request> tx: txs) {
            PluginContext ctx = new PluginContext();
            ctx.txId = tx.txId;
            ctx.deadline = tx.deadlineTime;
            // skip invalid txs
            if (!engine_.onInit(id(), tx.userId, ctx))
                continue;

            TxData data = new TxData();
            data.ctx = ctx;
            data.request = tx.request;
            data.authUserId = tx.authUserId;

            // store data in context
            ctx.request = data;

            if (tx.authUserId != 0) {
                // - tx not executed, authed
                scheduleRequest(data);
            } else if (ctx.authRequest != null) {
                // - tx not executed, auth request created
                // wait for auth
            } else if (isUserPrivileged(ctx.user, tx)) {
                // - tx not executed, auth request not yet created, auth not required,
                scheduleRequest(data);
            } else {
                // - tx not executed, auth required, auth request not created
                engine_.onAuth(id(), ctx);
            }
        }
    }

    @Override
    public void work() {
        if (!lnd_.isRpcReady())
            return;

        for(TxData r: queue_) {
            if (!r.ctx.deleted)
                startRequest(r);
        }
        queue_.clear();
    }

    private void startRequest(final TxData data) {

        LndRequest lndRequest = createLndRequest(data.ctx, data.request);

        execute (lndRequest, new ILightningCallback<LndResponse>() {
            @Override
            public void onResponse(LndResponse r) {

                // convert request to response
                Response rep = createResponse(data.ctx, data.request, data.authUserId, r);

                // store response in finished tx
                rep = dao_.commitTransaction(data.ctx.user.id(), data.ctx.txId, rep);

                // notify other plugins if needed
                signal(data.ctx, data.request, rep);

                // response to client
                engine_.onReply(id(), data.ctx, convertResponse(rep), getResponseType());
                engine_.onDone(id(), data.ctx);
            }

            @Override
            public void onError(int i, String s) {
                dao_.failTransaction(data.ctx.user.id(), data.ctx.txId, Errors.LND_ERROR, s);

                engine_.onError(id(), data.ctx, Errors.LND_ERROR, s);
            }
        });
    }

    private void scheduleRequest(TxData data) {
        if (lnd_.isRpcReady()) {
            startRequest(data);
        } else {
            queue_.add(data);
        }
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

        Request req = getData(in);
        if (req == null) {
            engine_.onError(id(), ctx, Errors.PLUGIN_MESSAGE, Errors.errorMessage(Errors.PLUGIN_MESSAGE));
            return;
        }

        // prepare in-memory data for this tx
        TxData data = new TxData();
        data.ctx = ctx;
        data.request = req;

        // create tx
        tx = new Transaction<>();
        tx.userId = ctx.user.id();
        tx.txId = ctx.txId;
        tx.request = data.request;
        tx.createTime = System.currentTimeMillis();
        int timeout = (int)ctx.timeout;
        if (timeout == 0) {
            timeout = defaultTimeout();
        } else if (timeout > maxTimeout()) {
            timeout = maxTimeout();
        }
        tx.deadlineTime = tx.createTime + timeout;

        // store tx to be able to restore it
        dao_.startTransaction(tx);

        // set deadline for engine
        ctx.deadline = tx.deadlineTime;

        // store tx data in context for reuse
        ctx.request = data;

        if (!isUserPrivileged(ctx.user, tx)) {
            // tell engine we need user auth
            engine_.onAuth(id(), ctx);
            return;
        }

        scheduleRequest(data);
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
    public WalletData.Error auth(PluginContext ctx, WalletData.AuthResponse r) {

        if (r.authorized()) {
            Request authedRequest = (Request)r.data();

            // update tx state
            dao_.confirmTransaction(ctx.user.id(), ctx.txId, r.authUserId(), authedRequest);

            // get request from ctx
            TxData data = (TxData)ctx.request;
            if (authedRequest != null)
                data.request = authedRequest;

            data.authUserId = r.authUserId();

            // call lnd
            scheduleRequest(data);
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

        // timeout only applies to authing,
        // if we're waiting for other reasons then we don't care
        if (ctx.authRequest != null) {
            // finish tx
            dao_.timeoutTransaction(ctx.user.id(), ctx.txId);
            // authorized response
            engine_.onError(id(), ctx, Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
        }
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
        // FIXME implement
    }
}
