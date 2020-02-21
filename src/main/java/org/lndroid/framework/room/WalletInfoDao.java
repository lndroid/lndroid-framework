package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IWalletInfoDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetWalletInfo;
import org.lndroid.framework.plugins.WalletInfoWorker;

class WalletInfoDao implements
        IWalletInfoDao, IPluginDao,
        GetWalletInfo.IDao,
        WalletInfoWorker.IDao
{

    private DaoRoom dao_;

    WalletInfoDao(DaoRoom dao) {
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

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM WalletInfo LIMIT 1")
        RoomData.WalletInfo get();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void update(RoomData.WalletInfo b);
    }
}
