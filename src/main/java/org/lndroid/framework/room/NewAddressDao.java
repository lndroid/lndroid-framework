package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.NewAddress;

class NewAddressDao
        extends LndActionDaoBase<WalletData.NewAddressRequest, WalletData.NewAddress>
        implements NewAddress.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.NEW_ADDRESS;

    NewAddressDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomLndActionDaoBase<WalletData.NewAddressRequest, WalletData.NewAddress>
    {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract long upsertRequest(RoomTransactions.NewAddressRequest i);

        @Query("SELECT * FROM txNewAddressRequest WHERE id_ = :id")
        abstract RoomTransactions.NewAddressRequest getRequestRoom(long id);

        @Override
        @Transaction
        public WalletData.NewAddress commitTransaction(
                long userId, String txId, WalletData.NewAddress r, long time,
                ILndActionDao.OnResponseMerge<WalletData.NewAddress> merger) {
            return commitTransactionImpl(userId, txId, r, time, merger);
        }

        @Override
        @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.NewAddressRequest req) {
            createTransactionImpl(tx, req);
        }

        @Override
        protected long insertRequest(WalletData.NewAddressRequest req) {
            // convert request to room object
            RoomTransactions.NewAddressRequest r = new RoomTransactions.NewAddressRequest();
            r.data = req;

            // insert request
            return upsertRequest(r);
        }

        @Override
        protected void updateRequest(long id, WalletData.NewAddressRequest req) {
            // convert request to room object
            RoomTransactions.NewAddressRequest r = new RoomTransactions.NewAddressRequest();
            r.id_ = id;
            r.data = req;

            // update
            upsertRequest(r);
        }

        @Override
        public WalletData.NewAddressRequest getRequest(long id) {
            RoomTransactions.NewAddressRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(WalletData.NewAddress r,
                                      ILndActionDao.OnResponseMerge<WalletData.NewAddress> merger) {
            // not stored
            return 0;
        }

        @Override
        public WalletData.NewAddress getResponse(long id) {
            // not stored
            return null;
        }

        @Override
        @Transaction
        public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time,
                                       WalletData.NewAddressRequest authedRequest) {
            confirmTransactionImpl(txUserId, txId, txAuthUserId, time, authedRequest);
        }
    }
}
