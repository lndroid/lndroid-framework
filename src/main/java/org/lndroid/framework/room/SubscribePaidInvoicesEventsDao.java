package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.SubscribePaidInvoicesEvents;

import java.util.ArrayList;
import java.util.List;

public class SubscribePaidInvoicesEventsDao implements SubscribePaidInvoicesEvents.IDao, IPluginDao {

    private DaoRoom dao_;

    SubscribePaidInvoicesEventsDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public List<WalletData.Invoice> getNewPaidInvoices() {
        // last 24h only
        List<RoomData.Invoice> rs = dao_.getNewPaidInvoices(System.currentTimeMillis() - 86400000);
        List<WalletData.Invoice> list = new ArrayList<>();
        for(RoomData.Invoice r: rs)
            list.add(r.getData());
        return list;
    }

    @Dao
    abstract static class DaoRoom {
        @Query("SELECT * FROM Invoice WHERE settleTime > :minSettleTime AND notifyTime = 0")
        abstract List<RoomData.Invoice> getNewPaidInvoices(long minSettleTime);
    }
}

