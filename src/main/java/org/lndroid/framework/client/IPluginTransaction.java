package org.lndroid.framework.client;

import java.lang.reflect.Type;

public interface IPluginTransaction {
    // id of plugin that is communicated with using this tx
    String pluginId();
    // returns the tx id
    String id();
    // true if tx is started and still active (no error was returned by server)
    boolean isActive();
    // call to start request
    void start(Object r, Type type);
    // pass tx timeout in ms
    void start(Object r, Type type, long timeout);
    // call if it's an input stream to provide more inputs
    void send(Object r, Type type);
    // call to stop subscription or request streaming, etc
    void stop();

    // release tx resources, optional, but
    // might allow client to release some memory
    void destroy();
}
