package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.AddContactPaymentsPrivilege;

class AddContactPaymentsPrivilegeDao
        extends ActionDaoBase<WalletData.ContactPaymentsPrivilege, WalletData.ContactPaymentsPrivilege>
        implements AddContactPaymentsPrivilege.IDao
{
    static final String PLUGIN_ID = DefaultPlugins.ADD_APP_CONTACT;

    AddContactPaymentsPrivilegeDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomActionDaoBase<WalletData.ContactPaymentsPrivilege, WalletData.ContactPaymentsPrivilege> {

        @Override @Transaction
        public WalletData.ContactPaymentsPrivilege commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.ContactPaymentsPrivilege req,
                long time, IActionDao.OnResponseMerge<WalletData.ContactPaymentsPrivilege> merger) {
            return commitTransactionImpl(userId, txId, txAuthUserId, req, time, merger);
        }

        @Override @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.ContactPaymentsPrivilege req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.ContactPaymentsPrivilege i);

        @Override
        protected long insertRequest(WalletData.ContactPaymentsPrivilege req) {
            // convert request to room object
            RoomTransactions.ContactPaymentsPrivilege r = new RoomTransactions.ContactPaymentsPrivilege();
            r.data = req;

            // insert request
            return insertRequest(r);
        }

        @Query("SELECT * FROM ContactPaymentsPrivilege WHERE id = :id")
        abstract RoomData.ContactPaymentsPrivilege getResponseRoom(long id);

        @Override
        public WalletData.ContactPaymentsPrivilege getResponse(long id) {
            RoomData.ContactPaymentsPrivilege r = getResponseRoom(id);
            return r != null ? r.getData() : null;
        }

        @Query("SELECT * FROM txContactPaymentsPrivilege WHERE id_ = :id")
        abstract RoomTransactions.ContactPaymentsPrivilege getRequestRoom(long id);

        @Override
        public WalletData.ContactPaymentsPrivilege getRequest(long id) {
            RoomTransactions.ContactPaymentsPrivilege r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Query("SELECT * FROM ContactPaymentsPrivilege WHERE userId = :userId AND contactId = :contactId")
        abstract RoomData.ContactPaymentsPrivilege getExisting(long userId, long contactId);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void upsert(RoomData.ContactPaymentsPrivilege r);


        @Override
        protected WalletData.ContactPaymentsPrivilege mergeExisting(
                WalletData.ContactPaymentsPrivilege v, IActionDao.OnResponseMerge<WalletData.ContactPaymentsPrivilege> merger) {

            RoomData.ContactPaymentsPrivilege rv = getExisting(v.userId(), v.contactId());
            if (rv == null)
                return v;

            // merge
            if (merger != null)
                return merger.merge(rv.getData(), v);
            else
                return v.toBuilder().setId(rv.getData().id()).build();
        }

        @Override
        protected long insertResponse(
                WalletData.ContactPaymentsPrivilege v) {

            RoomData.ContactPaymentsPrivilege rv = new RoomData.ContactPaymentsPrivilege();
            rv.setData(v);

            // write
            upsert(rv);

            return v.id();
        }
    }
}

