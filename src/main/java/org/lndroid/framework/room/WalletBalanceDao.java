package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IWalletBalanceDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetWalletBalance;
import org.lndroid.framework.plugins.WalletBalanceWorker;

class WalletBalanceDao implements
        IWalletBalanceDao, IPluginDao,
        GetWalletBalance.IDao,
        WalletBalanceWorker.IDao
{

    private DaoRoom dao_;

    WalletBalanceDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public WalletData.WalletBalance get() {
        RoomData.WalletBalance r = dao_.get();
        return (r != null) ? r.data : null;
    }

    @Override
    public void update(WalletData.WalletBalance b) {
        RoomData.WalletBalance wb = new RoomData.WalletBalance();
        wb.data = b;
        dao_.update(wb);
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM WalletBalance LIMIT 1")
        RoomData.WalletBalance get();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void update(RoomData.WalletBalance b);
    }
}
