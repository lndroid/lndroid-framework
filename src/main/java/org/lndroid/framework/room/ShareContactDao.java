package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.ShareContact;

class ShareContactDao
        extends ActionDaoBase<WalletData.ShareContactRequest, WalletData.ShareContactResponse>
        implements ShareContact.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.SHARE_CONTACT;

    ShareContactDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomActionDaoBase<WalletData.ShareContactRequest, WalletData.ShareContactResponse>{

        @Override @Transaction
        public WalletData.ShareContactResponse commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.ShareContactResponse res, long time) {
            return commitTransactionImpl(userId, txId, txAuthUserId, res, time);
        }

        @Override @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.ShareContactRequest req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.ShareContactRequest i);

        @Override
        protected long insertRequest(WalletData.ShareContactRequest req) {
            // convert request to room object
            RoomTransactions.ShareContactRequest r = new RoomTransactions.ShareContactRequest();
            r.data = req;

            // insert request
            return insertRequest(r);
        }

        @Override
        public WalletData.ShareContactResponse getResponse(long id) {
            return null;
        }

        @Query("SELECT * FROM txShareContactRequest WHERE id_ = :id")
        abstract RoomTransactions.ShareContactRequest getRequestRoom(long id);

        @Override
        public WalletData.ShareContactRequest getRequest(long id) {
            RoomTransactions.ShareContactRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(WalletData.ShareContactResponse v) {
            // not stored
            return 0;
        }
    }

}

