package org.lndroid.framework.room;

abstract class RoomJobDaoBase<Request, Response> extends RoomActionDaoBase<Request, Response>{


    public Response commitTransaction(
            long userId, String txId, long txAuthUserId, Response r, long time) {
        throw new RuntimeException("Unsupported method");
    }

    protected Response commitTransactionImpl(
            long userId, String txId, long txAuthUserId, Response r, long time,
            int maxTries, long maxTryTime)
    {
        final long id = insertResponse(r);

        // update tx state: confirm and commit
        txDao().initTransactionJob(pluginId(), userId, txId, maxTries, maxTryTime);
        txDao().confirmTransaction(pluginId(), userId, txId, txAuthUserId, time);
        txDao().commitTransaction(pluginId(), userId, txId,
                org.lndroid.framework.plugins.Transaction.TX_STATE_COMMITTED,
                time, r.getClass().getName(), id);

        return r;
    }

    public abstract Response commitTransaction(
            long txUserId, String txId, long txAuthUserId, Response r, long time,
            int maxTries, long maxTryTime);

}
