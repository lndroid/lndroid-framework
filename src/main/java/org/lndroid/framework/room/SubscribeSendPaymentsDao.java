package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.SubscribeSendPayments;

public class SubscribeSendPaymentsDao implements SubscribeSendPayments.IDao, IPluginDao {

    private DaoRoom dao_;

    SubscribeSendPaymentsDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.SendPayment getPayment(long id) {
        RoomData.SendPayment p = dao_.getPayment(id);
        return p != null ? p.getData() : null;
    }

    @Override
    public List<WalletData.SendPayment> getActivePayments(long userId) {
        final int[] ACTIVE_STATES = {
                WalletData.SEND_PAYMENT_STATE_PENDING,
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

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM SendPayment WHERE id = :id")
        RoomData.SendPayment getPayment(long id);

        @Query("SELECT * FROM SendPayment WHERE state IN(:states)")
        List<RoomData.SendPayment> getPayments(int[] states);

        @Query("SELECT * FROM SendPayment WHERE userId = :userId AND state IN(:states)")
        List<RoomData.SendPayment> getPayments(int[] states, long userId);
    }
}
