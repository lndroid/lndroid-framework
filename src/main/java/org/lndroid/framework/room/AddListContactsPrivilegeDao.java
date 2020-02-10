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
    public abstract RoomTransactions.AddListContactsPrivilegeTransaction getTransaction(long txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddListContactsPrivilegeTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddListContactsPrivilegeTransaction tx);

    @Override
    @Query("UPDATE AddListContactsPrivilegeTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Query("SELECT * FROM ListContactsPrivilege WHERE userId = :userId")
    abstract RoomData.ListContactsPrivilege getByUserId(long userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void upsert(RoomData.ListContactsPrivilege r);

    // create value and add it to tx response and commit
    @Override @androidx.room.Transaction
    public WalletData.ListContactsPrivilege commitTransaction(
            RoomTransactions.AddListContactsPrivilegeTransaction tx, long txAuthUserId, WalletData.ListContactsPrivilege v, long time) {

        RoomData.ListContactsPrivilege rv = getByUserId(v.userId());
        if (rv == null) {
            rv = new RoomData.ListContactsPrivilege();
            v = v.toBuilder().setId(rv.getData().id()).build();
        }
        rv.setData(v);

        // write
        upsert(rv);

        // set response to tx
        tx.setResponse(v);

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