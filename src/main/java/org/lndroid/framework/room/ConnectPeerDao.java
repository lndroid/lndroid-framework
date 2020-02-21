package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.ConnectPeer;

public class ConnectPeerDao
        extends LndActionDaoBase<WalletData.ConnectPeerRequest, WalletData.ConnectPeerResponse>
        implements ConnectPeer.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.CONNECT_PEER;

    ConnectPeerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomLndActionDaoBase<WalletData.ConnectPeerRequest, WalletData.ConnectPeerResponse>
    {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract long upsertRequest(RoomTransactions.ConnectPeerRequest i);

        @Query("SELECT * FROM txConnectPeerRequest WHERE id_ = :id")
        abstract RoomTransactions.ConnectPeerRequest getRequestRoom(long id);

        @Override
        @Transaction
        public WalletData.ConnectPeerResponse commitTransaction(
                long userId, String txId, WalletData.ConnectPeerResponse r, long time) {
            return commitTransactionImpl(userId, txId, r, time);
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

        @Override
        protected long insertResponse(WalletData.ConnectPeerResponse r) {
            // not stored
            return 0;
        }

        @Override
        public WalletData.ConnectPeerResponse getResponse(long id) {
            // not stored
            return null;
        }

        @Override
        @Transaction
        public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time,
                                       WalletData.ConnectPeerRequest authedRequest) {
            confirmTransactionImpl(txUserId, txId, txAuthUserId, time, authedRequest);
        }
    }

}

