package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.NodeInfoWorker;

public class NodeInfoDao implements NodeInfoWorker.IDao, IPluginDao {

    private DaoRoom dao_;

    NodeInfoDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public String getWalletPubkey() {
        return dao_.getWalletPubkey();
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public void updateNode(WalletData.LightningNode node) {
        dao_.updateNode(node);
    }

    public void updateChannels(List<WalletData.ChannelEdge> channels) {
        dao_.updateChannels(channels);
    }


    @Dao
    abstract static class DaoRoom {

        @Query("SELECT identityPubkey FROM WalletInfo LIMIT 1")
        abstract String getWalletPubkey();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updateNode(RoomData.LightningNode node);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updateChannel(RoomData.ChannelEdge ce);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updateRoutingPolicy(RoomData.RoutingPolicy rp);

        public void updateNode(WalletData.LightningNode node) {
            RoomData.LightningNode rn = new RoomData.LightningNode();
            rn.setData(node);
            updateNode(rn);
        }

        @Transaction
        public void updateChannels(List<WalletData.ChannelEdge> channels) {
            for(WalletData.ChannelEdge c: channels) {
                RoomData.ChannelEdge rc = new RoomData.ChannelEdge();
                rc.setData(c);
                updateChannel(rc);

                RoomData.RoutingPolicy rp1 = new RoomData.RoutingPolicy();
                rp1.setData(c.node1Policy());
                updateRoutingPolicy(rp1);

                RoomData.RoutingPolicy rp2 = new RoomData.RoutingPolicy();
                rp2.setData(c.node2Policy());
                updateRoutingPolicy(rp2);
            }
        }
    }
}
