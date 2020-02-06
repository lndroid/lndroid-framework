package org.lndroid.framework.engine;

public interface IPlugin {
    String id();
    void init(IPluginServer server,
              IPluginForegroundCallback fcb,
              IPluginBackgroundCallback bcb);

    // either one must be not null
    IPluginForeground getForeground();
    IPluginBackground getBackground();
}
