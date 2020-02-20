package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.plugins.Transaction;

public interface IJobDao<Request, Response> {
    // get all active txs
    List<Transaction<Request>> getTransactions();

    // get tx
    Transaction<Request> getTransaction(long txUserId, String txId);

    // start tx
    void startTransaction(Transaction<Request> t);

    // write response to db (if required), attach response to tx, set to COMMITTED state,
    // resp.id will be initialized after this call.
    Response commitTransaction(long txUserId, String txId, long txAuthUserId, Response resp);

    // set auth user/time, set to REJECTED state
    void rejectTransaction(long txUserId, String txId, long txAuthUserId);

    // mark as timed out, set to TX_TIMEOUT state
    void timeoutTransaction(long txUserId, String txId);

    Response getResponse(long id);
}
