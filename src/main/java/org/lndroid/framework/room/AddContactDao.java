package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.lndroid.framework.WalletData;


class AddContactDao extends ActionDaoBase<WalletData.Contact, WalletData.Contact, RoomTransactions.AddContactTransaction> {

    AddContactDao(AddContactDaoRoom dao, RouteHintsDaoRoom routeDao) {
        super(dao, RoomTransactions.AddContactTransaction.class);
        dao.setRouteDao(routeDao);
    }
}

@Dao
abstract class AddContactDaoRoom
        implements IRoomActionDao<RoomTransactions.AddContactTransaction, WalletData.Contact>{

    private RouteHintsDaoRoom routeDao_;

    void setRouteDao(RouteHintsDaoRoom routeDao) {
        routeDao_ = routeDao;
    }

    @Override @Query("SELECT * FROM AddContactTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.AddContactTransaction> getTransactions();

    @Override @Query("SELECT * FROM AddContactTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.AddContactTransaction getTransaction(int txUserId, String txId);

    @Override @Insert
    public abstract void createTransaction(RoomTransactions.AddContactTransaction tx);

    @Override @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.AddContactTransaction tx);

    @Override
    @Query("UPDATE AddContactTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void failTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long upsertContact(RoomData.Contact i);

    // helper to persist the embedded user id
    // after auto-incremented user id was assigned during the insert
    @Query("UPDATE Contact SET id = id_ WHERE id_ = :id")
    abstract void setContactId(long id);

    @Query("SELECT * FROM Contact WHERE pubkey = :pubkey")
    abstract RoomData.Contact getContactByPubkey(String pubkey);

    @Override @Transaction
    public WalletData.Contact commitTransaction(
            RoomTransactions.AddContactTransaction tx, int txAuthUserId, WalletData.Contact contact, long time) {

        // make sure we replace existing contact w/ same pubkey
        RoomData.Contact ri = getContactByPubkey(contact.pubkey());
        if (ri == null) {
            ri = new RoomData.Contact();
        }
        ri.setData(contact);

        // insert
        final long id = upsertContact(ri);
        setContactId(id);

        // set id
        contact = contact.toBuilder().setId(id).build();

        // write route hints
        routeDao_.upsertRouteHints(RoomData.routeHintsParentId(contact), contact.routeHints());

        // update state
        tx.response = contact;
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;
        tx.txData.txAuthUserId = txAuthUserId;
        tx.txData.txAuthTime = time;

        // write tx
        updateTransaction(tx);

        return contact;
    }
}
