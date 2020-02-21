package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IJobDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.OpenChannel;
import org.lndroid.framework.plugins.Transaction;

public class OpenChannelDao
        extends ActionDaoBase<WalletData.OpenChannelRequest, WalletData.Channel>
        implements OpenChannel.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.OPEN_CHANNEL;

    OpenChannelDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom extends RoomActionDaoBase<WalletData.OpenChannelRequest, WalletData.Channel>{

        @Override @androidx.room.Transaction
        public WalletData.Channel commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.Channel r, long time) {
            return commitTransactionImpl(userId, txId, txAuthUserId, r, time);
        }

        @Override @androidx.room.Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.OpenChannelRequest req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.OpenChannelRequest i);

        @Override
        protected long insertRequest(WalletData.OpenChannelRequest req) {
            // convert request to room object
            RoomTransactions.OpenChannelRequest r = new RoomTransactions.OpenChannelRequest();
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

        @Query("SELECT * FROM txOpenChannelRequest WHERE id_ = :id")
        abstract RoomTransactions.OpenChannelRequest getRequestRoom(long id);

        @Override
        public WalletData.OpenChannelRequest getRequest(long id) {
            RoomTransactions.OpenChannelRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Insert
        public abstract void insertResponseRoom(RoomData.Channel i);

        @Query("UPDATE Channel SET channelPoint = :cp WHERE id = :id")
        abstract void setFakeChannelPoint(long id, String cp);

        @Override
        protected long insertResponse(WalletData.Channel v) {
            RoomData.Channel r = new RoomData.Channel();
            r.setData(v);

            insertResponseRoom(r);
            setFakeChannelPoint(v.id(), Long.toString(v.id()));

            return v.id();
        }
    }
}