package org.lndroid.framework.usecases.bg;

import android.util.Log;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.IPluginTransaction;
import org.lndroid.framework.client.IPluginTransactionCallback;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PaidInvoicesNotifyWorker {

    public interface INotificationManager {
        void createNotification(WalletData.PaidInvoicesEvent event);
    };

    private static final String TAG = "PaidInvNotifyWorker";
    private static final long TIMEOUT = 24 * 60 * 60 * 1000; // 1day

    private static final PaidInvoicesNotifyWorker instance_ = new PaidInvoicesNotifyWorker();

    private IPluginClient client_;
    private IPluginTransaction tx_;
    private INotificationManager notificationManager_;
    private Set<IPluginTransaction> setNotifiedTxs_ = new HashSet<>();

    private PaidInvoicesNotifyWorker() {
    }

    public static PaidInvoicesNotifyWorker getInstance() {
        return instance_;
    }

    public PaidInvoicesNotifyWorker setNotificationManager(INotificationManager nm) {
        notificationManager_ = nm;
        return this;
    }

    private void reset() {
        if (tx_ != null)
            tx_.destroy();
        tx_ = null;
    }

    private class SetNotified {
        private IPluginTransaction tx_;

        SetNotified(final WalletData.PaidInvoicesEvent e){
            tx_ = client_.createTransaction(DefaultPlugins.SET_NOTIFIED_INVOICES, "", new IPluginTransactionCallback() {
                @Override
                public void onResponse(IPluginData r) {
                    Log.i(TAG, "set notified invoices done for "+e);
                    tx_.destroy();
                    setNotifiedTxs_.remove(tx_);
                    tx_ = null;
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
                    Log.e(TAG, "set notified invoices failed for "+e+" code "+code+" message "+message);
                    tx_.destroy();
                    setNotifiedTxs_.remove(tx_);
                    tx_ = null;
                }
            });

            // store a reference to not let GC clear it!
            setNotifiedTxs_.add(tx_);

            // start
            WalletData.NotifiedInvoicesRequest r = WalletData.NotifiedInvoicesRequest.builder()
                    .setInvoiceIds(e.invoiceIds())
                    .build();
            tx_.start(r, WalletData.NotifiedInvoicesRequest.class);
        }
    }

    private void setNotified(WalletData.PaidInvoicesEvent e) {
        new SetNotified(e);
    }

    private void start() {
        WalletData.SubscribePaidInvoicesEventsRequest r = WalletData.SubscribePaidInvoicesEventsRequest.builder()
                .build();

        tx_ = client_.createTransaction(DefaultPlugins.SUBSCRIBE_PAID_INVOICES_EVENTS, "", new IPluginTransactionCallback() {
            @Override
            public void onResponse(IPluginData in) {
                in.assignDataType(WalletData.PaidInvoicesEvent.class);
                WalletData.PaidInvoicesEvent p = null;
                try {
                    p = in.getData();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                notificationManager_.createNotification(p);

                setNotified(p);

                Log.i(TAG, "notification for paid invoices event "+p);
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
                Log.e(TAG, "subscribe paid invoices events error "+code+" msg "+message);
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
        if (tx_ != null) {
            if (tx_.isActive())
                tx_.stop();
            tx_.destroy();
        }
        tx_ = null;
    }
}

