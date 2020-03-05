package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.AddAppContact;


class AddContactDao
        extends ActionDaoBase<WalletData.AddContactRequest, WalletData.Contact>
        implements AddAppContact.IDao
{

    static final String PLUGIN_ID = DefaultPlugins.ADD_APP_CONTACT;

    AddContactDao(DaoRoom dao, RoomTransactions.TransactionDao txDao, RouteHintsDaoRoom routeDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
        dao.routeDao = routeDao;
    }

    @Dao
    abstract static class DaoRoom
            extends RoomActionDaoBase<WalletData.AddContactRequest, WalletData.Contact>{

        RouteHintsDaoRoom routeDao;


        @Override @Transaction
        public WalletData.Contact commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.Contact contact, long time,
                IActionDao.OnResponseMerge<WalletData.Contact> merger) {
            return commitTransactionImpl(userId, txId, txAuthUserId, contact, time, merger);
        }

        @Override @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.AddContactRequest req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.AddContactRequest i);

        private String routeHintsRequestParentId(long id) {
            return "AddContact:"+id;
        }

        @Override
        protected long insertRequest(WalletData.AddContactRequest req) {
            // convert request to room object
            RoomTransactions.AddContactRequest r = new RoomTransactions.AddContactRequest();
            r.data = req;

            // insert request
            final long id = insertRequest(r);

            // write route hints
            routeDao.upsertRouteHints(routeHintsRequestParentId(id), req.routeHints());

            return id;
        }

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void upsertContact(RoomData.Contact i);

        @Query("SELECT * FROM Contact WHERE pubkey = :pubkey")
        abstract RoomData.Contact getContactByPubkey(String pubkey);

        @Query("SELECT * FROM Contact WHERE id = :id")
        abstract RoomData.Contact getResponseRoom(long id);

        WalletData.Contact addRouteHintsResponse(WalletData.Contact c) {
            ImmutableList<WalletData.RouteHint> routeHints = routeDao.getRouteHints(
                    RoomData.routeHintsParentId(c));
            return c.toBuilder().setRouteHints(routeHints).build();
        }

        @Override
        public WalletData.Contact getResponse(long id) {
            RoomData.Contact r = getResponseRoom(id);
            if (r == null)
                return null;

            return addRouteHintsResponse(r.getData());
        }

        @Query("SELECT * FROM txAddContactRequest WHERE id_ = :id")
        abstract RoomTransactions.AddContactRequest getRequestRoom(long id);

        WalletData.AddContactRequest addRouteHintsRequest(long id, WalletData.AddContactRequest c) {
            ImmutableList<WalletData.RouteHint> routeHints = routeDao.getRouteHints(
                    routeHintsRequestParentId(id));
            return c.toBuilder().setRouteHints(routeHints).build();
        }

        @Override
        public WalletData.AddContactRequest getRequest(long id) {
            RoomTransactions.AddContactRequest r = getRequestRoom(id);
            if (r == null)
                return null;

            return addRouteHintsRequest(id, r.data);
        }

        protected WalletData.Contact mergeExisting(
                WalletData.Contact r, IActionDao.OnResponseMerge<WalletData.Contact> merger) {
            RoomData.Contact ri = getContactByPubkey(r.pubkey());
            if (ri == null)
                return r;

             if (merger != null)
                return merger.merge(ri.getData(), r);
            else
                return r.toBuilder().setId(ri.getData().id()).build();
        }

        @Override
        protected long insertResponse(WalletData.Contact contact) {
            // make sure we replace existing contact w/ same pubkey
            RoomData.Contact ri = new RoomData.Contact();
            ri.setData(contact);

            // update
            upsertContact(ri);

            // write route hints
            routeDao.upsertRouteHints(RoomData.routeHintsParentId(contact), contact.routeHints());

            return contact.id();
        }
    }
}

