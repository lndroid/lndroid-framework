package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.SendPayment;

public class SendPaymentDao
        extends ActionDaoBase<WalletData.SendPaymentRequest, WalletData.SendPayment>
        implements SendPayment.IDao
{
    public static final String PLUGIN_ID = DefaultPlugins.SEND_PAYMENT;

    private DaoRoom dao_;

    SendPaymentDao(DaoRoom dao, RoomTransactions.TransactionDao txDao, RouteHintsDaoRoom routeDao) {
        super(dao);
        dao.init(PLUGIN_ID, txDao);
        dao.routeDao = routeDao;
        dao_ = dao;
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
    public WalletData.Payment commitTransaction(long txUserId, String txId, long txAuthUserId, WalletData.Payment p,
                                                int maxTries, long maxTryTime) {
        return dao_.commitTransaction(txUserId, txId, txAuthUserId, p, System.currentTimeMillis(),
                maxTries, maxTryTime);
    }

    @Override
    public WalletData.SendPayment commitTransaction(long txUserId, String txId, long txAuthUserId, WalletData.SendPayment p) {
        throw new RuntimeException("Unsupported method");
    }

    @Override
    public WalletData.SendPayment commitTransaction(long txUserId, String txId, long txAuthUserId, WalletData.SendPayment r, int maxTries, long maxTryTime) {
        throw new RuntimeException("Unsupported method");
    }

    @Dao
    abstract static class DaoRoom extends RoomJobDaoBase<WalletData.SendPaymentRequest, WalletData.SendPayment>{

        RouteHintsDaoRoom routeDao;

        @Query("SELECT identityPubkey FROM WalletInfo LIMIT 1")
        public abstract String walletPubkey();

        @Query("SELECT * FROM Contact WHERE id = :id")
        public abstract RoomData.Contact getContactRoom(long id);

        public WalletData.Contact getContact(long contactId) {
            RoomData.Contact rc = getContactRoom(contactId);
            if (rc == null)
                return null;

            return rc.getData().toBuilder()
                    .setRouteHints(routeDao.getRouteHints(RoomData.routeHintsParentId(rc.getData())))
                    .build();
        }

        @Query("SELECT id FROM ContactPaymentsPrivilege WHERE userId = :userId AND contactId = :contactId")
        abstract boolean hasContactPaymentsPrivilege(long userId, long contactId);


        @Override @androidx.room.Transaction
        public WalletData.SendPayment commitTransaction(
                long userId, String txId, long txAuthUserId, WalletData.SendPayment r, long time) {
            throw new RuntimeException("Unsupported method");
        }

        @Override @androidx.room.Transaction
        public void createTransaction(RoomTransactions.RoomTransaction tx, WalletData.SendPaymentRequest req){
            createTransactionImpl(tx, req);
        }

        private String routeHintsRequestParentId(long id) {
            return "SendPaymentRequest:"+id;
        }

        @Insert
        abstract long insertRequest(RoomTransactions.SendPaymentRequest i);

        @Override
        protected long insertRequest(WalletData.SendPaymentRequest req) {
            // convert request to room object
            RoomTransactions.SendPaymentRequest r = new RoomTransactions.SendPaymentRequest();
            r.data = req;

            // insert request
            final long id = insertRequest(r);

            // write route hints
            routeDao.upsertRouteHints(routeHintsRequestParentId(id), req.routeHints());

            return id;
        }

        WalletData.SendPaymentRequest addRouteHintsRequest(long id, WalletData.SendPaymentRequest c) {
            ImmutableList<WalletData.RouteHint> routeHints = routeDao.getRouteHints(
                    routeHintsRequestParentId(id));
            return c.toBuilder().setRouteHints(routeHints).build();
        }

        WalletData.SendPayment addRouteHintsResponse(WalletData.SendPayment c) {
            ImmutableList<WalletData.RouteHint> routeHints = routeDao.getRouteHints(
                    RoomData.routeHintsParentId(c));
            return c.toBuilder().setRouteHints(routeHints).build();
        }

        @Query("SELECT * FROM txSendPaymentRequest WHERE id_ = :id")
        abstract RoomTransactions.SendPaymentRequest getRequestRoom(long id);

        @Override
        public WalletData.SendPaymentRequest getRequest(long id) {
            RoomTransactions.SendPaymentRequest r = getRequestRoom(id);
            if (r == null)
                return null;

            return addRouteHintsRequest(id, r.data);
        }

        @Query("SELECT * FROM SendPayment WHERE id = :id")
        abstract RoomData.SendPayment getResponseRoom(long id);

        @Override
        public WalletData.SendPayment getResponse(long id) {
            RoomData.SendPayment r = getResponseRoom(id);
            if (r == null)
                return null;

            return addRouteHintsResponse(r.getData());
        }

        @Insert
        public abstract void insertSendPayment(RoomData.SendPayment i);

        @Insert
        public abstract long insertPayment(RoomData.Payment p);

        @Override
        protected long insertResponse(WalletData.SendPayment v) {
            RoomData.SendPayment r = new RoomData.SendPayment();
            r.setData(v);
            insertSendPayment(r);
            return v.id();
        }

        @androidx.room.Transaction
        public WalletData.Payment commitTransaction(
                long txUserId, String txId, long txAuthUserId,
                WalletData.Payment payment, long time,
                int maxTries, long maxTryTime) {

            // get sendpayment to be written
            WalletData.SendPayment sp = payment.sendPayments().get(payment.sourceId());

            // insert sendpayment
            insertResponse(sp);

            // write route hints
            routeDao.upsertRouteHints(RoomData.routeHintsParentId(sp), sp.routeHints());

            // insert payment
            RoomData.Payment rp = new RoomData.Payment();
            rp.setData(payment);
            insertPayment(rp);

            // update tx state: confirm and commit
            txDao().initTransactionJob(PLUGIN_ID, txUserId, txId, maxTries, maxTryTime);
            txDao().confirmTransaction(PLUGIN_ID, txUserId, txId, txAuthUserId, time);
            txDao().commitTransaction(PLUGIN_ID, txUserId, txId,
                    org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED,
                    time, sp.getClass().getName(), sp.id());

            return payment;
        }

        public WalletData.SendPayment commitTransaction(
                long txUserId, String txId, long txAuthUserId, WalletData.SendPayment r, long time,
                int maxTries, long maxTryTime) {
            throw new RuntimeException("Unsupported method");
        }
    }
}
