package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;

class NewAddressDao extends LndActionDaoBase<WalletData.NewAddressRequest, WalletData.NewAddress, RoomTransactions.NewAddressTransaction> {

    NewAddressDao(NewAddressDaoRoom dao) {
        super(dao, RoomTransactions.NewAddressTransaction.class);
    }
}

@Dao
abstract class NewAddressDaoRoom
        implements IRoomLndActionDao<RoomTransactions.NewAddressTransaction, WalletData.NewAddressRequest, WalletData.NewAddress> {
    @Query("SELECT * FROM NewAddressTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.NewAddressTransaction> getTransactions();

    @Query("SELECT * FROM NewAddressTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.NewAddressTransaction getTransaction(long txUserId, String txId);

    @Insert
    public abstract void createTransaction(RoomTransactions.NewAddressTransaction tx);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.NewAddressTransaction tx);

    @Query("UPDATE NewAddressTransaction " +
            "SET txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time);

    @Query("UPDATE NewAddressTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Override
    @Query("UPDATE NewAddressTransaction " +
            "SET txState = :txState, txDoneTime = :time, txError = :code " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, String code, int txState, long time);

    @Query("UPDATE NewAddressTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(long txUserId, String txId, int txState, long time);

    @Override @Transaction
    public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time, WalletData.NewAddressRequest authedRequest) {
        if (authedRequest != null) {
            RoomTransactions.NewAddressTransaction tx = getTransaction(txUserId, txId);
            tx.txData.txAuthTime = time;
            tx.txData.txAuthUserId = txAuthUserId;
            tx.request = authedRequest;
            updateTransaction(tx);
        } else {
            confirmTransaction(txUserId, txId, txAuthUserId, time);
        }
    }

    @Transaction
    public WalletData.NewAddress commitTransaction(long txUserId, String txId, WalletData.NewAddress r, long time) {
        RoomTransactions.NewAddressTransaction tx = getTransaction(txUserId, txId);

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
