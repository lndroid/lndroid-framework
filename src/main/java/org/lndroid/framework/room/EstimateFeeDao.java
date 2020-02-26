package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.EstimateFee;

import java.util.List;

public class EstimateFeeDao
        extends LndActionDaoBase<WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse>
        implements EstimateFee.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.ESTIMATE_FEE;

    EstimateFeeDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomLndActionDaoBase<WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse> {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract long upsertRequest(RoomTransactions.EstimateFeeRequest i);

        @Query("SELECT * FROM txEstimateFeeRequest WHERE id_ = :id")
        abstract RoomTransactions.EstimateFeeRequest getRequestRoom(long id);

        @Override
        @Transaction
        public WalletData.EstimateFeeResponse commitTransaction(
                long userId, String txId, WalletData.EstimateFeeResponse r, long time,
                ILndActionDao.OnResponseMerge<WalletData.EstimateFeeResponse> merger) {
            return commitTransactionImpl(userId, txId, r, time, merger);
        }

        @Override
        @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.EstimateFeeRequest req) {
            createTransactionImpl(tx, req);
        }

        @Override
        protected long insertRequest(WalletData.EstimateFeeRequest req) {
            // convert request to room object
            RoomTransactions.EstimateFeeRequest r = new RoomTransactions.EstimateFeeRequest();
            r.data = req;

            // insert request
            return upsertRequest(r);
        }

        @Override
        protected void updateRequest(long id, WalletData.EstimateFeeRequest req) {
            // convert request to room object
            RoomTransactions.EstimateFeeRequest r = new RoomTransactions.EstimateFeeRequest();
            r.id_ = id;
            r.data = req;

            // update
            upsertRequest(r);
        }

        @Override
        public WalletData.EstimateFeeRequest getRequest(long id) {
            RoomTransactions.EstimateFeeRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(WalletData.EstimateFeeResponse r,
                                      ILndActionDao.OnResponseMerge<WalletData.EstimateFeeResponse> merger) {
            // not stored
            return 0;
        }

        @Override
        public WalletData.EstimateFeeResponse getResponse(long id) {
            // not stored
            return null;
        }

        @Override
        @Transaction
        public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time,
                                       WalletData.EstimateFeeRequest authedRequest) {
            confirmTransactionImpl(txUserId, txId, txAuthUserId, time, authedRequest);
        }
    }
}

