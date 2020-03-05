package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.AddInvoice;

class AddInvoiceDao
        extends LndActionDaoBase<WalletData.AddInvoiceRequest, WalletData.Invoice>
        implements AddInvoice.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.ADD_INVOICE;

    AddInvoiceDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomLndActionDaoBase<WalletData.AddInvoiceRequest, WalletData.Invoice>
    {
        @Insert
        public abstract long insertResponseRoom(RoomData.Invoice i);

        @Query("SELECT * FROM Invoice WHERE id = :id")
        public abstract RoomData.Invoice getResponseRoom(long id);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract long upsertRequest(RoomTransactions.AddInvoiceRequest i);

        @Query("SELECT * FROM txAddInvoiceRequest WHERE id_ = :id")
        abstract RoomTransactions.AddInvoiceRequest getRequestRoom(long id);

        @Override
        @Transaction
        public WalletData.Invoice commitTransaction(
                long userId, String txId, WalletData.Invoice r, long time,
                ILndActionDao.OnResponseMerge<WalletData.Invoice> merger) {
            return commitTransactionImpl(userId, txId, r, time, merger);
        }

        @Override
        @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.AddInvoiceRequest req) {
            createTransactionImpl(tx, req);
        }

        @Override
        protected long insertRequest(WalletData.AddInvoiceRequest req) {
            // convert request to room object
            RoomTransactions.AddInvoiceRequest r = new RoomTransactions.AddInvoiceRequest();
            r.data = req;

            // insert request
            return upsertRequest(r);
        }

        @Override
        protected void updateRequest(long id, WalletData.AddInvoiceRequest req) {
            // convert request to room object
            RoomTransactions.AddInvoiceRequest r = new RoomTransactions.AddInvoiceRequest();
            r.id_ = id;
            r.data = req;

            // update
            upsertRequest(r);
        }

        @Override
        public WalletData.AddInvoiceRequest getRequest(long id) {
            RoomTransactions.AddInvoiceRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Override
        protected long insertResponse(WalletData.Invoice r) {
            RoomData.Invoice ri = new RoomData.Invoice();
            ri.setData(r);

            // insert
            return insertResponseRoom(ri);
        }

        @Override
        public WalletData.Invoice getResponse(long id) {
            RoomData.Invoice r = getResponseRoom(id);
            return r != null ? r.getData() : null;
        }

        @Override
        @Transaction
        public void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time,
                                       WalletData.AddInvoiceRequest authedRequest) {
            confirmTransactionImpl(txUserId, txId, txAuthUserId, time, authedRequest);
        }
    }
}
