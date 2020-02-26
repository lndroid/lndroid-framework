package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetPeer;

public class GetPeerDao implements
        IGetDao<WalletData.Peer>, IPluginDao,
        GetPeer.IDao
{
    private DaoRoom dao_;

    GetPeerDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Peer get(long id) {
        RoomData.Peer r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM Peer WHERE id = :id")
        RoomData.Peer get(long id);
    }
}

