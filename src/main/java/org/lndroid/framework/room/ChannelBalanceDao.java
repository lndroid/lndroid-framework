package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IChannelBalanceDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.ChannelBalanceWorker;
import org.lndroid.framework.plugins.GetChannelBalance;

public class ChannelBalanceDao implements
        IChannelBalanceDao, IPluginDao,
        ChannelBalanceWorker.IDao,
        GetChannelBalance.IDao
{
    private DaoRoom dao_;

    ChannelBalanceDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.ChannelBalance get() {
        RoomData.ChannelBalance r = dao_.get();
        return (r != null) ? r.data : null;
    }

    @Override
    public void update(WalletData.ChannelBalance b) {
        RoomData.ChannelBalance r = new RoomData.ChannelBalance();
        r.data = b;
        dao_.update(r);
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM ChannelBalance LIMIT 1")
        RoomData.ChannelBalance get();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void update(RoomData.ChannelBalance b);
    }
}
