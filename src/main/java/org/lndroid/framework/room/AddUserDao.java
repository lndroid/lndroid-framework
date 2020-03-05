package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.AddUser;

class AddUserDao
        extends ActionDaoBase<WalletData.AddUserRequest, WalletData.User>
        implements AddUser.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.ADD_USER;

    AddUserDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom extends RoomActionDaoBase<WalletData.AddUserRequest, WalletData.User> {

        @Override @Transaction
        public WalletData.User commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.User req, long time,
                IActionDao.OnResponseMerge<WalletData.User> merger) {
            return commitTransactionImpl(userId, txId, txAuthUserId, req, time, merger);
        }

        @Override @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.AddUserRequest req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.AddUserRequest i);

        @Override
        protected long insertRequest(WalletData.AddUserRequest req) {
            // convert request to room object
            RoomTransactions.AddUserRequest r = new RoomTransactions.AddUserRequest();
            r.data = req;

            // insert request
            return insertRequest(r);
        }

        @Query("SELECT * FROM User WHERE id = :id")
        abstract RoomData.User getResponseRoom(long id);

        @Override
        public WalletData.User getResponse(long id) {
            RoomData.User r = getResponseRoom(id);
            return r != null ? r.getData() : null;
        }

        @Query("SELECT * FROM txAddUserRequest WHERE id_ = :id")
        abstract RoomTransactions.AddUserRequest getRequestRoom(long id);

        @Override
        public WalletData.AddUserRequest getRequest(long id) {
            RoomTransactions.AddUserRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Insert
        abstract void insertResponseRoom(RoomData.User r);

        @Override
        protected long insertResponse(WalletData.User v) {

            RoomData.User ru = new RoomData.User();
            ru.setData(v);

            // write
            insertResponseRoom(ru);

            return v.id();
        }
    }
}

