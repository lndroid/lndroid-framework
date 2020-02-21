package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetSendPayment;

public class GetSendPaymentDao implements
        IGetDao<WalletData.SendPayment>, IPluginDao,
        GetSendPayment.IDao
{
    private DaoRoom dao_;

    GetSendPaymentDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.SendPayment get(long id) {
        RoomData.SendPayment r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM SendPayment WHERE id = :id")
        RoomData.SendPayment get(long id);
    }
}
