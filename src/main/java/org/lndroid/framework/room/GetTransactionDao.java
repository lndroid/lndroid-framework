package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;

public class GetTransactionDao implements IGetDao<WalletData.Transaction>, IPluginDao {

    private GetTransactionDaoRoom dao_;

    GetTransactionDao(GetTransactionDaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Transaction get(long id) {
        RoomData.Transaction r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
interface GetTransactionDaoRoom {
    @Query("SELECT * FROM 'Transaction' WHERE id = :id")
    RoomData.Transaction get(long id);
}
