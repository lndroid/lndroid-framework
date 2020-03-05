package org.lndroid.framework.dao;

public interface ITransactionDao {
    void init();
    <T> T getTransactionRequest(String pluginId, long userId, String txId, Class<T> cls);
}
