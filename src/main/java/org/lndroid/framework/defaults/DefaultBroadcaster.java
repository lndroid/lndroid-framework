package org.lndroid.framework.defaults;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.engine.IBroadcaster;

public class DefaultBroadcaster implements IBroadcaster {

    private Context ctx_;

    public DefaultBroadcaster(Context ctx) {
        ctx_ = ctx;
    }

    @Override
    public void sendBroadcast(String pluginId, String txId, ComponentName component) {
        Intent in = new Intent(PluginData.BROADCAST_ACTION);
        in.setComponent(component);
        in.putExtra(PluginData.BROADCAST_PLUGIN, pluginId);
        in.putExtra(PluginData.BROADCAST_TX, txId);
        ctx_.sendBroadcast(in);
    }
}
