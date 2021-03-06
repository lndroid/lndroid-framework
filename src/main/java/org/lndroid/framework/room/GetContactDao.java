package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetContact;

public class GetContactDao implements
        IGetDao<WalletData.Contact>, IPluginDao,
        GetContact.IDao
{
    private DaoRoom dao_;

    GetContactDao(DaoRoom dao, RouteHintsDaoRoom routeDao) {
        dao_ = dao;
        dao_.setRouteDao(routeDao);
    }

    @Override
    public WalletData.Contact get(long id) {
        return dao_.get(id);
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public boolean hasPrivilege(WalletDataDecl.GetRequestTmpl<Long> req, WalletData.User user) {
        return dao_.hasListContactsPrivilege(user.id());
    }


    @Dao
    abstract static class DaoRoom {
        private RouteHintsDaoRoom routeDao_;

        void setRouteDao(RouteHintsDaoRoom routeDao) {
            routeDao_ = routeDao;
        }

        @Query("SELECT * FROM Contact WHERE id = :id")
        abstract RoomData.Contact getContact(long id);

        @Query("SELECT id FROM ListContactsPrivilege WHERE userId = :userId")
        abstract boolean hasListContactsPrivilege(long userId);

        @Transaction
        WalletData.Contact get(long id) {
            RoomData.Contact rc = getContact(id);
            if (rc == null)
                return null;

            return rc.getData().toBuilder()
                    .setRouteHints(routeDao_.getRouteHints(RoomData.routeHintsParentId(rc.getData())))
                    .build();
        }

    }

}
