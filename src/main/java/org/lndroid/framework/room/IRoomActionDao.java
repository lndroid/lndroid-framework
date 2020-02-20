package org.lndroid.framework.room;

import java.util.List;

interface IRoomActionDao<Transaction, Response> {
    List<Transaction> getTransactions();
    Transaction getTransaction(long txUserId, String txId);
    void createTransaction(Transaction tx);
    void updateTransaction(Transaction tx);
    void failTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);
    Response commitTransaction(Transaction tx, long txAuthUserId, Response user, long time);
    Response getResponse(long id);
}

