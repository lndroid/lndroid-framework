package org.lndroid.framework.engine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Messenger;

import androidx.annotation.Nullable;

public class PluginService extends Service {

    private Messenger server_;

    PluginService(Messenger server) {
        super();
        server_ = server;
    }

    @Nullable
    public void onCreate() {
        // FIXME show notification
    }

    @Override
    public void onDestroy() {
        // FIXME hide notification?
    }

    @Override
    public IBinder onBind(Intent intent) {
        return server_.getBinder();
    }
}
