package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.plugins.Transaction;

public interface ILndActionDao<Request, Response> {

    // get all active txs
    List<Transaction<Request, Response>> getTransactions();

    // get tx
    Transaction<Request, Response> getTransaction(int txUserId, String txId);

    // start tx
    void startTransaction(Transaction<Request, Response> t);

    // write response to db (if required), attach response to tx, set to COMMITTED state,
    // resp.id will be initialized after this call.
    Response commitTransaction(int txUserId, String txId, Response resp);

    // set auth user/time
    void confirmTransaction(int txUserId, String txId, int txAuthUserId, Request authedRequest);

    // set auth user/time, set to REJECTED state
    void rejectTransaction(int txUserId, String txId, int txAuthUserId);

    // set transaction state to 'failed'
    void failTransaction(int txUserId, String txId, String code);

    // mark as timed out, set to TX_TIMEOUT state
    void timeoutTransaction(int txUserId, String txId);
}
