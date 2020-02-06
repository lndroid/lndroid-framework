package org.lndroid.framework.engine;

public interface IPluginBackgroundCallback {

    // auth id is returned to plugin so that it could store
    // some additional info. auth clients are
    // not notified immediately, and thus
    // plugin can finish writing it's request before
    // clients start reading it.
    int onAuthBackground(String pluginId, String type);

    // notify other plugins about some change
    void onSignal(String pluginId, String topic, Object data);
}
