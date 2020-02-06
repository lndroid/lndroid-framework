package org.lndroid.framework.engine;

import org.lndroid.framework.IKeyStore;

public interface IPluginServer {

    IDaoProvider getDaoProvider();
    IKeyStore getKeyStore();
}
