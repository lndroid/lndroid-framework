package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;

class ShareContactDao extends ActionDaoBase<WalletData.ShareContactRequest, WalletData.ShareContactResponse, RoomTransactions.ShareContactTransaction> {

    ShareContactDao(ShareContactDaoRoom dao) {
        super(dao, RoomTransactions.ShareContactTransaction.class);
    }
}

@Dao
abstract class ShareContactDaoRoom
        implements IRoomActionDao<RoomTransactions.ShareContactTransaction, WalletData.ShareContactResponse>{

    @Override @Query("SELECT * FROM ShareContactTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.ShareContactTransaction> getTransactions();

    @Override @Query("SELECT * FROM ShareContactTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.ShareContactTransaction getTransaction(long txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.ShareContactTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.ShareContactTransaction tx);

    @Override
    @Query("UPDATE ShareContactTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Override @Transaction
    public WalletData.ShareContactResponse commitTransaction(
            RoomTransactions.ShareContactTransaction tx, long txAuthUserId, WalletData.ShareContactResponse rep, long time) {

        // we don't store this response, only attach it to the tx

        // update state
        tx.response = rep;
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;
        tx.txData.txAuthUserId = txAuthUserId;
        tx.txData.txAuthTime = time;

        // write tx
        updateTransaction(tx);

        return rep;
    }
}
