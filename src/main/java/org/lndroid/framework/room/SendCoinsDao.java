package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IJobDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

import java.util.ArrayList;
import java.util.List;

public class SendCoinsDao implements IJobDao<WalletData.SendCoinsRequest, WalletData.Transaction>, IPluginDao {
    private SendCoinsDaoRoom dao_;

    SendCoinsDao(SendCoinsDaoRoom dao) {
        dao_ = dao;
    }

    private Transaction<WalletData.SendCoinsRequest, WalletData.Transaction> fromRoom(
            RoomTransactions.SendCoinsTransaction tx) {
        Transaction<WalletData.SendCoinsRequest, WalletData.Transaction> t = new Transaction<>();
        RoomConverters.TxConverter.toTx(tx.txData, t);
        t.request = tx.getRequest();
        t.response = tx.getResponse();
        return t;
    }

    @Override
    public List<Transaction<WalletData.SendCoinsRequest, WalletData.Transaction>> getTransactions() {
        List<Transaction<WalletData.SendCoinsRequest, WalletData.Transaction>> r = new ArrayList<>();

        List<RoomTransactions.SendCoinsTransaction> txs = dao_.getTransactions();
        for (RoomTransactions.SendCoinsTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public Transaction<WalletData.SendCoinsRequest, WalletData.Transaction> getTransaction(long txUserId, String txId) {
        RoomTransactions.SendCoinsTransaction tx = dao_.getTransaction(txUserId, txId);
        if (tx == null)
            return null;

        return fromRoom(tx);
    }

    @Override
    public void startTransaction(Transaction<WalletData.SendCoinsRequest, WalletData.Transaction> t) {
        RoomTransactions.SendCoinsTransaction tx = new RoomTransactions.SendCoinsTransaction();
        tx.setTxData(RoomConverters.TxConverter.fromTx(t));
        tx.setRequest(t.request);
        tx.setResponse(t.response);
        dao_.createTransaction(tx);
    }

    @Override
    public WalletData.Transaction commitTransaction(
            long txUserId, String txId, long txAuthUserId, WalletData.Transaction r) {
        return dao_.commitTransaction(txUserId, txId, txAuthUserId, r, System.currentTimeMillis());
    }

    @Override
    public void rejectTransaction(long txUserId, String txId, long txAuthUserId) {
        dao_.rejectTransaction(txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(long txUserId, String txId) {
        dao_.timeoutTransaction(txUserId, txId, Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis());
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class SendCoinsDaoRoom {
    @Query("SELECT * FROM SendCoinsTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.SendCoinsTransaction> getTransactions();

    @Query("SELECT * FROM SendCoinsTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.SendCoinsTransaction getTransaction(long txUserId, String txId);

    @Insert
    public abstract void createTransaction(RoomTransactions.SendCoinsTransaction tx);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.SendCoinsTransaction tx);

    @Query("UPDATE SendCoinsTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Query("UPDATE SendCoinsTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(long txUserId, String txId, int txState, long time);

    @Insert
    public abstract void insertTransaction(RoomData.Transaction i);

    @androidx.room.Transaction
    public WalletData.Transaction commitTransaction(
            long txUserId, String txId, long txAuthUserId, WalletData.Transaction t, long time) {


        // insert response
        RoomData.Transaction p = new RoomData.Transaction();
        p.setData(t);

        insertTransaction(p);

        // get tx
        RoomTransactions.SendCoinsTransaction tx = getTransaction(txUserId, txId);

        // update state
        tx.setResponse(t);
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;
        if (txAuthUserId != 0) {
            tx.txData.txAuthUserId = txAuthUserId;
            tx.txData.txAuthTime = time;
        }

        // write tx
        updateTransaction(tx);

        return t;
    }
}