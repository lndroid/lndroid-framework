package org.lndroid.framework.room;

import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.plugins.Transaction;

import java.util.List;

abstract class RoomActionDaoBase<Request, Response> {

    private String pluginId_;
    private RoomTransactions.TransactionDao txDao_;

    protected RoomTransactions.TransactionDao txDao() { return txDao_; };
    protected String pluginId() { return pluginId_; }

    public void init (String pluginId, RoomTransactions.TransactionDao txDao) {
        pluginId_ = pluginId;
        txDao_ = txDao;
    }

    protected abstract long insertRequest(Request req);
    protected abstract long insertResponse(Response rep);
    protected Response mergeExisting(Response r, IActionDao.OnResponseMerge<Response> merger) {
        // by default no merging is required
        if (merger != null)
            throw new RuntimeException("Merger not supported");
        return r;
    }

    public abstract Request getRequest(long id);
    public abstract Response getResponse(long id);

    // we need this to be an atomic db tx, so each Room dao wraps this w/ @Transaction
    protected void createTransactionImpl(RoomTransactions.RoomTransaction tx, Request req){

        // link tx to request
        tx.txData.requestClass = req.getClass().getName();
        tx.txData.requestId = insertRequest(req);

        // write transaction
        txDao_.createTransaction(tx);
    }
    public abstract void createTransaction(RoomTransactions.RoomTransaction tx, Request req);

    public List<RoomTransactions.RoomTransaction> getTransactions() {
        return txDao_.getTransactions(pluginId_);
    }

    public RoomTransactions.RoomTransaction getTransaction(long txUserId, String txId) {
        return txDao_.getTransaction(pluginId_, txUserId, txId);
    }

    public void rejectTransaction(long userId, String txId, long authUserId, long time) {
        txDao_.rejectTransaction(pluginId_, userId, txId, authUserId,
                Transaction.TX_STATE_REJECTED, time);
    }

    public void failTransaction(long userId, String txId,
                                int state, long time, String errorCode, String errorMessage){
        txDao_.failTransaction(pluginId_, userId, txId, state, time, errorCode, errorMessage);
    }

    // we need this to be an atomic db tx, so each Room dao wraps this w/ @Transaction
    protected Response commitTransactionImpl(
            long userId, String txId, long txAuthUserId, Response r, long time,
            IActionDao.OnResponseMerge<Response> merger) {

        r = mergeExisting(r, merger);

        final long id = insertResponse(r);

        // update tx state: confirm and commit
        txDao_.confirmTransaction(pluginId_, userId, txId, txAuthUserId, time);
        txDao_.commitTransaction(pluginId_, userId, txId,
                org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED,
                time, r.getClass().getName(), id);

        return r;
    }

    public abstract Response commitTransaction(
            long txUserId, String txId, long txAuthUserId, Response r, long time,
            IActionDao.OnResponseMerge<Response> merger);
}

