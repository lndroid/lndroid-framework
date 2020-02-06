package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

public class AddUserDao extends ActionDaoBase<WalletData.AddUserRequest, WalletData.User, RoomTransactions.AddUserTransaction> {

    private AddUserDaoRoom dao_;

    AddUserDao(AddUserDaoRoom dao) {
        super(dao, RoomTransactions.AddUserTransaction.class);
        dao_ = dao;
    }

    public int getNextUserId() {
        return (int)dao_.getMaxUserId() + 1;
    }
}

@Dao
abstract class AddUserDaoRoom implements IRoomActionDao<RoomTransactions.AddUserTransaction, WalletData.User> {
    @Override
    @Query("SELECT * FROM AddUserTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.AddUserTransaction> getTransactions();

    @Override
    @Query("SELECT * FROM AddUserTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.AddUserTransaction getTransaction(int txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddUserTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddUserTransaction tx);

    @Override
    @Query("UPDATE AddUserTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);

    @Query("SELECT MAX(id_) FROM User")
    abstract long getMaxUserId();

    // helper to insert users
    @Insert
    abstract long insertUser(RoomData.User r);

    // helper to persist the embedded user id
    // after auto-incremented user id was assigned during the insert
    @Query("UPDATE User SET id = id_ WHERE id_ = :id")
    abstract void setUserId(int id);

    // create user and add it to tx response and commit
    @Override @androidx.room.Transaction
    public WalletData.User commitTransaction(
            RoomTransactions.AddUserTransaction tx, int txAuthUserId, WalletData.User user, long time) {

        RoomData.User ru = new RoomData.User();
        ru.data = user;

        // insert new user
        final int id = (int) insertUser(ru);

        // persist the user id
        setUserId(id);

        // create new user object w/ proper id
        user = user.toBuilder().setId(id).build();

        // set response to tx
        tx.response = user;

        // update state
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txAuthUserId = txAuthUserId;
        tx.txData.txDoneTime = time;
        tx.txData.txAuthTime = time;

        // update tx
        updateTransaction(tx);

        // user with id
        return user;
    }
}