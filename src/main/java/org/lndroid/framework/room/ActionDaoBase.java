package org.lndroid.framework.room;


import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.common.Errors;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

class ActionDaoBase<Request, Response>
        implements IActionDao<Request, Response>, IPluginDao {

    private RoomActionDaoBase<Request, Response> dao_;

    ActionDaoBase(RoomActionDaoBase<Request, Response> dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    private Transaction<Request> fromRoom(RoomTransactions.RoomTransaction tx) {
        Transaction<Request> t = new Transaction<>();
        t.job = tx.jobData;
        t.tx = tx.txData;
        t.request = dao_.getRequest(tx.txData.requestId);
        return t;
    }

    @Override
    public List<Transaction<Request>> getTransactions() {
        List<Transaction<Request>> r = new ArrayList<>();

        List<RoomTransactions.RoomTransaction> txs = dao_.getTransactions();
        for (RoomTransactions.RoomTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public Transaction<Request> getTransaction(long txUserId, String txId) {
        RoomTransactions.RoomTransaction tx = dao_.getTransaction(txUserId, txId);
        if (tx == null)
            return null;

        return fromRoom(tx);
    }

    @Override
    public Response getResponse(long id) {
        return dao_.getResponse(id);
    }

    @Override
    public void startTransaction(Transaction<Request> t) {
        try {
            RoomTransactions.RoomTransaction tx = new RoomTransactions.RoomTransaction();
            tx.txData = t.tx;
            tx.jobData = t.job;
            dao_.createTransaction(tx, t.request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response commitTransaction(long txUserId, String txId, long txAuthUserId, Response r) {
        return dao_.commitTransaction(txUserId, txId, txAuthUserId, r, System.currentTimeMillis());
    }

    @Override
    public void rejectTransaction(long txUserId, String txId, long txAuthUserId) {
        dao_.rejectTransaction(txUserId, txId, txAuthUserId, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(long txUserId, String txId) {
        dao_.failTransaction(txUserId, txId,
                Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis(),
                Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }
}
