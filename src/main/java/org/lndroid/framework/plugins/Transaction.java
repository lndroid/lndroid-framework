package org.lndroid.framework.plugins;

import androidx.annotation.NonNull;

public class Transaction<Request> {
    public static final int TX_STATE_NEW = 0;
    public static final int TX_STATE_COMMITTED = 1;
    public static final int TX_STATE_REJECTED = 2;
    public static final int TX_STATE_TIMEDOUT = 3;
    public static final int TX_STATE_ERROR = 4;

    public static final int JOB_STATE_NONE = 0;
    public static final int JOB_STATE_EXECUTING = 1;
    public static final int JOB_STATE_DONE = 2;
    public static final int JOB_STATE_LOST = 3;
    public static final int JOB_STATE_RETRY = 4;
    public static final int JOB_STATE_FAILED = 5;
    public static final int JOB_STATE_NEW = 6;

    public static class TransactionData {
        @NonNull
        public String pluginId;
        public long userId;
        @NonNull
        public String txId;
        public int state;
        public long createTime;
        public long deadlineTime;
        public long doneTime;

        // user and time of last auth on this tx
        public long authUserId;
        public long authTime;

        // some txs might fail even with valid parameters and
        // we need to store that info
        public String errorCode;
        public String errorMessage;

        // link to request object
        public String requestClass;
        public long requestId;

        // if response is not stored then it's assumed
        // that tx has no side-effets and thus may be safely repeated
        public String responseClass;
        public long responseId;

        public TransactionData(String pluginId, long userId, String txId) {
            this.pluginId = pluginId;
            this.userId = userId;
            this.txId = txId;

            state = 0;
            createTime = 0;
            deadlineTime = 0;
            doneTime = 0;
            authUserId = 0;
            authTime = 0;

            errorCode = null;
            errorMessage = null;

            requestClass = null;
            requestId = 0;

            responseClass = null;
            responseId = 0;
        }
    }

    public static class JobData{

        // current number of tries
        public int tries;

        // max number of tries
        public int maxTries;

        // deadline for retries, in ms
        public long maxTryTime;

        // last time we retried, in ms
        public long lastTryTime;

        // next time we'll retry, in ms
        public long nextTryTime;

        // see above
        public int jobState;

        // error code if state=failed
        public String jobErrorCode;

        // error message
        public String jobErrorMessage;

        public JobData() {
            tries = 0;
            maxTries = 0;
            maxTryTime = 0;
            lastTryTime = 0;
            nextTryTime = 0;
            jobState = 0;
            jobErrorCode = null;
            jobErrorMessage = null;
        }
    }

    @NonNull
    public TransactionData tx;
    @NonNull
    public JobData job;

    // Embedded request: we need to store request bcs
    // all calls are async and might require auth and must be
    // restored after restart. And we don't store requests
    // in separate tables bcs a) this is simpler, b) some
    // requests are not entities but only partial info on target
    // entity.
    public Request request;

    public Transaction() {
        // NOTE: txData requires params, so it's initialized separately
        job = new JobData();
    }
}
