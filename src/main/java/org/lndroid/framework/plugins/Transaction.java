package org.lndroid.framework.plugins;

public class Transaction<Request, Response> {
    public static final int TX_STATE_COMMITTED = 1;
    public static final int TX_STATE_REJECTED = 2;
    public static final int TX_STATE_TIMEDOUT = 3;
    public static final int TX_STATE_ERROR = 4;

    public int userId;
    public String txId;
    public long createTime;
    public long deadlineTime;
    public long doneTime;

    // user and time of last auth on this tx
    public int authUserId;
    public long authTime;

    // embedded request and response
    public Request request;
    public Response response;
}
