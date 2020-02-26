package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.plugins.Transaction;

public interface IActionDao<Request, Response> {

    interface OnResponseMerge<Response>{
        Response merge(Response old, Response cur);
    }

    // get all non-committed sessions
    List<Transaction<Request>> getTransactions();

    // check if specific tx exists
    Transaction<Request> getTransaction(long txUserId, String txId);

    // returns response entity
    Response getResponse(long id);

    // start tx
    void startTransaction(Transaction<Request> t);

    // actually add user, return new user object w/ id set properly
    Response commitTransaction(long txUserId, String txId, long txAuthUserId, Response r,
                               OnResponseMerge<Response> merger);

    // mark as rejected
    void rejectTransaction(long txUserId, String txId, long txAuthUserId);

    // mark as timed out
    void timeoutTransaction(long txUserId, String txId);
}
