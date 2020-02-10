package org.lndroid.framework.room;

import java.util.List;

interface IRoomLndActionDao<Transaction, Request, Response> {
    List<Transaction> getTransactions();
    Transaction getTransaction(long txUserId, String txId);
    void createTransaction(Transaction tx);
    void updateTransaction(Transaction tx);
    void confirmTransaction(long txUserId, String txId, long txAuthUserId, long time, Request authedRequest);
    void rejectTransaction(long txUserId, String txId, long txAuthUserId, int txState, long time);
    void failTransaction(long txUserId, String txId, String code, int txState, long time);
    void timeoutTransaction(long txUserId, String txId, int txState, long time);
    Response commitTransaction(long txUserId, String txId, Response r, long time);
}
