package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import org.lndroid.framework.WalletData;

public class AddContactPaymentsPrivilegeDao extends ActionDaoBase<WalletData.ContactPaymentsPrivilege,
        WalletData.ContactPaymentsPrivilege,
        RoomTransactions.AddContactPaymentsPrivilegeTransaction>
{
    AddContactPaymentsPrivilegeDao(AddContactPaymentsPrivilegeDaoRoom dao) {
        super(dao, RoomTransactions.AddContactPaymentsPrivilegeTransaction.class);
    }
}

@Dao
abstract class AddContactPaymentsPrivilegeDaoRoom
        implements IRoomActionDao<RoomTransactions.AddContactPaymentsPrivilegeTransaction, WalletData.ContactPaymentsPrivilege> {

    @Override
    @Query("SELECT * FROM AddContactPaymentsPrivilegeTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.AddContactPaymentsPrivilegeTransaction> getTransactions();

    @Override
    @Query("SELECT * FROM AddContactPaymentsPrivilegeTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.AddContactPaymentsPrivilegeTransaction getTransaction(int txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddContactPaymentsPrivilegeTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddContactPaymentsPrivilegeTransaction tx);

    @Override
    @Query("UPDATE AddContactPaymentsPrivilegeTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);

    @Query("SELECT * FROM ContactPaymentsPrivilege WHERE userId = :userId AND contactId = :contactId")
    abstract RoomData.ContactPaymentsPrivilege getExisting(int userId, long contactId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insert(RoomData.ContactPaymentsPrivilege r);

    @Query("UPDATE ContactPaymentsPrivilege SET id = id_ WHERE id_ = :id")
    abstract void updateId(int id);

    // create value and add it to tx response and commit
    @Override @androidx.room.Transaction
    public WalletData.ContactPaymentsPrivilege commitTransaction(
            RoomTransactions.AddContactPaymentsPrivilegeTransaction tx, int txAuthUserId, WalletData.ContactPaymentsPrivilege v, long time) {

        RoomData.ContactPaymentsPrivilege rv = getExisting(v.userId(), v.contactId());
        if (rv == null)
            rv = new RoomData.ContactPaymentsPrivilege();
        rv.setData(v);

        // insert new user
        final int id = (int) insert(rv);

        // persist the value id
        updateId(id);

        // create new value object w/ proper id
        v = v.toBuilder().setId(id).build();

        // set response to tx
        tx.setRequest(v);

        // update state
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txAuthUserId = txAuthUserId;
        tx.txData.txDoneTime = time;
        tx.txData.txAuthTime = time;

        // update tx
        updateTransaction(tx);

        // value with id
        return v;
    }
}