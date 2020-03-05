package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.SetNotifiedInvoices;

import java.util.List;

public class SetNotifiedInvoicesDao
        extends ActionDaoBase<WalletData.NotifiedInvoicesRequest, WalletData.NotifiedInvoicesResponse>
        implements SetNotifiedInvoices.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.SET_NOTIFIED_INVOICES;

    DaoRoom dao_;

    SetNotifiedInvoicesDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
        dao_ = dao;
    }

    @Override
    public void setNotifyTime(ImmutableList<Long> invoiceIds) {
        dao_.setNotifyTime(invoiceIds, System.currentTimeMillis());
    }

    @Dao
    abstract static class DaoRoom
            extends RoomActionDaoBase<WalletData.NotifiedInvoicesRequest, WalletData.NotifiedInvoicesResponse> {

        @Query("UPDATE Invoice SET notifyTime = :time WHERE settleTime != 0 AND notifyTime = 0 AND id IN (:ids)")
        abstract void setNotifyTime(List<Long> ids, long time);

        @Override @Transaction
        public WalletData.NotifiedInvoicesResponse commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.NotifiedInvoicesResponse req,
                long time, IActionDao.OnResponseMerge<WalletData.NotifiedInvoicesResponse> merger) {
            return commitTransactionImpl(userId, txId, txAuthUserId, req, time, merger);
        }

        @Override @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.NotifiedInvoicesRequest req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.NotifiedInvoicesRequest i);

        @Override
        protected long insertRequest(WalletData.NotifiedInvoicesRequest req) {
            // convert request to room object
            RoomTransactions.NotifiedInvoicesRequest r = new RoomTransactions.NotifiedInvoicesRequest();
            r.data = req;

            // insert request
            return insertRequest(r);
        }

        @Override
        public WalletData.NotifiedInvoicesResponse getResponse(long id) {
            // not stored
            return null;
        }

        @Query("SELECT * FROM txNotifiedInvoicesRequest WHERE id_ = :id")
        abstract RoomTransactions.NotifiedInvoicesRequest getRequestRoom(long id);

        @Override
        public WalletData.NotifiedInvoicesRequest getRequest(long id) {
            RoomTransactions.NotifiedInvoicesRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(
                WalletData.NotifiedInvoicesResponse v) {
            // not stored
            return 0;
        }
    }
}

