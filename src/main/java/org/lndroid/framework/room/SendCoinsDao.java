package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IJobDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.SendCoins;
import org.lndroid.framework.plugins.Transaction;

import java.util.ArrayList;
import java.util.List;

public class SendCoinsDao
        extends JobDaoBase<WalletData.SendCoinsRequest, WalletData.Transaction>
        implements SendCoins.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.SEND_COINS;

    SendCoinsDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom extends RoomJobDaoBase<WalletData.SendCoinsRequest, WalletData.Transaction>{

        public WalletData.Transaction commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.Transaction r, long time) {
            throw new RuntimeException("Unsupported method");
        }

        @Override @androidx.room.Transaction
        public WalletData.Transaction commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.Transaction r, long time,
                int maxTries, long maxTryTime) {
            return commitTransactionImpl(userId, txId, txAuthUserId, r, time, maxTries, maxTryTime);
        }

        @Override @androidx.room.Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.SendCoinsRequest req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.SendCoinsRequest i);

        @Override
        protected long insertRequest(WalletData.SendCoinsRequest req) {
            // convert request to room object
            RoomTransactions.SendCoinsRequest r = new RoomTransactions.SendCoinsRequest();
            r.data = req;

            // insert request
            return insertRequest(r);
        }

        @Query("SELECT * FROM 'Transaction' WHERE id = :id")
        abstract RoomData.Transaction getResponseRoom(long id);

        @Override
        public WalletData.Transaction getResponse(long id) {
            RoomData.Transaction r = getResponseRoom(id);
            return r != null ? r.getData() : null;
        }

        @Query("SELECT * FROM txSendCoinsRequest WHERE id_ = :id")
        abstract RoomTransactions.SendCoinsRequest getRequestRoom(long id);

        @Override
        public WalletData.SendCoinsRequest getRequest(long id) {
            RoomTransactions.SendCoinsRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Insert
        public abstract void insertResponseRoom(RoomData.Transaction i);

        @Override
        protected long insertResponse(WalletData.Transaction v) {
            RoomData.Transaction r = new RoomData.Transaction();
            r.setData(v);
            insertResponseRoom(r);
            return v.id();
        }
    }
}
