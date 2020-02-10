package org.lndroid.framework.dao;

import org.lndroid.framework.engine.IPluginDao;

public interface IDBDaoProvider {
    // open and decrypt db
    void init(String db, byte[] password);

    IAuthDao getAuthDao();
    IAuthRequestDao getAuthRequestDao();
    IRawQueryDao getRawQueryDao();
    IPluginDao getPluginDao(String pluginId);
}
