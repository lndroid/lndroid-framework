package org.lndroid.framework.engine;

import android.content.ComponentName;
import android.content.Intent;

public interface IBroadcaster {
    void sendBroadcast(String pluginId, String txId, ComponentName component);
}
