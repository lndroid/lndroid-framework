package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IWalletInfoDao;
import org.lndroid.framework.engine.IPluginDao;

class WalletInfoDao implements IWalletInfoDao, IPluginDao {

    private WalletInfoDaoRoom dao_;

    WalletInfoDao(WalletInfoDaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.WalletInfo get() {
        RoomData.WalletInfo r = dao_.get();
        return (r != null) ? r.data : null;
    }

    @Override
    public void update(WalletData.WalletInfo r) {
        RoomData.WalletInfo w = new RoomData.WalletInfo();
        w.data = r;
        dao_.update(w);
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
interface WalletInfoDaoRoom {
    @Query("SELECT * FROM WalletInfo WHERE id_ = 0")
    RoomData.WalletInfo get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(RoomData.WalletInfo b);
}
