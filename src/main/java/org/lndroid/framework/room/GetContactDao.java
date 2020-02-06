package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetContactDao;
import org.lndroid.framework.engine.IPluginDao;

public class GetContactDao implements IGetContactDao, IPluginDao {
    private GetContactDaoRoom dao_;

    GetContactDao(GetContactDaoRoom dao, RouteHintsDaoRoom routeDao) {
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
}

@Dao
abstract class GetContactDaoRoom {
    private RouteHintsDaoRoom routeDao_;

    void setRouteDao(RouteHintsDaoRoom routeDao) {
        routeDao_ = routeDao;
    }

    @Query("SELECT * FROM Contact WHERE id_ = :id")
    abstract RoomData.Contact getContact(long id);

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
