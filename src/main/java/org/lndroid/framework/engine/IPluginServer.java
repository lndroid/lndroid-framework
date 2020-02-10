package org.lndroid.framework.engine;

public interface IPluginServer {

    IDaoProvider getDaoProvider();
    IKeyStore getKeyStore();
    IIdGenerator getIdGenerator();
}
