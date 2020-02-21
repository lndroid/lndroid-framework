package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.google.common.collect.ImmutableList;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.AddAppContact;


class AddAppContactDao
        extends ActionDaoBase<WalletData.Contact, WalletData.Contact>
        implements AddAppContact.IDao
{

    static final String PLUGIN_ID = DefaultPlugins.ADD_APP_CONTACT;

    AddAppContactDao(DaoRoom dao, RoomTransactions.TransactionDao txDao, RouteHintsDaoRoom routeDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
        dao.routeDao = routeDao;
    }

    @Dao
    abstract static class DaoRoom extends RoomActionDaoBase<WalletData.Contact, WalletData.Contact>{

        RouteHintsDaoRoom routeDao;


        @Override @Transaction
        public WalletData.Contact commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.Contact contact, long time) {
            return commitTransactionImpl(userId, txId, txAuthUserId, contact, time);
        }

        @Override @Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.Contact req){
            createTransactionImpl(tx, req);
        }

        @Insert
        abstract long insertRequest(RoomTransactions.Contact i);

        private String routeHintsRequestParentId(long id) {
            return "AddAppContact:"+id;
        }

        @Override
        protected long insertRequest(WalletData.Contact req) {
            // convert request to room object
            RoomTransactions.Contact r = new RoomTransactions.Contact();
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

        @Override
        public WalletData.Contact getResponse(long id) {
            RoomData.Contact r = getResponseRoom(id);
            if (r == null)
                return null;

            return addRouteHints(id, r.getData());
        }

        @Query("SELECT * FROM txContact WHERE id_ = :id")
        abstract RoomTransactions.Contact getRequestRoom(long id);

        WalletData.Contact addRouteHints(long id, WalletData.Contact c) {
            ImmutableList<WalletData.RouteHint> routeHints = routeDao.getRouteHints(
                    routeHintsRequestParentId(id));
            return c.toBuilder().setRouteHints(routeHints).build();
        }

        @Override
        public WalletData.Contact getRequest(long id) {
            RoomTransactions.Contact r = getRequestRoom(id);
            if (r == null)
                return null;

            return addRouteHints(id, r.data);
        }

        @Override
        protected long insertResponse(WalletData.Contact contact) {
            // make sure we replace existing contact w/ same pubkey
            RoomData.Contact ri = getContactByPubkey(contact.pubkey());
            if (ri == null) {
                ri = new RoomData.Contact();
                // drop newly generated id, reuse old one
                contact = contact.toBuilder().setId(ri.getData().id()).build();
            }
            ri.setData(contact);

            // update
            upsertContact(ri);

            // write route hints
            routeDao.upsertRouteHints(RoomData.routeHintsParentId(contact), contact.routeHints());

            return contact.id();
        }
    }
}

