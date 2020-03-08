package org.lndroid.framework.defaults;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.engine.IBroadcaster;

public class DefaultBroadcaster implements IBroadcaster {

    private static final String TAG = "DefaultBroadcaster";

    private Context ctx_;

    public DefaultBroadcaster(Context ctx) {
        ctx_ = ctx;
    }

    @Override
    public void sendBroadcast(String pluginId, String txId, ComponentName component) {
        Log.i(TAG, "sending broadcast for "+pluginId+" tx "+txId+" to "+component);
        Intent in = new Intent(PluginData.BROADCAST_ACTION);
        in.setComponent(component);
        in.putExtra(PluginData.BROADCAST_PLUGIN, pluginId);
        in.putExtra(PluginData.BROADCAST_TX, txId);
        ctx_.sendBroadcast(in);
    }
}
