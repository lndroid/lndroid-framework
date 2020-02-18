package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.engine.IPluginDao;

public class GetSendPaymentDao implements IGetDao<WalletData.SendPayment>, IPluginDao {

    private GetSendPaymentDaoRoom dao_;

    GetSendPaymentDao(GetSendPaymentDaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.SendPayment get(long id) {
        RoomData.SendPayment r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
interface GetSendPaymentDaoRoom {
    @Query("SELECT * FROM SendPayment WHERE id = :id")
    RoomData.SendPayment get(long id);
}
