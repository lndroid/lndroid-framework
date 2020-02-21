package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetChannel;

public class GetChannelDao implements
        IGetDao<WalletData.Channel>, IPluginDao,
        GetChannel.IDao
{

    private DaoRoom dao_;

    GetChannelDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Channel get(long id) {
        RoomData.Channel d = dao_.get(id);
        if (d == null)
            return null;

        WalletData.Channel c = d.getData();
        if (Long.toString(c.id()).equals(c.channelPoint()))
            c = c.toBuilder().setChannelPoint(null).build();

        return c;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM Channel WHERE id = :id")
        RoomData.Channel get(long id);
    }
}
