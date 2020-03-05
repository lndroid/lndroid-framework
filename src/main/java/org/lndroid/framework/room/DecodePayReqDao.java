package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.DecodePayReq;

public class DecodePayReqDao
        extends LndActionDaoBase<String, WalletData.SendPayment>
        implements DecodePayReq.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.DECODE_PAYREQ;

    DecodePayReqDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomLndActionDaoBase<String, WalletData.SendPayment> {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract long upsertRequest(RoomTransactions.PayReqString i);

        @Query("SELECT * FROM txPayReqString WHERE id_ = :id")
        abstract RoomTransactions.PayReqString getRequestRoom(long id);

        @Override
        @Transaction
        public WalletData.SendPayment commitTransaction(
                long userId, String txId, WalletData.SendPayment r, long time,
                ILndActionDao.OnResponseMerge<WalletData.SendPayment> merger) {
            return commitTransactionImpl(userId, txId, r, time, merger);
        }

        @Override
        @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, String req) {
            createTransactionImpl(tx, req);
        }

        @Override
        protected long insertRequest(String req) {
            // convert request to room object
            RoomTransactions.PayReqString r = new RoomTransactions.PayReqString();
            r.data = req;

            // insert request
            return upsertRequest(r);
        }

        @Override
        protected void updateRequest(long id, String req) {
            // convert request to room object
            RoomTransactions.PayReqString r = new RoomTransactions.PayReqString();
            r.id_ = id;
            r.data = req;

            // update
            upsertRequest(r);
        }

        @Override
        public String getRequest(long id) {
            RoomTransactions.PayReqString r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(WalletData.SendPayment r) {
            // not stored
            return 0;
        }

        @Override
        public WalletData.SendPayment getResponse(long id) {
            // not stored
            return null;
        }

        @Override
        @Transaction
        public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time,
                                       String authedRequest) {
            confirmTransactionImpl(txUserId, txId, txAuthUserId, time, authedRequest);
        }
    }
}
