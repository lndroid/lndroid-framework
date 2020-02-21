package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.TransactionStateWorker;

import java.util.ArrayList;
import java.util.List;

public class TransactionStateWorkerDao implements TransactionStateWorker.IDao, IPluginDao {

    private DaoRoom dao_;

    TransactionStateWorkerDao(DaoRoom dao) { dao_ = dao; }

    @Override
    public WalletData.Transaction getTransaction(String txHash) {
        RoomData.Transaction t = dao_.getTransaction(txHash);
        return t != null ? t.getData() : null;
    }

    @Override
    public List<WalletData.Transaction> getSendingTransactions() {
        List<RoomData.Transaction> rs = dao_.getTransactions(
                WalletData.TRANSACTION_STATE_NEW, WalletData.TRANSACTION_STATE_LOST);
        List<WalletData.Transaction> txs = new ArrayList<>();
        for(RoomData.Transaction r: rs) {
            txs.add(r.getData());
        }
        return txs;
    }

    @Override
    public void updateTransaction(WalletData.Transaction t) {
        RoomData.Transaction r = new RoomData.Transaction();
        r.setData(t);
        dao_.updateTransaction(r);
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM 'Transaction' WHERE txHash = :txHash")
        RoomData.Transaction getTransaction(String txHash);

        @Query("SELECT * FROM 'Transaction' WHERE state = :state1 OR state = :state2")
        List<RoomData.Transaction> getTransactions(int state1, int state2);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void updateTransaction(RoomData.Transaction c);
    }
}
