package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;

class AddInvoiceDao extends LndActionDaoBase<WalletData.AddInvoiceRequest, WalletData.Invoice, RoomTransactions.AddInvoiceTransaction> {

    AddInvoiceDao(AddInvoiceDaoRoom dao) {
        super(dao, RoomTransactions.AddInvoiceTransaction.class);
    }
}

@Dao
abstract class AddInvoiceDaoRoom
        implements IRoomLndActionDao<RoomTransactions.AddInvoiceTransaction, WalletData.AddInvoiceRequest, WalletData.Invoice>{

    @Override @Query("SELECT * FROM AddInvoiceTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.AddInvoiceTransaction> getTransactions();

    @Override @Query("SELECT * FROM AddInvoiceTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.AddInvoiceTransaction getTransaction(long txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddInvoiceTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddInvoiceTransaction tx);

    @Query("UPDATE AddInvoiceTransaction " +
            "SET txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time);

    @Override
    @Query("UPDATE AddInvoiceTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Override
    @Query("UPDATE AddInvoiceTransaction " +
            "SET txState = :txState, txDoneTime = :time, txError = :code " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, String code, int txState, long time);

    @Override
    @Query("UPDATE AddInvoiceTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(long txUserId, String txId, int txState, long time);

    @Insert
    public abstract void insertInvoice(RoomData.Invoice i);

    @Override @Transaction
    public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time, WalletData.AddInvoiceRequest authedRequest) {
        if (authedRequest != null) {
            RoomTransactions.AddInvoiceTransaction tx = getTransaction(txUserId, txId);
            tx.txData.txAuthTime = time;
            tx.txData.txAuthUserId = txAuthUserId;
            tx.request = authedRequest;
            updateTransaction(tx);
        } else {
            confirmTransaction(txUserId, txId, txAuthUserId, time);
        }

    }

    @Override @Transaction
    public WalletData.Invoice commitTransaction(long txUserId, String txId, WalletData.Invoice invoice, long time) {

        // insert invoice into it's own table
        RoomData.Invoice ri = new RoomData.Invoice();
        ri.setData(invoice);

        // insert
        insertInvoice(ri);

        // get tx
        RoomTransactions.AddInvoiceTransaction tx = getTransaction(txUserId, txId);

        // update state
        tx.setResponse(invoice);
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;

        // write tx
        updateTransaction(tx);

        return invoice;
    }
}