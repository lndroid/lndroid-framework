package org.lndroid.framework.room;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IAuthRequestDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetAuthRequestUser;

class AuthRequestDao implements
        IAuthRequestDao, IPluginDao,
        GetAuthRequestUser.IDao
{

    private DaoRoom dao_;

    AuthRequestDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public WalletData.AuthRequest get(long id) {
        RoomData.AuthRequest r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public WalletData.AuthRequest get(long userId, String txId) {
        RoomData.AuthRequest r = dao_.get(userId, txId);
        return r != null ? r.getData() : null;
    }

    @Nullable
    @Override
    public WalletData.User getAuthRequestUser(long authRequestId) {
        RoomData.User r = dao_.getAuthRequestUser(authRequestId);
        return r != null ? r.getData() : null;
    }

    @Override
    public WalletData.AuthRequest insert(WalletData.AuthRequest r) {
        RoomData.AuthRequest d = new RoomData.AuthRequest();
        d.setData(r);
        dao_.insert(d);
        return r;
    }

    @Override
    public void delete(long id) {
        dao_.delete(id);
    }

    @Override
    public void deleteBackgroundRequests() {
        dao_.deleteBackgroundRequests();
    }

    @Override
    public List<WalletData.AuthRequest> getBackgroundRequests() {
        List<WalletData.AuthRequest> r = new ArrayList<>();
        for (RoomData.AuthRequest ar: dao_.getBackgroundRequests())
            r.add(ar.getData());
        return r;
    }

    @Dao
    abstract static class DaoRoom {
        @Query("SELECT * FROM AuthRequest WHERE id = :id")
        abstract RoomData.AuthRequest get(long id);

        @Query("SELECT * FROM AuthRequest WHERE userId = :userId AND txId = :txId")
        abstract RoomData.AuthRequest get(long userId, String txId);

        @Query("SELECT * FROM User WHERE id = (SELECT userId FROM AuthRequest WHERE id = :authRequestId)")
        abstract RoomData.User getAuthRequestUser(long authRequestId);

        @Insert
        abstract void insert(RoomData.AuthRequest r);

        @Query("DELETE FROM AuthRequest WHERE id = :id")
        abstract void delete(long id);

        @Query("DELETE FROM AuthRequest WHERE background != 0")
        abstract void deleteBackgroundRequests();

        @Query("SELECT * FROM AuthRequest WHERE background != 0")
        abstract List<RoomData.AuthRequest> getBackgroundRequests();
    }

}

