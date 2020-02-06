package org.lndroid.framework.engine;

public interface IPlugin {
    String id();
    void init(IDaoProvider dp,
              IPluginForegroundCallback fcb,
              IPluginBackgroundCallback bcb);

    // both might not be null
    IPluginForeground getForeground();
    IPluginBackground getBackground();
}
