package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.plugins.Transaction;

public interface IActionDao<Request, Response> {

    // get all non-committed sessions
    List<Transaction<Request, Response>> getTransactions();

    // check if specific tx exists
    Transaction<Request, Response> getTransaction(long txUserId, String txId);

    // start tx
    void startTransaction(Transaction<Request, Response> t);

    // actually add user, return new user object w/ id set properly
    Response commitTransaction(long txUserId, String txId, long txAuthUserId, Response r);

    // mark as rejected
    void rejectTransaction(long txUserId, String txId, long txAuthUserId);

    // mark as timed out
    void timeoutTransaction(long txUserId, String txId);
}
