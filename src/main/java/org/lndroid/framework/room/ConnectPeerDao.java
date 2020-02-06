package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;

public class ConnectPeerDao extends LndActionDaoBase<WalletData.ConnectPeerRequest, WalletData.ConnectPeerResponse,
        RoomTransactions.ConnectPeerTransaction> {

    ConnectPeerDao(ConnectPeerDaoRoom dao) {
        super(dao, RoomTransactions.ConnectPeerTransaction.class);
    }
}

@Dao
abstract class ConnectPeerDaoRoom
        implements IRoomLndActionDao<RoomTransactions.ConnectPeerTransaction, WalletData.ConnectPeerRequest, WalletData.ConnectPeerResponse> {
    @Query("SELECT * FROM ConnectPeerTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.ConnectPeerTransaction> getTransactions();

    @Query("SELECT * FROM ConnectPeerTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.ConnectPeerTransaction getTransaction(int txUserId, String txId);

    @Insert
    public abstract void createTransaction(RoomTransactions.ConnectPeerTransaction tx);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.ConnectPeerTransaction tx);

    @Query("UPDATE ConnectPeerTransaction " +
            "SET txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void confirmTransaction(int txUserId, String txId, int txAuthUserId, long time);

    @Query("UPDATE ConnectPeerTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);

    @Override
    @Query("UPDATE ConnectPeerTransaction " +
            "SET txState = :txState, txDoneTime = :time, txError = :code " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(int txUserId, String txId, String code, int txState, long time);

    @Query("UPDATE ConnectPeerTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(int txUserId, String txId, int txState, long time);

    @Override @Transaction
    public void confirmTransaction(int txUserId, String txId, int txAuthUserId, long time, WalletData.ConnectPeerRequest authedRequest) {
        if (authedRequest != null) {
            RoomTransactions.ConnectPeerTransaction tx = getTransaction(txUserId, txId);
            tx.txData.txAuthTime = time;
            tx.txData.txAuthUserId = txAuthUserId;
            tx.request = authedRequest;
            updateTransaction(tx);
        } else {
            confirmTransaction(txUserId, txId, txAuthUserId, time);
        }
    }

    @Transaction
    public WalletData.ConnectPeerResponse commitTransaction(int txUserId, String txId, WalletData.ConnectPeerResponse r, long time) {
        RoomTransactions.ConnectPeerTransaction tx = getTransaction(txUserId, txId);

        // NOTE: r is not written to it's own table bcs we don't store addresses

        // update state
        tx.setResponse(r);
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;

        // update tx
        updateTransaction(tx);

        return r;
    }
}
