package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.PeerStateWorker;

public class PeerStateWorkerDao implements PeerStateWorker.IDao, IPluginDao {

    private DaoRoom dao_;

    PeerStateWorkerDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Peer getPeerByPubkey(String pubkey) {
        RoomData.Peer r = dao_.getPeerByPubkey(pubkey);
        return r != null ? r.getData() : null;
    }

    @Override
    public void updatePeer(WalletData.Peer peer) {
        RoomData.Peer r = new RoomData.Peer();
        r.setData(peer);
        dao_.updatePeer(r);
    }

    @Override
    public void updatePeerOnline(String pubkey, boolean online) {
        dao_.updatePeerOnline(pubkey, online);
    }

    @Override
    public void updatePeersOffline() {
        dao_.updatePeersOffline();
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {
        @Query("SELECT * FROM Peer WHERE pubkey = :pubkey")
        abstract RoomData.Peer getPeerByPubkey(String pubkey);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updatePeer(RoomData.Peer i);

        @Query("UPDATE Peer SET online = :online WHERE pubkey = :pubkey")
        abstract void updatePeerOnline(String pubkey, boolean online);

        @Query("UPDATE Peer SET online = 0 WHERE online != 0")
        abstract void updatePeersOffline();
    }

}

