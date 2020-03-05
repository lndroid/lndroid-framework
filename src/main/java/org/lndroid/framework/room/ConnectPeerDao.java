package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.ConnectPeer;

public class ConnectPeerDao
        extends LndActionDaoBase<WalletData.ConnectPeerRequest, WalletData.Peer>
        implements ConnectPeer.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.CONNECT_PEER;

    ConnectPeerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomLndActionDaoBase<WalletData.ConnectPeerRequest, WalletData.Peer>
    {
        @Override
        @Transaction
        public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time,
                                       WalletData.ConnectPeerRequest authedRequest) {
            confirmTransactionImpl(txUserId, txId, txAuthUserId, time, authedRequest);
        }

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract long upsertRequest(RoomTransactions.ConnectPeerRequest i);

        @Query("SELECT * FROM txConnectPeerRequest WHERE id_ = :id")
        abstract RoomTransactions.ConnectPeerRequest getRequestRoom(long id);

        @Override
        @Transaction
        public WalletData.Peer commitTransaction(
                long userId, String txId, WalletData.Peer r, long time,
                ILndActionDao.OnResponseMerge<WalletData.Peer> merger) {
            return commitTransactionImpl(userId, txId, r, time, merger);
        }

        @Override
        @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.ConnectPeerRequest req) {
            createTransactionImpl(tx, req);
        }

        @Override
        protected long insertRequest(WalletData.ConnectPeerRequest req) {
            // convert request to room object
            RoomTransactions.ConnectPeerRequest r = new RoomTransactions.ConnectPeerRequest();
            r.data = req;

            // insert request
            return upsertRequest(r);
        }

        @Override
        protected void updateRequest(long id, WalletData.ConnectPeerRequest req) {
            // convert request to room object
            RoomTransactions.ConnectPeerRequest r = new RoomTransactions.ConnectPeerRequest();
            r.id_ = id;
            r.data = req;

            // update
            upsertRequest(r);
        }

        @Override
        public WalletData.ConnectPeerRequest getRequest(long id) {
            RoomTransactions.ConnectPeerRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void upsertPeer(RoomData.Peer i);

        @Query("SELECT * FROM Peer WHERE pubkey = :pubkey")
        abstract RoomData.Peer getPeerByPubkey(String pubkey);

        @Query("SELECT * FROM Peer WHERE id = :id")
        abstract RoomData.Peer getResponseRoom(long id);

        @Override
        public WalletData.Peer getResponse(long id) {
            RoomData.Peer r = getResponseRoom(id);
            return r != null ? r.getData() : null;
        }

        @Override
        protected WalletData.Peer mergeExisting(
                WalletData.Peer peer, ILndActionDao.OnResponseMerge<WalletData.Peer> merger) {
            // make sure we replace existing peer w/ same pubkey
            RoomData.Peer ri = getPeerByPubkey(peer.pubkey());
            if (ri == null)
                return peer;

            // merge existing record w/ new one
            if (merger != null)
                return merger.merge(ri.getData(), peer);
            else
                return peer.toBuilder().setId(ri.getData().id()).build();
        }

        @Override
        protected long insertResponse(WalletData.Peer peer) {

            RoomData.Peer ri = new RoomData.Peer();
            ri.setData(peer);

            // update
            upsertPeer(ri);

            return peer.id();
        }
    }

}

