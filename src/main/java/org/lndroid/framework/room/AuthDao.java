package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IAuthDao;
import org.lndroid.framework.engine.IPluginDao;

class AuthDao implements IAuthDao, IPluginDao {

    private AuthDaoRoom dao_;

    AuthDao(AuthDaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public WalletData.User get(long id) {
        RoomData.User r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public WalletData.User getByAppPubkey(String pk) {
        RoomData.User r = dao_.getByAppPubkey(pk);
        return r != null ? r.getData() : null;
    }
}

@Dao
interface AuthDaoRoom {
    @Query("SELECT * FROM User WHERE id = :id")
    RoomData.User get(long id);
    @Query("SELECT * FROM User WHERE appPubkey = :pk")
    RoomData.User getByAppPubkey(String pk);
}

