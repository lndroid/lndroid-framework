package org.lndroid.framework.usecases.bg;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.IPluginTransaction;
import org.lndroid.framework.client.IPluginTransactionCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

public class BackgroundActivityService {

    public interface IServiceManager {
        void startService(WalletData.BackgroundInfo info);
        void updateNotification(WalletData.BackgroundInfo info);
        void stopService();
        int notificationId();
        Notification notification();
    };

    private static final String TAG = "BackgroundActivService";
    private static final long TIMEOUT = 24 * 60 * 60 * 1000; // 1day

    private static final BackgroundActivityService instance_ = new BackgroundActivityService();

    private IPluginClient client_;
    private IPluginTransaction tx_;
    private IServiceManager serviceManager_;
    private WalletData.BackgroundInfo info_;

    private BackgroundActivityService() {
    }

    public static BackgroundActivityService getInstance() {
        return instance_;
    }

    public void setServiceManager(IServiceManager sm) {
        serviceManager_ = sm;
    }

    private void startService() {
        serviceManager_.startService(info_);
    }

    private void stopService() {
        serviceManager_.stopService();
    }

    private void updateNotification() {
        serviceManager_.updateNotification(info_);
    }

    private void reset() {
        if (tx_ != null)
            tx_.destroy();
        tx_ = null;
        info_ = null;
    }

    private void start() {
        WalletData.SubscribeRequest r = WalletData.SubscribeRequest.builder()
                .setNoAuth(true)
                .build();

        tx_ = client_.createTransaction(DefaultPlugins.SUBSCRIBE_BACKGROUND_INFO, "", new IPluginTransactionCallback() {
            @Override
            public void onResponse(IPluginData in) {
                in.assignDataType(WalletData.BackgroundInfo.class);
                WalletData.BackgroundInfo p = null;
                try {
                    p = in.getData();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                final boolean wasActive = info_ != null && info_.isActive();
                info_ = p;

                if (info_.isActive() && !wasActive)
                    startService();
                else if (!info_.isActive() && wasActive)
                    stopService();
                else if (info_.isActive())
                    updateNotification();

                Log.i(TAG, "background wasActive "+wasActive+" info  "+info_);
            }

            @Override
            public void onAuth(WalletData.AuthRequest r) {
                throw new RuntimeException("Unexpected auth");
            }

            @Override
            public void onAuthed(WalletData.AuthResponse r) {
                throw new RuntimeException("Unexpected authed");
            }

            @Override
            public void onError(String code, String message) {
                Log.e(TAG, "subscribe background info error "+code+" msg "+message);
                reset();

                // restart if terminated by tx timeout
                if (code.equals(Errors.TX_TIMEOUT))
                    start();
            }
        });

        tx_.start(r, WalletData.SubscribeRequest.class, TIMEOUT);
    }

    public void start(IPluginClient client) {
        client_ = client;
        start();
    }

    public void stop() {
        if (info_ != null && info_.isActive())
            stopService();

        if (tx_ != null) {
            if (tx_.isActive())
                tx_.stop();
            tx_.destroy();
        }
        tx_ = null;
        info_ = null;
    }

    public static class ForegroundService extends Service {

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Toast.makeText(this, "Starting background service", Toast.LENGTH_SHORT).show();
            BackgroundActivityService ps = BackgroundActivityService.getInstance();
            if (ps.serviceManager_.notification() != null) {
                startForeground(ps.serviceManager_.notificationId(), ps.serviceManager_.notification());
                return START_STICKY;
            } else {
                Log.e(TAG, "notification is null, was the service shut down requested?");
                return START_NOT_STICKY;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.i(TAG, "destroyed");
//            Intent broadcastIntent = new Intent(this, RestartBroadcastReceiver.class);
//            sendBroadcast(broadcastIntent);
        }
    }

/*    public static class RestartBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BroadcastReceiver.PendingResult pendingResult = goAsync();
            Task asyncTask = new Task(pendingResult, context);
            asyncTask.execute();
        }
    }

 */
}
