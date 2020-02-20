package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import org.lndroid.framework.WalletData;

public class AddUserDao extends ActionDaoBase<WalletData.AddUserRequest, WalletData.User, RoomTransactions.AddUserTransaction> {

    AddUserDao(AddUserDaoRoom dao) {
        super(dao, RoomTransactions.AddUserTransaction.class);
    }
}

@Dao
abstract class AddUserDaoRoom implements IRoomActionDao<RoomTransactions.AddUserTransaction, WalletData.User> {
    @Override
    @Query("SELECT * FROM AddUserTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.AddUserTransaction> getTransactions();

    @Override
    @Query("SELECT * FROM AddUserTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.AddUserTransaction getTransaction(long txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddUserTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddUserTransaction tx);

    @Override
    @Query("UPDATE AddUserTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    // helper to insert users
    @Insert
    abstract void insertUser(RoomData.User r);

    @Query("SELECT * FROM User WHERE id = :id")
    abstract RoomData.User getResponseRoom(long id);

    @Override
    public WalletData.User getResponse(long id) {
        RoomData.User r = getResponseRoom(id);
        return r != null ? r.getData() : null;
    }

    // create user and add it to tx response and commit
    @Override @androidx.room.Transaction
    public WalletData.User commitTransaction(
            RoomTransactions.AddUserTransaction tx, long txAuthUserId, WalletData.User user, long time) {

        RoomData.User ru = new RoomData.User();
        ru.setData(user);

        // insert new user
        insertUser(ru);

        // set response to tx
        tx.setResponse(user.getClass(), user.id());

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