package org.lndroid.framework.engine;

public interface IPluginServer {

    IDaoConfig getDaoConfig();
    IDaoProvider getDaoProvider();
    IKeyStore getKeyStore();
    IIdGenerator getIdGenerator();
}
