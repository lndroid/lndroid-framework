package org.lndroid.framework.room;


import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

class ActionDaoBase<Request, Response, RoomTransaction extends IRoomTransaction<Request>>
        implements IActionDao<Request, Response>, IPluginDao {

    private IRoomActionDao<RoomTransaction, Response> dao_;
    private Class<RoomTransaction> roomTxClass_;

    ActionDaoBase(IRoomActionDao<RoomTransaction, Response> dao, Class<RoomTransaction> roomTxClass) {
        dao_ = dao;
        roomTxClass_ = roomTxClass;
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
    public Response getResponse(long id) {
        return dao_.getResponse(id);
    }

    @Override
    public void startTransaction(Transaction<Request> t) {
        try {
            RoomTransaction tx = roomTxClass_.newInstance();
            tx.setRequest(t.request);
            tx.setTxData(RoomConverters.TxConverter.fromTx(t));
            dao_.createTransaction(tx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response commitTransaction(long txUserId, String txId, long txAuthUserId, Response r) {
        RoomTransaction tx = dao_.getTransaction(txUserId, txId);
        return dao_.commitTransaction(tx, txAuthUserId, r, System.currentTimeMillis());
    }

    @Override
    public void rejectTransaction(long txUserId, String txId, long txAuthUserId) {
        dao_.failTransaction(txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(long txUserId, String txId) {
        dao_.failTransaction(txUserId, txId, 0, Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis());
    }
}
