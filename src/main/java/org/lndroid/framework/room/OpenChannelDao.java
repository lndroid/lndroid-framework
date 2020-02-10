package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IJobDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

public class OpenChannelDao implements IJobDao<WalletData.OpenChannelRequest, WalletData.Channel>, IPluginDao {
    private OpenChannelDaoRoom dao_;

    OpenChannelDao(OpenChannelDaoRoom dao) {
        dao_ = dao;
    }

    private Transaction<WalletData.OpenChannelRequest, WalletData.Channel> fromRoom(RoomTransactions.OpenChannelTransaction tx) {
        Transaction<WalletData.OpenChannelRequest, WalletData.Channel> t = new Transaction<>();
        RoomConverters.TxConverter.toTx(tx.txData, t);
        t.request = tx.request;
        t.response = tx.response;
        return t;
    }

    @Override
    public List<Transaction<WalletData.OpenChannelRequest, WalletData.Channel>> getTransactions() {
        List<Transaction<WalletData.OpenChannelRequest, WalletData.Channel>> r = new ArrayList<>();

        List<RoomTransactions.OpenChannelTransaction> txs = dao_.getTransactions();
        for (RoomTransactions.OpenChannelTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public Transaction<WalletData.OpenChannelRequest, WalletData.Channel> getTransaction(long txUserId, String txId) {
        RoomTransactions.OpenChannelTransaction tx = dao_.getTransaction(txUserId, txId);
        if (tx == null)
            return null;

        return fromRoom(tx);
    }

    @Override
    public void startTransaction(Transaction<WalletData.OpenChannelRequest, WalletData.Channel> t) {
        RoomTransactions.OpenChannelTransaction tx = new RoomTransactions.OpenChannelTransaction();
        tx.txData = RoomConverters.TxConverter.fromTx(t);
        tx.request = t.request;
        tx.response = t.response;
        dao_.createTransaction(tx);
    }

    @Override
    public WalletData.Channel commitTransaction(long txUserId, String txId, long txAuthUserId, WalletData.Channel r) {
        return dao_.commitTransaction(txUserId, txId, txAuthUserId, r, System.currentTimeMillis());
    }

    @Override
    public void rejectTransaction(long txUserId, String txId, long txAuthUserId) {
        dao_.rejectTransaction(txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(long txUserId, String txId) {
        dao_.timeoutTransaction(txUserId, txId, Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis());
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class OpenChannelDaoRoom {
    @Query("SELECT * FROM OpenChannelTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.OpenChannelTransaction> getTransactions();

    @Query("SELECT * FROM OpenChannelTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.OpenChannelTransaction getTransaction(long txUserId, String txId);

    @Insert
    public abstract void createTransaction(RoomTransactions.OpenChannelTransaction tx);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.OpenChannelTransaction tx);

    @Query("UPDATE OpenChannelTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);

    @Query("UPDATE OpenChannelTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(long txUserId, String txId, int txState, long time);

    @Insert
    public abstract void insertChannel(RoomData.Channel i);

    @Query("UPDATE Channel SET channelPoint = :cp WHERE id = :id")
    abstract void setFakeChannelPoint(long id, String cp);

    @androidx.room.Transaction
    public WalletData.Channel commitTransaction(long txUserId, String txId, long txAuthUserId, WalletData.Channel channel, long time) {
        // insert payment into it's own table
        RoomData.Channel p = new RoomData.Channel();
        p.setData(channel);

        insertChannel(p);
        setFakeChannelPoint(channel.id(), Long.toString(channel.id()));

        // get tx
        RoomTransactions.OpenChannelTransaction tx = getTransaction(txUserId, txId);

        // update state
        tx.setResponse(channel);
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;
        if (txAuthUserId != 0) {
            tx.txData.txAuthUserId = txAuthUserId;
            tx.txData.txAuthTime = time;
        }

        // write tx
        updateTransaction(tx);

        return channel;
    }
}