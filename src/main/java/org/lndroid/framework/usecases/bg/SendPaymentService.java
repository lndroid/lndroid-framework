package org.lndroid.framework.usecases.bg;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.IPluginTransaction;
import org.lndroid.framework.client.IPluginTransactionCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.DefaultPlugins;

public class SendPaymentService {

    public interface INofiticationFactory {
        int notificationId();
        Notification createNotification(int paymentCount);
    }

    private static final String TAG = "SendPaymentService";
    private static final long TIMEOUT = 24 * 60 * 60 * 1000; // 1day

    private static final SendPaymentService instance_ = new SendPaymentService();

    private IPluginClient client_;
    private IPluginTransaction tx_;
    private INofiticationFactory notificationFactory_;
    private Set<Long> activePayments_ = new HashSet<>();
    private Context ctx_;
    private Notification notification_;
    private int notificationId_;

    private SendPaymentService() {
    }

    public static SendPaymentService getInstance() {
        return instance_;
    }

    public void setContext(Context ctx) {
        ctx_ = ctx;
    }

    public void setNotificationFactory(INofiticationFactory nf) {
        notificationFactory_ = nf;
    }

    private void startService() {
        // we might call this many times if tx terminates by timeout
        // and we restart, we'd get new active payment list but
        // service is already started and no need to restart it.
        if (notification_ != null)
            return;

        notification_ = notificationFactory_.createNotification(activePayments_.size());
        notificationId_ = notificationFactory_.notificationId();

        Intent intent = new Intent(ctx_, ForegroundService.class);
        ContextCompat.startForegroundService(ctx_, intent);
    }

    private void stopService() {
        Intent intent = new Intent(ctx_, ForegroundService.class);
        ctx_.stopService(intent);
        notification_ = null;
    }

    private void reset() {
        if (tx_ != null)
            tx_.destroy();
        tx_ = null;
        activePayments_.clear();
    }

    private void start() {
        WalletData.SubscribeRequest r = WalletData.SubscribeRequest.builder()
                .setNoAuth(true)
                .build();

        tx_ = client_.createTransaction(DefaultPlugins.SUBSCRIBE_SEND_PAYMENTS, "", new IPluginTransactionCallback() {
            @Override
            public void onResponse(IPluginData in) {
                in.assignDataType(WalletData.SendPayment.class);
                WalletData.SendPayment p = null;
                try {
                    p = in.getData();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                final boolean wasEmpty = activePayments_.isEmpty();
                switch(p.state()) {
                    case WalletData.SEND_PAYMENT_STATE_OK:
                    case WalletData.SEND_PAYMENT_STATE_FAILED:
                        activePayments_.remove(p.id());
                        if (!wasEmpty && activePayments_.isEmpty())
                            stopService();
                        break;
                    case WalletData.SEND_PAYMENT_STATE_PENDING:
                    case WalletData.SEND_PAYMENT_STATE_SENDING:
                        activePayments_.add(p.id());
                        if (wasEmpty)
                            startService();
                        break;
                }
                Log.i(TAG, "active payments: "+activePayments_.size());

                // FIXME use notification manager to update notification w/ new counter
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
                Log.e(TAG, "subscribe send payments error "+code+" msg "+message);
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
        if (!activePayments_.isEmpty())
            stopService();
        activePayments_.clear();

        if (tx_ != null) {
            if (tx_.isActive())
                tx_.stop();
            tx_.destroy();
        }
        tx_ = null;
    }

    public static class ForegroundService extends Service {

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public int onStartCommand(Intent intent, int flags, int startId) {
            Toast.makeText(this, "Starting service to send payments", Toast.LENGTH_SHORT).show();
            SendPaymentService ps = SendPaymentService.getInstance();
            startForeground(ps.notificationId_, ps.notification_);

            return START_STICKY;
        }
    }
}
