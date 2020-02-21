package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.AddListContactsPrivilege;

class AddListContactsPrivilegeDao
        extends ActionDaoBase<WalletData.ListContactsPrivilege, WalletData.ListContactsPrivilege>
        implements AddListContactsPrivilege.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.ADD_LIST_CONTACTS_PRIVILEGE;

    AddListContactsPrivilegeDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
    }

    @Dao
    abstract static class DaoRoom
            extends RoomActionDaoBase<WalletData.ListContactsPrivilege, WalletData.ListContactsPrivilege> {

        @Override @Transaction
        public WalletData.ListContactsPrivilege commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.ListContactsPrivilege req, long time) {
            return commitTransactionImpl(userId, txId, txAuthUserId, req, time);
        }

        @Override @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.ListContactsPrivilege req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.ListContactsPrivilege i);

        @Override
        protected long insertRequest(WalletData.ListContactsPrivilege req) {
            // convert request to room object
            RoomTransactions.ListContactsPrivilege r = new RoomTransactions.ListContactsPrivilege();
            r.data = req;

            // insert request
            return insertRequest(r);
        }

        @Query("SELECT * FROM ListContactsPrivilege WHERE id = :id")
        abstract RoomData.ListContactsPrivilege getResponseRoom(long id);

        @Override
        public WalletData.ListContactsPrivilege getResponse(long id) {
            RoomData.ListContactsPrivilege r = getResponseRoom(id);
            return r != null ? r.getData() : null;
        }

        @Query("SELECT * FROM txListContactsPrivilege WHERE id = :id")
        abstract RoomTransactions.ListContactsPrivilege getRequestRoom(long id);

        @Override
        public WalletData.ListContactsPrivilege getRequest(long id) {
            RoomTransactions.ListContactsPrivilege r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        @Query("SELECT * FROM ListContactsPrivilege WHERE userId = :userId")
        abstract RoomData.ListContactsPrivilege getExisting(long userId);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void upsert(RoomData.ListContactsPrivilege r);

        @Override
        protected long insertResponse(WalletData.ListContactsPrivilege v) {

            RoomData.ListContactsPrivilege rv = getExisting(v.userId());
            if (rv == null) {
                rv = new RoomData.ListContactsPrivilege();
            } else {
                // reuse existing id
                v = v.toBuilder().setId(rv.getData().id()).build();
            }

            rv.setData(v);

            // write
            upsert(rv);

            return v.id();
        }
    }
}

