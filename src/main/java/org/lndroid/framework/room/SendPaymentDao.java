package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ISendPaymentDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Transaction;

public class SendPaymentDao implements ISendPaymentDao, IPluginDao {
    private SendPaymentDaoRoom dao_;

    SendPaymentDao(SendPaymentDaoRoom dao, RouteHintsDaoRoom routeDao) {
        dao_ = dao;
        dao_.setRouteDao(routeDao);
    }

    private Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> fromRoom(RoomTransactions.SendPaymentTransaction tx) {
        Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> t = new Transaction<>();
        RoomConverters.TxConverter.toTx(tx.getTxData(), t);
        t.request = tx.getRequest();
        t.response = tx.getResponse();
        return t;
    }

    @Override
    public String walletPubkey() {
        return dao_.walletPubkey();
    }

    @Override
    public WalletData.Contact getContact(long contactId) {
        return dao_.getContact(contactId);
    }

    @Override
    public boolean hasPrivilege(WalletData.SendPaymentRequest req, WalletData.User user) {
        if (req.contactId() != 0)
            return dao_.hasContactPaymentsPrivilege(user.id(), req.contactId());
        return false;
    }

    @Override
    public List<Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment>> getTransactions() {
        List<Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment>> r = new ArrayList<>();

        List<RoomTransactions.SendPaymentTransaction> txs = dao_.getTransactions();
        for (RoomTransactions.SendPaymentTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> getTransaction(int txUserId, String txId) {
        RoomTransactions.SendPaymentTransaction tx = dao_.getTransaction(txUserId, txId);
        if (tx == null)
            return null;

        return fromRoom(tx);
    }

    @Override
    public void startTransaction(Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> t) {
        RoomTransactions.SendPaymentTransaction tx = new RoomTransactions.SendPaymentTransaction();
        tx.setTxData(RoomConverters.TxConverter.fromTx(t));
        tx.setRequest(t.request);
        tx.setResponse(t.response);
        dao_.createTransaction(tx);
    }

    @Override
    public WalletData.Payment commitTransaction(int txUserId, String txId, int txAuthUserId, WalletData.Payment p) {
        return dao_.commitTransaction(txUserId, txId, txAuthUserId, p, System.currentTimeMillis());
    }

    @Override
    public void rejectTransaction(int txUserId, String txId, int txAuthUserId) {
        dao_.rejectTransaction(txUserId, txId, txAuthUserId, Transaction.TX_STATE_REJECTED, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(int txUserId, String txId) {
        dao_.timeoutTransaction(txUserId, txId, Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis());
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class SendPaymentDaoRoom {

    private RouteHintsDaoRoom routeDao_;

    void setRouteDao(RouteHintsDaoRoom routeDao) {
        routeDao_ = routeDao;
    }

    @Query("SELECT identityPubkey FROM WalletInfo LIMIT 1")
    public abstract String walletPubkey();

    @Query("SELECT * FROM Contact WHERE id = :id")
    public abstract RoomData.Contact getContactRoom(long id);

    public WalletData.Contact getContact(long contactId) {
        RoomData.Contact rc = getContactRoom(contactId);
        if (rc == null)
            return null;

        return rc.getData().toBuilder()
                .setRouteHints(routeDao_.getRouteHints(RoomData.routeHintsParentId(rc.getData())))
                .build();
    }

    @Query("SELECT id FROM ContactPaymentsPrivilege WHERE userId = :userId AND contactId = :contactId")
    abstract boolean hasContactPaymentsPrivilege(int userId, long contactId);

    @Query("SELECT * FROM SendPaymentTransaction WHERE txState = 0")
    public abstract List<RoomTransactions.SendPaymentTransaction> getTransactionsRoom();

    @Query("SELECT * FROM SendPaymentTransaction WHERE txUserId = :txUserId AND txId = :txId")
    public abstract RoomTransactions.SendPaymentTransaction getTransactionRoom(int txUserId, String txId);

    @Insert
    public abstract void createTransaction(RoomTransactions.SendPaymentTransaction tx);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateTransaction(RoomTransactions.SendPaymentTransaction tx);

    @Query("UPDATE SendPaymentTransaction " +
            "SET txState = :txState, txDoneTime = :time, txAuthTime = :time, txAuthUserId = :txAuthUserId " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void rejectTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);

    @Query("UPDATE SendPaymentTransaction " +
            "SET txState = :txState, txDoneTime = :time " +
            "WHERE txUserId = :txUserId AND txId = :txId")
    public abstract void timeoutTransaction(int txUserId, String txId, int txState, long time);

    @Insert
    public abstract long insertSendPayment(RoomData.SendPayment i);
    @Query("UPDATE SendPayment SET id = id_ WHERE id_ = :id")
    abstract void setSendPaymentId(long id);

    @Insert
    public abstract long insertPayment(RoomData.Payment p);
    @Query("UPDATE Payment SET id = id_ WHERE id_ = :id")
    abstract void setPaymentId(long id);

    void readRouteHints(RoomTransactions.SendPaymentTransaction tx) {
        ImmutableList<WalletData.RouteHint> routeHints = routeDao_.getRouteHints(routeHintsParentId(tx));
        tx.request = tx.request.toBuilder().setRouteHints(routeHints).build();
    }

    @androidx.room.Transaction
    public List<RoomTransactions.SendPaymentTransaction> getTransactions() {
        List<RoomTransactions.SendPaymentTransaction> txs = getTransactionsRoom();
        for (RoomTransactions.SendPaymentTransaction tx: txs)
            readRouteHints(tx);
        return txs;
    }

    public RoomTransactions.SendPaymentTransaction getTransaction(int txUserId, String txId) {
        RoomTransactions.SendPaymentTransaction tx = getTransactionRoom(txUserId, txId);
        if (tx != null)
            readRouteHints(tx);
        return tx;
    }

    static String routeHintsParentId(RoomTransactions.SendPaymentTransaction tx) {
        return "spr:"+tx.txData.txId;
    }


    @androidx.room.Transaction
    public void startTransaction(RoomTransactions.SendPaymentTransaction tx) {
        createTransaction(tx);

        // write route hints
        routeDao_.upsertRouteHints(routeHintsParentId(tx), tx.request.routeHints());
    }

    @androidx.room.Transaction
    public WalletData.Payment commitTransaction(int txUserId, String txId, int txAuthUserId,
                                                WalletData.Payment payment, long time) {

        // get tx
        RoomTransactions.SendPaymentTransaction tx = getTransaction(txUserId, txId);

        // get sendpayment to be written
        WalletData.SendPayment sp = payment.sendPayments().get(0L);

        // insert sendpayment
        RoomData.SendPayment rsp = new RoomData.SendPayment();
        rsp.setData(sp);

        final long id = insertSendPayment(rsp);
        setSendPaymentId(id);

        // create new SP w/ proper id
        sp = sp.toBuilder().setId(id).build();

        // write route hints
        routeDao_.upsertRouteHints(RoomData.routeHintsParentId(sp), sp.routeHints());

        // set payment source id
        payment = payment.toBuilder()
                .setSourceId(id)
                .build();

        // insert payment
        RoomData.Payment rp = new RoomData.Payment();
        rp.setData(payment);

        final long pid = insertPayment(rp);
        setPaymentId(pid);

        // update state
        tx.setResponse(sp);
        tx.txData.txState = org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED;
        tx.txData.txDoneTime = time;
        if (txAuthUserId != 0) {
            tx.txData.txAuthUserId = txAuthUserId;
            tx.txData.txAuthTime = time;
        }

        // write tx
        updateTransaction(tx);

        // create list of SP w/ proper id
        ImmutableMap.Builder<Long,WalletData.SendPayment> spb = ImmutableMap.builder();
        spb.put(id, sp);

        // create new Payment object w/ proper ids set
        payment = payment.toBuilder()
                .setId(pid)
                .setSendPayments(spb.build())
                .build();

        return payment;
    }
}