package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ISubscribeSendPaymentsDao;
import org.lndroid.framework.engine.IPluginDao;

public class SubscribeSendPaymentsDao implements ISubscribeSendPaymentsDao, IPluginDao {

    private SubscribeSendPaymentsDaoRoom dao_;

    SubscribeSendPaymentsDao(SubscribeSendPaymentsDaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.SendPayment getPayment(long id) {
        RoomData.SendPayment p = dao_.getPayment(id);
        return p != null ? p.getData() : null;
    }

    @Override
    public List<WalletData.SendPayment> getActivePayments(int userId) {
        final int[] ACTIVE_STATES = {
                WalletData.SEND_PAYMENT_STATE_PENDING,
                WalletData.SEND_PAYMENT_STATE_SENDING
        };

        List<RoomData.SendPayment> rps = (userId != 0)
                ? dao_.getPayments(ACTIVE_STATES, userId)
                : dao_.getPayments(ACTIVE_STATES);
        List<WalletData.SendPayment> ps = new ArrayList<>();
        for(RoomData.SendPayment rp: rps)
            ps.add(rp.getData());

        return ps;
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
interface SubscribeSendPaymentsDaoRoom {
    @Query("SELECT * FROM SendPayment WHERE id_ = :id")
    RoomData.SendPayment getPayment(long id);

    @Query("SELECT * FROM SendPayment WHERE state IN(:states)")
    List<RoomData.SendPayment> getPayments(int[] states);

    @Query("SELECT * FROM SendPayment WHERE userId = :userId AND state IN(:states)")
    List<RoomData.SendPayment> getPayments(int[] states, int userId);
}