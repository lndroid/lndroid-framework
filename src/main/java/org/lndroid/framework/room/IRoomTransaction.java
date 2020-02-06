package org.lndroid.framework.room;

interface IRoomTransaction<Request, Response> {
    Request getRequest();
    Response getResponse();
    RoomTransactions.TransactionData getTxData();
    void setRequest(Request r);
    void setResponse(Response r);
    void setTxData(RoomTransactions.TransactionData t);
}
