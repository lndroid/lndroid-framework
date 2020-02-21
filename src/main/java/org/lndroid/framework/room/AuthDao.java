package org.lndroid.framework.room;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RoomWarnings;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IAuthDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetAppUser;
import org.lndroid.framework.plugins.GetUser;

class AuthDao implements
        IAuthDao, IPluginDao,
        GetUser.IDao,
        GetAppUser.IDao
{

    private DaoRoom dao_;

    AuthDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    @Nullable
    @Override
    public WalletData.User get(long id) {
        RoomData.User r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Nullable
    @Override
    public WalletData.User getByAppPubkey(String pk) {
        RoomData.User r = dao_.getByAppPubkey(pk);
        return r != null ? r.getData() : null;
    }

    @Nullable
    @Override
    public WalletData.User getAuthInfo(long id) {
        RoomData.User r = dao_.getAuthInfo(id);
        if (r == null)
            return null;

        // workaround Room's requirement to return all primitive fields
        return r.getData().toBuilder()
                .setAuthUserId(0)
                .setCreateTime(0)
                .build();
    }

    @Dao
    interface DaoRoom {
        @Query("SELECT * FROM User WHERE id = :id")
        RoomData.User get(long id);
        @Query("SELECT * FROM User WHERE appPubkey = :pk")
        RoomData.User getByAppPubkey(String pk);

        @Query("SELECT id_, id, authUserId, createTime, authType, nonce, pubkey FROM User WHERE id = :id")
        @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
        RoomData.User getAuthInfo(long id);
    }

}

