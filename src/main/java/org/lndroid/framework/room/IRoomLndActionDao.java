package org.lndroid.framework.room;

import java.util.List;

interface IRoomLndActionDao<Transaction, Request, Response> {
    List<Transaction> getTransactions();
    Transaction getTransaction(int txUserId, String txId);
    void createTransaction(Transaction tx);
    void updateTransaction(Transaction tx);
    void confirmTransaction(int txUserId, String txId, int txAuthUserId, long time, Request authedRequest);
    void rejectTransaction(int txUserId, String txId, int txAuthUserId, int txState, long time);
    void failTransaction(int txUserId, String txId, String code, int txState, long time);
    void timeoutTransaction(int txUserId, String txId, int txState, long time);
    Response commitTransaction(int txUserId, String txId, Response r, long time);
}
