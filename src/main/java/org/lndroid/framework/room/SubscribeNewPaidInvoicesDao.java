package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.google.common.collect.ImmutableMap;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.SubscribeNewPaidInvoices;
import org.lndroid.framework.plugins.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscribeNewPaidInvoicesDao implements SubscribeNewPaidInvoices.IDao, IPluginDao {

    private DaoRoom dao_;

    SubscribeNewPaidInvoicesDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        dao_ = dao;
        dao_.txDao = txDao;
    }

    @Override
    public WalletData.Payment getPayment(String protocol, long invoiceId) {
        // FIXME only messages are supported atm
        if (!WalletData.PROTOCOL_MESSAGES.equals(protocol))
            return null;

        return dao_.getMessagePayment(invoiceId);
    }

    @Override
    public List<WalletData.Payment> getPayments(String protocol) {
        if (!WalletData.PROTOCOL_MESSAGES.equals(protocol))
            return new ArrayList<>();

        return dao_.getMessagePayments();
    }

    private Transaction<WalletData.SubscribeNewPaidInvoicesRequest> fromRoom(RoomTransactions.RoomTransaction tx) {
        Transaction<WalletData.SubscribeNewPaidInvoicesRequest> t = new Transaction<>();
        t.job = tx.jobData;
        t.tx = tx.txData;
        t.request = dao_.getRequest(tx.txData.requestId);
        return t;
    }

    @Override
    public List<Transaction<WalletData.SubscribeNewPaidInvoicesRequest>> getTransactions() {
        List<Transaction<WalletData.SubscribeNewPaidInvoicesRequest>> r = new ArrayList<>();

        List<RoomTransactions.RoomTransaction> txs = dao_.getTransactions();
        for (RoomTransactions.RoomTransaction tx: txs) {
            r.add(fromRoom(tx));
        }

        return r;
    }

    @Override
    public void startTransaction(Transaction<WalletData.SubscribeNewPaidInvoicesRequest> t) {
        RoomTransactions.RoomTransaction tx = new RoomTransactions.RoomTransaction();
        tx.txData = t.tx;
        tx.jobData = t.job;
        dao_.upsertTransaction(tx, t.request);
    }

    @Override
    public void rejectTransaction(long txUserId, String txId, long txAuthUserId) {
        dao_.rejectTransaction(txUserId, txId, txAuthUserId, System.currentTimeMillis());
    }

    @Override
    public void timeoutTransaction(long txUserId, String txId) {
        dao_.failTransaction(txUserId, txId,
                Transaction.TX_STATE_TIMEDOUT, System.currentTimeMillis(),
                Errors.TX_TIMEOUT, Errors.errorMessage(Errors.TX_TIMEOUT));
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {

        static final String PLUGIN_ID = DefaultPlugins.SUBSCRIBE_NEW_PAID_INVOICES;

        RoomTransactions.TransactionDao txDao;

        @Query("SELECT * FROM Payment WHERE message IS NOT NULL AND type = :type AND sourceId = :invoiceId")
        abstract RoomData.Payment getMessagePayment(int type, long invoiceId);

        @Query("SELECT * FROM Invoice WHERE id = :id")
        abstract RoomData.Invoice getInvoice(long id);

        @androidx.room.Transaction
        WalletData.Payment getMessagePayment(long invoiceId){
            RoomData.Payment p = getMessagePayment(WalletData.PAYMENT_TYPE_INVOICE, invoiceId);
            if (p == null)
                return null;

            RoomData.Invoice i = getInvoice(invoiceId);
            if (i == null)
                return null;

            ImmutableMap.Builder<Long, WalletData.Invoice> ib = ImmutableMap.builder();
            ib.put(i.getData().id(), i.getData());
            return p.getData().toBuilder()
                    .setInvoices(ib.build())
                    .build();
        }

        @Query("SELECT * FROM Payment WHERE type = :type AND sourceId IN (:invoiceIds)")
        abstract List<RoomData.Payment> getPayments(int type, List<Long> invoiceIds);

        @Query("SELECT * FROM Invoice WHERE message IS NOT NULL AND settleTime > :minTime AND notifyTime = 0 "+
                "AND isKeysend != 0")
        abstract List<RoomData.Invoice> getMessageInvoices(long minTime);

        @androidx.room.Transaction
        List<WalletData.Payment> getMessagePayments() {
            List<RoomData.Invoice> ins = getMessageInvoices(System.currentTimeMillis() - 86400000);
            Map<Long, WalletData.Invoice> invoices = new HashMap<>();
            List<Long> invoiceIds = new ArrayList<>();
            for(RoomData.Invoice i: ins) {
                invoices.put(i.getData().id(), i.getData());
                invoiceIds.add(i.getData().id());
            }

            List<RoomData.Payment> ps = getPayments(WalletData.PAYMENT_TYPE_INVOICE, invoiceIds);
            List<WalletData.Payment> payments = new ArrayList<>();
            for(RoomData.Payment rp: ps) {
                WalletData.Payment p = rp.getData();
                WalletData.Invoice i = invoices.get(p.sourceId());
                ImmutableMap.Builder<Long, WalletData.Invoice> ib = ImmutableMap.builder();
                ib.put(i.id(), i);
                payments.add(p.toBuilder().setInvoices(ib.build()).build());
            }

            return payments;
        }

        @Insert
        abstract long insertRequest(RoomTransactions.SubscribeNewPaidInvoicesRequest i);

        @Query("SELECT * FROM txSubscribeNewPaidInvoicesRequest WHERE id_ = :id")
        abstract RoomTransactions.SubscribeNewPaidInvoicesRequest getRequestRoom(long id);

        public WalletData.SubscribeNewPaidInvoicesRequest getRequest(long id) {
            RoomTransactions.SubscribeNewPaidInvoicesRequest r = getRequestRoom(id);
            return r != null ? r.data : null;
        }

        // we need this to be an atomic db tx, so each Room dao wraps this w/ @Transaction
        @androidx.room.Transaction
        void upsertTransaction(
                RoomTransactions.RoomTransaction tx, WalletData.SubscribeNewPaidInvoicesRequest req){

            RoomTransactions.SubscribeNewPaidInvoicesRequest r =
                    new RoomTransactions.SubscribeNewPaidInvoicesRequest();
            r.data = req;

            // link tx to request
            tx.txData.requestClass = req.getClass().getName();
            tx.txData.requestId = insertRequest(r);

            // upsert
            txDao.updateTransaction(tx);

/*            RoomTransactions.RoomTransaction existing = txDao.getTransaction(
                    tx.txData.pluginId, tx.txData.userId, tx.txData.txId);
            if (existing == null) {

                // ensure transaction
                txDao.createTransaction(tx);
            } else {

            }
 */
        }

        public List<RoomTransactions.RoomTransaction> getTransactions() {
            return txDao.getTransactions(PLUGIN_ID);
        }

        public void rejectTransaction(long userId, String txId, long authUserId, long time) {
            txDao.rejectTransaction(PLUGIN_ID, userId, txId, authUserId,
                    Transaction.TX_STATE_REJECTED, time);
        }

        public void failTransaction(long userId, String txId,
                                    int state, long time, String errorCode, String errorMessage){
            txDao.failTransaction(PLUGIN_ID, userId, txId, state, time, errorCode, errorMessage);
        }
    }
}

