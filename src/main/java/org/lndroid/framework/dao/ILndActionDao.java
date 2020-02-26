package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.plugins.Transaction;

public interface ILndActionDao<Request, Response> {

    interface OnResponseMerge<Response>{
        Response merge(Response old, Response cur);
    }

    // get all active txs
    List<Transaction<Request>> getTransactions();

    // get tx
    Transaction<Request> getTransaction(long txUserId, String txId);

    // start tx
    void startTransaction(Transaction<Request> t);

    // write response to db (if required), attach response to tx, set to COMMITTED state,
    // allows client to specify custom merger in case an
    // existing Response record might be updated
    Response commitTransaction(long txUserId, String txId, Response resp, OnResponseMerge<Response> merger);

    // set auth user/time
    void confirmTransaction(long txUserId, String txId, long txAuthUserId, Request authedRequest);

    // set auth user/time, set to REJECTED state
    void rejectTransaction(long txUserId, String txId, long txAuthUserId);

    // set transaction state to 'failed'
    void failTransaction(long txUserId, String txId, String code, String message);

    // mark as timed out, set to TX_TIMEOUT state
    void timeoutTransaction(long txUserId, String txId);

    Response getResponse(long id);
}
