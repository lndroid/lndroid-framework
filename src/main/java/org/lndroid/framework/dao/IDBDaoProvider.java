package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;

public interface IDBDaoProvider {

    interface OpenCallback {
        void onOpen();
    }

    // open and decrypt db
    void init(String db, byte[] password, OpenCallback cb);
    void insertUser(WalletData.User user);

    IAuthDao getAuthDao();
    IAuthRequestDao getAuthRequestDao();
    IRawQueryDao getRawQueryDao();
    IPluginDao getPluginDao(String pluginId);
}
