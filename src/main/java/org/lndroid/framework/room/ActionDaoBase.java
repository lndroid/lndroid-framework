package org.lndroid.framework.room;


import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

class ActionDaoBase<Request, Response, RoomTransaction extends IRoomTransaction<Request, Response>>
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

    private Transaction<Request, Response> fromRoom(RoomTransaction tx) {
        Transaction<Request, Response> t = new Transaction<>();
        RoomConverters.TxConverter.toTx(tx.getTxData(), t);
        t.request = tx.getRequest();
        t.response = tx.getResponse();
        return t;
    }

    @Override
    public List<Transaction<Request, Response>> getTransactions() {
        List<Transaction<Request, Response>> r = new ArrayList<>();

        List<RoomTransaction> txs = dao_.getTransactions();
        for (RoomTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public Transaction<Request, Response> getTransaction(int txUserId, String txId) {
        RoomTransaction tx = dao_.getTransaction(txUserId, txId);
        if (tx == null)
            return null;

        return fromRoom(tx);
    }

    @Override
    public void startTransaction(Transaction<Request, Response> t) {
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
    public Response commitTransaction(int txUserId, String txId, int txAuthUserId, Response r) {
        RoomTransaction tx = dao_.getTransaction(txUserId, txId);
        return dao_.commitTransaction(tx, txAuthUserId, r, System.currentTimeMillis());
    }

    @Override
    public void rejectTransaction(int txUserId, String txId, int txAuthUserId) {
        dao_.failTransaction(txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(int txUserId, String txId) {
        dao_.failTransaction(txUserId, txId, 0, Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis());
    }
}
