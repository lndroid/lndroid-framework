package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.CloseChannel;

class CloseChannelDao
        extends JobDaoBase<WalletData.CloseChannelRequest, WalletData.Channel>
        implements CloseChannel.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.CLOSE_CHANNEL;

    CloseChannelDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom extends RoomJobDaoBase<WalletData.CloseChannelRequest, WalletData.Channel>{

        @Override @androidx.room.Transaction
        public WalletData.Channel commitTransaction(
                long txUserId, String txId, long txAuthUserId, WalletData.Channel r, long time,
                int maxTries, long maxTryTime) {
            return commitTransactionImpl(txUserId, txId, txAuthUserId, r, time, maxTries, maxTryTime);
        }

        @Override @androidx.room.Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.CloseChannelRequest req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.CloseChannelRequest i);

        @Override
        protected long insertRequest(WalletData.CloseChannelRequest req) {
            // convert request to room object
            RoomTransactions.CloseChannelRequest r = new RoomTransactions.CloseChannelRequest();
            r.data = req;

            // insert request
            return insertRequest(r);
        }

        @Query("SELECT * FROM Channel WHERE id = :id")
        abstract RoomData.Channel getResponseRoom(long id);

        @Override
        public WalletData.Channel getResponse(long id) {
            RoomData.Channel r = getResponseRoom(id);
            return r != null ? r.getData() : null;
        }

        @Query("SELECT * FROM txCloseChannelRequest WHERE id_ = :id")
        abstract RoomTransactions.CloseChannelRequest getRequestRoom(long id);

        @Override
        public WalletData.CloseChannelRequest getRequest(long id) {
            RoomTransactions.CloseChannelRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(WalletData.Channel v) {
            // it's already stored and not modified by this job request
            return v.id();
        }
    }
}
