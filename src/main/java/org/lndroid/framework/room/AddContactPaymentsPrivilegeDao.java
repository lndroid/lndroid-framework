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
    public abstract RoomTransactions.AddContactPaymentsPrivilegeTransaction getTransaction(long txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddContactPaymentsPrivilegeTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddContactPaymentsPrivilegeTransaction tx);

    @Override
    @Query("UPDATE AddContactPaymentsPrivilegeTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Query("SELECT * FROM ContactPaymentsPrivilege WHERE userId = :userId AND contactId = :contactId")
    abstract RoomData.ContactPaymentsPrivilege getExisting(long userId, long contactId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void upsert(RoomData.ContactPaymentsPrivilege r);

    @Query("SELECT * FROM ContactPaymentsPrivilege WHERE id = :id")
    abstract RoomData.ContactPaymentsPrivilege getResponseRoom(long id);

    @Override
    public WalletData.ContactPaymentsPrivilege getResponse(long id) {
        RoomData.ContactPaymentsPrivilege r = getResponseRoom(id);
        return r != null ? r.getData() : null;
    }

    // create value and add it to tx response and commit
    @Override @androidx.room.Transaction
    public WalletData.ContactPaymentsPrivilege commitTransaction(
            RoomTransactions.AddContactPaymentsPrivilegeTransaction tx, long txAuthUserId,
            WalletData.ContactPaymentsPrivilege v, long time)
    {
        RoomData.ContactPaymentsPrivilege rv = getExisting(v.userId(), v.contactId());
        if (rv == null) {
            rv = new RoomData.ContactPaymentsPrivilege();
            v = v.toBuilder().setId(rv.getData().id()).build();
        }

        rv.setData(v);

        // write
        upsert(rv);

        // set response to tx
        tx.setResponse(v.getClass(), v.id());

        // update state
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txAuthUserId = txAuthUserId;
        tx.txData.txDoneTime = time;
        tx.txData.txAuthTime = time;

        // update tx
        updateTransaction(tx);

        return v;
    }
}