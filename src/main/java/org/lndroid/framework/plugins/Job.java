package org.lndroid.framework.plugins;

import java.util.List;

public class Job {
    public String pluginId;
    public long userId;
    public String txId;

    public Transaction.JobData job;
    public Object request;
    public List<Object> objects;

    public Job(String pluginId, long userId, String txId) {
        this.pluginId = pluginId;
        this.userId = userId;
        this.txId = txId;
    }
}
