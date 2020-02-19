package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;

public class GetUtxoDao implements IGetDao<WalletData.Utxo>, IPluginDao {

    private Room dao_;

    GetUtxoDao(Room dao) {
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
    interface Room {
        @Query("SELECT * FROM Utxo WHERE id = :id")
        RoomData.Utxo get(long id);
    }
}

