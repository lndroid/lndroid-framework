package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;

public class DecodePayReqDao extends LndActionDaoBase<String, WalletData.SendPayment, RoomTransactions.DecodePayReqTransaction> {

    DecodePayReqDao(DecodePayReqDaoRoom dao) {
        super(dao, RoomTransactions.DecodePayReqTransaction.class);
    }
}

@Dao
abstract class DecodePayReqDaoRoom
        implements IRoomLndActionDao<RoomTransactions.DecodePayReqTransaction, String, WalletData.SendPayment> {
    @Query("SELECT * FROM DecodePayReqTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.DecodePayReqTransaction> getTransactions();

    @Query("SELECT * FROM DecodePayReqTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.DecodePayReqTransaction getTransaction(long txUserId, String txId);

    @Insert
    public abstract void createTransaction(RoomTransactions.DecodePayReqTransaction tx);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.DecodePayReqTransaction tx);

    @Query("UPDATE DecodePayReqTransaction " +
            "SET txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time);

    @Query("UPDATE DecodePayReqTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Override
    @Query("UPDATE DecodePayReqTransaction " +
            "SET txState = :txState, txDoneTime = :time, txError = :code " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, String code, int txState, long time);

    @Query("UPDATE DecodePayReqTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(long txUserId, String txId, int txState, long time);

    @Override @Transaction
    public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time, String authedRequest) {
        if (authedRequest != null) {
            RoomTransactions.DecodePayReqTransaction tx = getTransaction(txUserId, txId);
            tx.txData.txAuthTime = time;
            tx.txData.txAuthUserId = txAuthUserId;
            tx.request = authedRequest;
            updateTransaction(tx);
        } else {
            confirmTransaction(txUserId, txId, txAuthUserId, time);
        }
    }

    @Transaction
    public WalletData.SendPayment commitTransaction(long txUserId, String txId, WalletData.SendPayment r, long time) {
        RoomTransactions.DecodePayReqTransaction tx = getTransaction(txUserId, txId);

        // NOTE: r is not written to it's own table bcs we don't store them

        // update state
        tx.setResponse(r);
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;

        // update tx
        updateTransaction(tx);

        return r;
    }
}
