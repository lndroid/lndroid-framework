package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IUtxoWorkerDao;
import org.lndroid.framework.engine.IPluginDao;

public class UtxoWorkerDao implements IUtxoWorkerDao, IPluginDao {

    private Room dao_;

    UtxoWorkerDao(Room dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Utxo getByOutpoint(String txidHex, int outputIndex) {
        RoomData.Utxo r = dao_.getByOutpoint(txidHex, outputIndex);
        return (r != null) ? r.getData() : null;
    }

    @Override
    public void update(WalletData.Utxo utxo) {
        RoomData.Utxo r = new RoomData.Utxo();
        r.setData(utxo);
        dao_.update(r);
    }

    @Override
    public void init() {

    }

    @Dao
    interface Room {
        @Query("SELECT * FROM Utxo WHERE txidHex = :txidHex AND outputIndex = :outputIndex")
        RoomData.Utxo getByOutpoint(String txidHex, int outputIndex);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void update(RoomData.Utxo b);
    }

}

