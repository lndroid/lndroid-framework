package org.lndroid.framework.usecases.bg;

public interface ISyncNotificationManager {
    int SYNC_TYPE_GRAPH_CHAIN = 0;
    int SYNC_TYPE_RECV_PAYMENTS = 0;

    void showNotification(int syncType);
    void hideNotification(int syncType);
};
