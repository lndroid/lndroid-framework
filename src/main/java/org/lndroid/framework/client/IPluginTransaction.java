package org.lndroid.framework.client;

import java.lang.reflect.Type;

public interface IPluginTransaction {
    // id of plugin that is communicated with using this tx
    String pluginId();
    // returns the tx id
    String id();
    // true if tx is started and still active (no error was returned by server)
    boolean isActive();
    // to override the client's current token
    void setSessionToken(String token);
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

    // if owner does not want to store a reference to the tx,
    // and instead want's it to exist until done,
    // call detach: client will hold a strong reference to this tx,
    // and it will self-destruct when done
    void detach();
}
