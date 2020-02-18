package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ISendCoinsWorkerDao;
import org.lndroid.framework.engine.IPluginDao;

import java.util.ArrayList;
import java.util.List;

public class SendCoinsWorkerDao implements ISendCoinsWorkerDao, IPluginDao {
    private SendCoinsWorkerDaoRoom dao_;

    SendCoinsWorkerDao(SendCoinsWorkerDaoRoom dao) {
        dao_ = dao;
    }

    private List<WalletData.Transaction> fromRoom(List<RoomData.Transaction> list) {
        List<WalletData.Transaction> r = new ArrayList<>();
        for (RoomData.Transaction c: list)
            r.add(c.getData());
        return r;
    }

    @Override
    public List<WalletData.Transaction> getPendingTransactions() {
        return fromRoom(dao_.getTransactions(WalletData.TRANSACTION_STATE_PENDING));
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
}

@Dao
interface SendCoinsWorkerDaoRoom {
    @Query("SELECT * FROM 'Transaction' WHERE state = :state")
    List<RoomData.Transaction> getTransactions(int state);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void updateTransaction(RoomData.Transaction c);
}
