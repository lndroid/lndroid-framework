package org.lndroid.framework.dao;

public interface IJobDao<Request, Response> extends IActionDao<Request, Response> {

    Response commitTransaction(long txUserId, String txId, long txAuthUserId, Response r,
                               int maxTries, long maxTryTime);

}
