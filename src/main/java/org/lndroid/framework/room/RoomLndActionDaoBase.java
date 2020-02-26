package org.lndroid.framework.room;

import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.plugins.Transaction;

import java.util.List;

abstract class RoomLndActionDaoBase<Request, Response> {

    private String pluginId_;
    private RoomTransactions.TransactionDao txDao_;

    protected RoomTransactions.TransactionDao txDao() { return txDao_; };

    public void init (String pluginId, RoomTransactions.TransactionDao txDao) {
        pluginId_ = pluginId;
        txDao_ = txDao;
    }

    protected abstract long insertRequest(Request req);
    protected abstract void updateRequest(long id, Request req);
    protected abstract long insertResponse(
            Response r, ILndActionDao.OnResponseMerge<Response> merger);

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

    protected void confirmTransactionImpl(long txUserId, String txId, long txAuthUserId, long time, Request authedRequest) {
        if (authedRequest != null) {
            long id = txDao_.getTransactionRequestId(pluginId_, txUserId, txId);
            updateRequest(id, authedRequest);
        }

        txDao_.confirmTransaction(pluginId_, txUserId, txId, txAuthUserId, time);
    }

    public abstract void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time, Request authedRequest);


    public void rejectTransaction(long txUserId, String txId, long txAuthUserId, long time) {
        txDao_.rejectTransaction(pluginId_, txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, time);
    }

    public void failTransaction(long userId, String txId,
                                int state, long time, String errorCode, String errorMessage){
        txDao_.failTransaction(pluginId_, userId, txId, state, time, errorCode, errorMessage);
    }

    // we need this to be an atomic db tx, so each Room dao wraps this w/ @Transaction
    protected Response commitTransactionImpl(
            long userId, String txId, Response r, long time,
            ILndActionDao.OnResponseMerge<Response> merger) {
        final long id = insertResponse(r, merger);

        // update tx state
        txDao_.commitTransaction(pluginId_, userId, txId,
                org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED,
                time, r.getClass().getName(), id);

        return r;
    }

    public abstract Response commitTransaction(long txUserId, String txId, Response resp, long time,
                                               ILndActionDao.OnResponseMerge<Response> merger);
}
