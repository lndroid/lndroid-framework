package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.SendCoinsWorker;

import java.util.ArrayList;
import java.util.List;

public class SendCoinsWorkerDao implements SendCoinsWorker.IDao, IPluginDao {
    private DaoRoom dao_;

    SendCoinsWorkerDao(DaoRoom dao) {
        dao_ = dao;
    }

    private List<WalletData.Transaction> fromRoom(List<RoomData.Transaction> list) {
        List<WalletData.Transaction> r = new ArrayList<>();
        for (RoomData.Transaction c: list)
            r.add(c.getData());
        return r;
    }

    @Override
    public List<WalletData.Transaction> getNewTransactions() {
        return fromRoom(dao_.getTransactions(WalletData.TRANSACTION_STATE_NEW));
    }

    @Override
    public List<WalletData.Transaction> getSendingTransactions() {
        return fromRoom(dao_.getTransactions(WalletData.TRANSACTION_STATE_SENDING));
    }

    @Override
    public List<WalletData.Transaction> getRetryTransactions() {
        return fromRoom(dao_.getTransactions(WalletData.TRANSACTION_STATE_RETRY));
    }

    @Override
    public void updateTransaction(WalletData.Transaction c) {
        RoomData.Transaction d = new RoomData.Transaction();
        d.setData(c);
        dao_.updateTransaction(d);
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM 'Transaction' WHERE state = :state")
        List<RoomData.Transaction> getTransactions(int state);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void updateTransaction(RoomData.Transaction c);
    }

}
