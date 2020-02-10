package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IWalletBalanceDao;
import org.lndroid.framework.engine.IPluginDao;

class WalletBalanceDao implements IWalletBalanceDao, IPluginDao {

    private WalletBalanceDaoRoom dao_;

    WalletBalanceDao(WalletBalanceDaoRoom dao) {
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
}

@Dao
interface WalletBalanceDaoRoom {
    @Query("SELECT * FROM WalletBalance LIMIT 1")
    RoomData.WalletBalance get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(RoomData.WalletBalance b);
}
