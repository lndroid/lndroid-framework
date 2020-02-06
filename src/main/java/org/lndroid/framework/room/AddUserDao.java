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

class AddUserDao extends ActionDaoBase<WalletData.AddUserRequest, WalletData.User, RoomTransactions.AddUserTransaction> {

    AddUserDao(AddUserDaoRoom dao) {
        super(dao, RoomTransactions.AddUserTransaction.class);
    }
}


class AddUserDaoOld implements IActionDao<WalletData.AddUserRequest, WalletData.User>, IPluginDao {

    private AddUserDaoRoom dao_;

    AddUserDaoOld(AddUserDaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    private Transaction<WalletData.AddUserRequest, WalletData.User> fromRoom(RoomTransactions.AddUserTransaction tx) {
        Transaction<WalletData.AddUserRequest, WalletData.User> t = new Transaction<>();
        RoomConverters.TxConverter.toTx(tx.txData, t);
        t.request = tx.request;
        t.response = tx.response;
        return t;
    }

    @Override
    public List<Transaction<WalletData.AddUserRequest, WalletData.User>> getTransactions() {
        List<Transaction<WalletData.AddUserRequest, WalletData.User>> r = new ArrayList<>();

        List<RoomTransactions.AddUserTransaction> txs = dao_.getTransactions();
        for (RoomTransactions.AddUserTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public Transaction<WalletData.AddUserRequest, WalletData.User> getTransaction(int txUserId, String txId) {
        RoomTransactions.AddUserTransaction tx = dao_.getTransaction(txUserId, txId);
        if (tx == null)
            return null;

        return fromRoom(tx);
    }

    @Override
    public void startTransaction(Transaction<WalletData.AddUserRequest, WalletData.User> t) {
        RoomTransactions.AddUserTransaction tx = new RoomTransactions.AddUserTransaction();
        tx.request = t.request;
        tx.txData = RoomConverters.TxConverter.fromTx(t);
        dao_.createTransaction(tx);
    }

    @Override
    public WalletData.User commitTransaction(int txUserId, String txId, int txAuthUserId, WalletData.User r) {
        RoomTransactions.AddUserTransaction tx = dao_.getTransaction(txUserId, txId);
        return dao_.commitTransaction(tx, txAuthUserId, r, System.currentTimeMillis());
    }

    @Override
    public void rejectTransaction(int txUserId, String txId, int txAuthUserId) {
        dao_.failTransaction(txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(int txUserId, String txId) {
        dao_.failTransaction(txUserId, txId, 0, Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis());
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