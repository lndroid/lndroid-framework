package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IChannelBalanceDao;
import org.lndroid.framework.engine.IPluginDao;

public class ChannelBalanceDao implements IChannelBalanceDao, IPluginDao {
    private ChannelBalanceDaoRoom dao_;

    ChannelBalanceDao(ChannelBalanceDaoRoom dao) {
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
}

@Dao
interface ChannelBalanceDaoRoom {
    @Query("SELECT * FROM ChannelBalance WHERE id_ = 0")
    RoomData.ChannelBalance get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(RoomData.ChannelBalance b);
}
