package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetInvoice;

public class GetInvoiceDao implements
        IGetDao<WalletData.Invoice>, IPluginDao,
        GetInvoice.IDao
{
    private DaoRoom dao_;

    GetInvoiceDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Invoice get(long id) {
        RoomData.Invoice r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM Invoice WHERE id = :id")
        RoomData.Invoice get(long id);
    }
}
