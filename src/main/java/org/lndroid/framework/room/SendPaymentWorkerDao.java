package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ISendPaymentWorkerDao;
import org.lndroid.framework.engine.IPluginDao;

public class SendPaymentWorkerDao implements ISendPaymentWorkerDao, IPluginDao {
    private SendPaymentWorkerDaoRoom dao_;

    SendPaymentWorkerDao(SendPaymentWorkerDaoRoom dao, RouteHintsDaoRoom routeDao) {
        dao_ = dao;
        dao_.setRouteDao(routeDao);
    }

    @Override
    public List<WalletData.SendPayment> getSendingPayments() {
        return dao_.getPayments(WalletData.SEND_PAYMENT_STATE_SENDING);
    }

    @Override
    public List<WalletData.SendPayment> getPendingPayments(long now) {
        return dao_.getRetryPayments(WalletData.SEND_PAYMENT_STATE_PENDING, now);
    }

    @Override
    public WalletData.Contact getContact(String contactPubkey) {
        return dao_.getContact(contactPubkey);
    }

    @Override
    public void updatePayment(WalletData.SendPayment p) {
        RoomData.SendPayment rp = new RoomData.SendPayment();
        rp.setData(p);
        dao_.updatePayment(rp);
    }

    @Override
    public void settlePayment(WalletData.SendPayment sp, WalletData.HTLCAttempt htlc) {
        RoomData.SendPayment rsp = new RoomData.SendPayment();
        rsp.setData(sp);
        RoomData.HTLCAttempt rhtlc = new RoomData.HTLCAttempt();
        rhtlc.setData(htlc);
        dao_.settlePayment(rsp, rhtlc);
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class SendPaymentWorkerDaoRoom {

    private RouteHintsDaoRoom routeDao_;

    void setRouteDao(RouteHintsDaoRoom routeDao) {
        routeDao_ = routeDao;
    }

    @Query("SELECT * FROM Contact WHERE pubkey = :pubkey")
    public abstract RoomData.Contact getContactRoom(String pubkey);

    public WalletData.Contact getContact(String contactPubkey) {
        RoomData.Contact rc = getContactRoom(contactPubkey);
        if (rc == null)
            return null;

        return rc.getData().toBuilder()
                .setRouteHints(routeDao_.getRouteHints(RoomData.routeHintsParentId(rc.getData())))
                .build();
    }

    @Query("SELECT * FROM SendPayment WHERE state = :state")
    abstract List<RoomData.SendPayment> getPaymentsRoom(int state);

    @Query("SELECT * FROM SendPayment WHERE state = :state and nextTryTime <= :now")
    abstract List<RoomData.SendPayment> getRetryPaymentsRoom(int state, long now);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void updateSendPayment(RoomData.SendPayment p);

    @Query("UPDATE Payment SET peerPubkey = :peerPubkey WHERE type = :type AND sourceId = :sourceId")
    abstract void setPaymentPeer(int type, long sourceId, String peerPubkey);

    @Query("UPDATE Payment SET sourceHTLCId = :htlcId WHERE type = :type AND sourceId = :sourceId")
    abstract void setPaymentHTLC(int type, long sourceId, long htlcId);

    @Insert
    abstract long insertHTLC(RoomData.HTLCAttempt htlc);

    private List<WalletData.SendPayment> fromRoom(List<RoomData.SendPayment> rsps) {
        List<WalletData.SendPayment> sps = new ArrayList<>();
        for (RoomData.SendPayment r: rsps) {
            WalletData.SendPayment sp = r.getData();
            sps.add (sp.toBuilder()
                    .setRouteHints(routeDao_.getRouteHints(RoomData.routeHintsParentId(sp)))
                    .build());
        }
        return sps;
    }

    @Transaction
    public List<WalletData.SendPayment> getPayments(int state) {
        return fromRoom(getPaymentsRoom(state));
    }

    @Transaction
    public List<WalletData.SendPayment> getRetryPayments(int state, long now) {
        return fromRoom(getRetryPaymentsRoom(state, now));
    }

    @Transaction
    public void updatePayment(RoomData.SendPayment sp) {
        updateSendPayment(sp);
        setPaymentPeer(WalletData.PAYMENT_TYPE_SENDPAYMENT, sp.getData().id(), sp.getData().destPubkey());
    }

    @Transaction
    public void settlePayment(RoomData.SendPayment sp, RoomData.HTLCAttempt htlc) {
        updateSendPayment(sp);
        final long htlcId = insertHTLC(htlc);
        setPaymentHTLC(WalletData.PAYMENT_TYPE_SENDPAYMENT, sp.getData().id(), htlcId);
    }
}

