package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IChannelStateWorkerDao;
import org.lndroid.framework.engine.IPluginDao;

import java.util.ArrayList;
import java.util.List;

public class ChannelStateWorkerDao implements IChannelStateWorkerDao, IPluginDao {

    private ChannelStateWorkerDaoRoom dao_;

    ChannelStateWorkerDao(ChannelStateWorkerDaoRoom dao) { dao_ = dao; }

    @Override
    public WalletData.Channel getChannelByChannelPoint(String channelPoint) {
        RoomData.Channel c = dao_.getChannelByChannelPoint(channelPoint);
        return c != null ? c.getData() : null;
    }

    @Override
    public List<WalletData.Channel> getOpeningChannels() {
        List<RoomData.Channel> rs = dao_.getChannels(WalletData.CHANNEL_STATE_OPENING);
        List<WalletData.Channel> cs = new ArrayList<>();
        for(RoomData.Channel rc: rs)
            cs.add(rc.getData());
        return cs;
    }

    @Override
    public void updateChannel(WalletData.Channel c) {
        RoomData.Channel r = new RoomData.Channel();
        r.setData(c);
        dao_.updateChannel(r);
    }

    @Override
    public void setChannelActive(String channelPoint, boolean active) {
        dao_.setChannelActive(channelPoint, active);
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
interface ChannelStateWorkerDaoRoom {
    @Query("SELECT * FROM Channel WHERE channelPoint = :channelPoint")
    RoomData.Channel getChannelByChannelPoint(String channelPoint);

    @Query("SELECT * FROM Channel WHERE state = :state")
    List<RoomData.Channel> getChannels(int state);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void updateChannel(RoomData.Channel c);

    @Query("UPDATE Channel SET active = :active WHERE channelPoint = :channelPoint")
    void setChannelActive(String channelPoint, boolean active);
}
