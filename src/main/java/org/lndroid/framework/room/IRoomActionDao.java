package org.lndroid.framework.room;

import java.util.List;

interface IRoomActionDao<Transaction, Response> {
    List<Transaction> getTransactions();
    Transaction getTransaction(int txUserId, String txId);
    void createTransaction(Transaction tx);
    void updateTransaction(Transaction tx);
    void failTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);
    Response commitTransaction(Transaction tx, int txAuthUserId, Response user, long time);
}

