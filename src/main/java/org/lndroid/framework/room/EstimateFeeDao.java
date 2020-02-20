package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;

import java.util.List;

public class EstimateFeeDao extends
        LndActionDaoBase<WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse, RoomTransactions.EstimateFeeTransaction> {

    EstimateFeeDao(EstimateFeeDaoRoom dao) {
        super(dao, RoomTransactions.EstimateFeeTransaction.class);
    }
}

@Dao
abstract class EstimateFeeDaoRoom
        implements IRoomLndActionDao<RoomTransactions.EstimateFeeTransaction, WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse> {

    @Query("SELECT * FROM EstimateFeeTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.EstimateFeeTransaction> getTransactions();

    @Query("SELECT * FROM EstimateFeeTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.EstimateFeeTransaction getTransaction(long txUserId, String txId);

    @Insert
    public abstract void createTransaction(RoomTransactions.EstimateFeeTransaction tx);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.EstimateFeeTransaction tx);

    @Query("UPDATE EstimateFeeTransaction " +
            "SET txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time);

    @Query("UPDATE EstimateFeeTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Override
    @Query("UPDATE EstimateFeeTransaction " +
            "SET txState = :txState, txDoneTime = :time, txErrorCode = :code, txErrorMessage = :message " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, String code, String message, int txState, long time);

    @Query("UPDATE EstimateFeeTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(long txUserId, String txId, int txState, long time);

    @Override @Transaction
    public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time, WalletData.EstimateFeeRequest authedRequest) {
        if (authedRequest != null) {
            RoomTransactions.EstimateFeeTransaction tx = getTransaction(txUserId, txId);
            tx.txData.txAuthTime = time;
            tx.txData.txAuthUserId = txAuthUserId;
            tx.request = authedRequest;
            updateTransaction(tx);
        } else {
            confirmTransaction(txUserId, txId, txAuthUserId, time);
        }
    }

    @Override
    public WalletData.EstimateFeeResponse getResponse(long id) { return null; };

    @Transaction
    public WalletData.EstimateFeeResponse commitTransaction(long txUserId, String txId, WalletData.EstimateFeeResponse r, long time) {
        RoomTransactions.EstimateFeeTransaction tx = getTransaction(txUserId, txId);

        // NOTE: r is not written to it's own table bcs we don't store them

        // update state
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;

        // update tx
        updateTransaction(tx);

        return r;
    }
}

