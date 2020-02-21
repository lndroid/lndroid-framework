package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetUtxo;

public class GetUtxoDao implements
        IGetDao<WalletData.Utxo>, IPluginDao,
        GetUtxo.IDao
{
    private DaoRoom dao_;

    GetUtxoDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Utxo get(long id) {
        RoomData.Utxo r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM Utxo WHERE id = :id")
        RoomData.Utxo get(long id);
    }
}

