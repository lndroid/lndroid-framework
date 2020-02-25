package org.lndroid.framework.room;

import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.dao.IJobDao;
import org.lndroid.framework.engine.IPluginDao;

class JobDaoBase<Request, Response>
        extends ActionDaoBase<Request, Response>
        implements IJobDao<Request, Response>
{
    private RoomJobDaoBase<Request, Response> dao_;

    JobDaoBase(RoomJobDaoBase<Request, Response> dao) {
        super(dao);
        dao_ = dao;
    }

    @Override
    public Response commitTransaction(long txUserId, String txId, long txAuthUserId, Response r,
                                      int maxTries, long maxTryTime) {

        return dao_.commitTransaction(txUserId, txId, txAuthUserId, r, System.currentTimeMillis(),
                maxTries, maxTryTime);
    }
}
