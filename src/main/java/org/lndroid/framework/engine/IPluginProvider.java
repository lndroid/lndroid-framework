package org.lndroid.framework.engine;

import java.util.Set;

public interface IPluginProvider {
    // create plugins, etc,
    // called inside the plugin server thread
    void init();
    Set<String> getPluginIds();
    IPlugin getPlugin(String pluginId);
}
