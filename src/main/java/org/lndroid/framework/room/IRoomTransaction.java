package org.lndroid.framework.room;

interface IRoomTransaction<Request> {
    Request getRequest();
    RoomTransactions.TransactionData getTxData();
    void setRequest(Request r);
    void setResponse(Class<?> cls, long id);
    void setTxData(RoomTransactions.TransactionData t);
}
