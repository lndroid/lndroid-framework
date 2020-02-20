package org.lndroid.framework.room;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

class LndActionDaoBase<Request, Response, RoomTransaction extends IRoomTransaction<Request>>
        implements ILndActionDao<Request, Response>, IPluginDao {

    private IRoomLndActionDao<RoomTransaction, Request, Response> dao_;
    private Class<RoomTransaction> roomTxClass_;

    LndActionDaoBase(IRoomLndActionDao<RoomTransaction, Request, Response> dao, Class<RoomTransaction> c) {
        dao_ = dao;
        roomTxClass_ = c;
    }

    @Override
    public void init() {
        // noop
    }

    private Transaction<Request> fromRoom(RoomTransaction tx) {
        Transaction<Request> t = new Transaction<>();
        RoomConverters.TxConverter.toTx(tx.getTxData(), t);
        t.request = tx.getRequest();
        return t;
    }

    @Override
    public List<Transaction<Request>> getTransactions() {
        List<Transaction<Request>> r = new ArrayList<>();

        List<RoomTransaction> txs = dao_.getTransactions();
        for (RoomTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public Transaction<Request> getTransaction(long txUserId, String txId) {
        RoomTransaction tx = dao_.getTransaction(txUserId, txId);
        if (tx == null)
            return null;

        return fromRoom(tx);
    }

    @Override
    public void startTransaction(Transaction<Request> t) {
        try {
            RoomTransaction tx = roomTxClass_.newInstance();
            tx.setTxData(RoomConverters.TxConverter.fromTx(t));
            tx.setRequest(t.request);
            dao_.createTransaction(tx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response commitTransaction(long txUserId, String txId, Response r) {
        return dao_.commitTransaction(txUserId, txId, r, System.currentTimeMillis());
    }

    @Override
    public void confirmTransaction(long txUserId, String txId, long txAuthUserId, Request authedRequest) {
        dao_.confirmTransaction(txUserId, txId, txAuthUserId, System.currentTimeMillis(), authedRequest);
    }

    @Override
    public void rejectTransaction(long txUserId, String txId, long txAuthUserId) {
        dao_.rejectTransaction(txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, System.currentTimeMillis());
    }

    @Override
    public void failTransaction(long txUserId, String txId, String code, String message) {
        dao_.failTransaction(txUserId, txId, code, message, Transaction.TX_STATE_ERROR, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(long txUserId, String txId) {
        dao_.timeoutTransaction(txUserId, txId, Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis());
    }

    @Override
    public Response getResponse(long id) {
        return dao_.getResponse(id);
    }
}
