package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import org.lndroid.framework.WalletData;

class AddListContactsPrivilegeDao
        extends ActionDaoBase<WalletData.ListContactsPrivilege,
                            WalletData.ListContactsPrivilege,
                            RoomTransactions.AddListContactsPrivilegeTransaction>
{
    AddListContactsPrivilegeDao (AddListContactsPrivilegeDaoRoom dao) {
        super(dao, RoomTransactions.AddListContactsPrivilegeTransaction.class);
    }
}

@Dao
abstract class AddListContactsPrivilegeDaoRoom
        implements IRoomActionDao<RoomTransactions.AddListContactsPrivilegeTransaction, WalletData.ListContactsPrivilege> {

    @Override
    @Query("SELECT * FROM AddListContactsPrivilegeTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.AddListContactsPrivilegeTransaction> getTransactions();

    @Override
    @Query("SELECT * FROM AddListContactsPrivilegeTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.AddListContactsPrivilegeTransaction getTransaction(int txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddListContactsPrivilegeTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddListContactsPrivilegeTransaction tx);

    @Override
    @Query("UPDATE AddListContactsPrivilegeTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);

    @Query("SELECT * FROM ListContactsPrivilege WHERE userId = :userId")
    abstract RoomData.ListContactsPrivilege getByUserId(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insert(RoomData.ListContactsPrivilege r);

    @Query("UPDATE ListContactsPrivilege SET id = id_ WHERE id_ = :id")
    abstract void updateId(int id);

    // create value and add it to tx response and commit
    @Override @androidx.room.Transaction
    public WalletData.ListContactsPrivilege commitTransaction(
            RoomTransactions.AddListContactsPrivilegeTransaction tx, int txAuthUserId, WalletData.ListContactsPrivilege v, long time) {

        RoomData.ListContactsPrivilege rv = getByUserId(v.userId());
        if (rv == null)
            rv = new RoomData.ListContactsPrivilege();
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