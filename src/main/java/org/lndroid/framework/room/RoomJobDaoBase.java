package org.lndroid.framework.room;

import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.plugins.Transaction;

abstract class RoomJobDaoBase<Request, Response> extends RoomActionDaoBase<Request, Response>{


    public Response commitTransaction(
            long userId, String txId, long txAuthUserId, Response r, long time,
            IActionDao.OnResponseMerge<Response> merger) {
        throw new RuntimeException("Unsupported method");
    }

    protected Response commitTransactionImpl(
            long userId, String txId, long txAuthUserId, Response r, long time,
            int maxTries, long maxTryTime,
            IActionDao.OnResponseMerge<Response> merger)
    {
        r = mergeExisting(r, merger);

        final long id = insertResponse(r);

        // update tx state: confirm and commit
        txDao().initTransactionJob(pluginId(), userId, txId,
                Transaction.JOB_STATE_NEW, maxTries, maxTryTime);
        txDao().confirmTransaction(pluginId(), userId, txId, txAuthUserId, time);
        txDao().commitTransaction(pluginId(), userId, txId,
                Transaction.TX_STATE_COMMITTED,
                time, r.getClass().getName(), id);

        return r;
    }

    public abstract Response commitTransaction(
            long txUserId, String txId, long txAuthUserId, Response r, long time,
            int maxTries, long maxTryTime,
            IActionDao.OnResponseMerge<Response> merger);

}
