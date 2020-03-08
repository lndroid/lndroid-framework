package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.GetPaymentPeerContact;

public class GetPaymentPeerContactDao implements
        IGetDao<WalletData.Contact>, IPluginDao,
        GetPaymentPeerContact.IDao
{
    private DaoRoom dao_;

    GetPaymentPeerContactDao(DaoRoom dao, RouteHintsDaoRoom routeDao) {
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

    @Dao
    abstract static class DaoRoom {
        private RouteHintsDaoRoom routeDao_;

        void setRouteDao(RouteHintsDaoRoom routeDao) {
            routeDao_ = routeDao;
        }

        @Query("SELECT * FROM Contact WHERE pubkey = (SELECT peerPubkey FROM Payment WHERE id = :paymentId)")
        abstract RoomData.Contact getContact(long paymentId);

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

