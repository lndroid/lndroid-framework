package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IOpenChannelWorkerDao;
import org.lndroid.framework.engine.IPluginDao;

public class OpenChannelWorkerDao implements IOpenChannelWorkerDao, IPluginDao {
    private OpenChannelWorkerDaoRoom dao_;

    OpenChannelWorkerDao(OpenChannelWorkerDaoRoom dao) {
        dao_ = dao;
    }

    private List<WalletData.Channel> fromRoom(List<RoomData.Channel> list) {
        List<WalletData.Channel> r = new ArrayList<>();
        for (RoomData.Channel c: list)
            r.add(c.getData());
        return r;
    }

    @Override
    public List<WalletData.Channel> getOpeningChannels() {
        return fromRoom(dao_.getChannels(WalletData.CHANNEL_STATE_OPENING));
    }

    @Override
    public List<WalletData.Channel> getRetryChannels() {
        return fromRoom(dao_.getChannels(WalletData.CHANNEL_STATE_RETRY));
    }

    @Override
    public List<WalletData.Channel> getNewChannels() {
        return fromRoom(dao_.getChannels(WalletData.CHANNEL_STATE_NEW));
    }

    @Override
    public void updateChannel(WalletData.Channel c) {
        RoomData.Channel d = new RoomData.Channel();
        d.setData(c);
        dao_.updateChannel(d);
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
interface OpenChannelWorkerDaoRoom {
    @Query("SELECT * FROM Channel WHERE state = :state")
    List<RoomData.Channel> getChannels(int state);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void updateChannel(RoomData.Channel c);
}
