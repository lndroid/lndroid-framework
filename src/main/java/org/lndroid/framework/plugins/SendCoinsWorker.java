package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.List;

public class SendCoinsWorker implements IPluginBackground {

    public interface IDao {
        List<WalletData.Transaction> getNewTransactions();
        List<WalletData.Transaction> getSendingTransactions();
        List<WalletData.Transaction> getRetryTransactions();
        void updateTransaction(WalletData.Transaction t);
    }

    private static final String TAG = "SendCoinsWorker";
    private static final long DEFAULT_EXPIRY = 3600000; // 1h
    private static final long TRY_INTERVAL = 60000; // 1m
    private static final long WORK_INTERVAL = 10000; // 10sec

    private IPluginBackgroundCallback engine_;
    private IDao dao_;
    private ILightningDao lnd_;
    private boolean notified_;
    private long nextWorkTime_;

    @Override
    public String id() {
        return DefaultPlugins.SEND_COINS_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        engine_ = callback;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();

        // NOTE: start protocol to recover in-flight payments' state:
        // - get all payments w/ 'opening' state from db
        // - mark as Lost, let StateWorker sync w/ lnd and
        //   either update this channel to proper state, or set state to RETRY
        List<WalletData.Transaction> pending = dao_.getSendingTransactions();
        for (WalletData.Transaction t: pending) {
            onUpdate(t.toBuilder().setState(WalletData.TRANSACTION_STATE_LOST).build());
        }
    }

    private void onUpdate(WalletData.Transaction t) {
        dao_.updateTransaction(t);
        engine_.onSignal(id(), DefaultTopics.TRANSACTION_STATE, null);
    }

    private void onFailed(WalletData.Transaction c, WalletData.Transaction.Builder b) {
        // FIXME check if it's permanent failure or not

        int tries = c.tries();
        tries++;

        if (tries >= c.maxTries() || System.currentTimeMillis() > c.maxTryTime()) {
            b.setState(WalletData.TRANSACTION_STATE_FAILED);
        } else {
            b.setNextTryTime(System.currentTimeMillis() + TRY_INTERVAL);
            b.setState(WalletData.TRANSACTION_STATE_NEW);
        }
    }

    private WalletData.Transaction prepare(WalletData.Transaction.Builder b){

        // mark as opening
        b.setState(WalletData.TRANSACTION_STATE_SENDING);
        b.setLastTryTime(System.currentTimeMillis());

        // ensure deadline
        if (b.maxTryTime() == 0) {
            b.setMaxTryTime(b.lastTryTime() + DEFAULT_EXPIRY);
        }

        return b.build();
    }

    private void onLndError(WalletData.Transaction ut, int code, String error) {
        Log.e(TAG, "send coins error "+code+" err "+error);

        WalletData.Transaction.Builder b = ut.toBuilder();
        b.setErrorCode(Errors.LND_ERROR);
        b.setErrorMessage(error);

        onFailed(ut, b);

        // write
        onUpdate(b.build());
    }

    private void sendCoins(WalletData.Transaction t) {

        WalletData.Transaction.Builder b = t.toBuilder();

        // convert to lnd request
        Data.SendCoinsRequest r = new Data.SendCoinsRequest();

        // bad payment?
        if (!LightningCodec.SendCoinsCodec.encode(t, r)) {
            Log.e(TAG, "send coins error bad request");
            b.setErrorCode(Errors.PLUGIN_INPUT);
            b.setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT));
            b.setState(WalletData.TRANSACTION_STATE_FAILED);
            onUpdate(b.build());
            return;
        }

        final WalletData.Transaction ut = prepare(b);

        // write opening state
        onUpdate(ut);

        // send
        lnd_.client().sendCoins(r, new ILightningCallback<Data.SendCoinsResponse>() {
            @Override
            public void onResponse(Data.SendCoinsResponse r) {
                Log.i(TAG, "send coins response "+r);
                WalletData.Transaction.Builder b = ut.toBuilder();
                LightningCodec.SendCoinsCodec.decode(r, b);
                b.setState(WalletData.TRANSACTION_STATE_SENT);
                b.setSendTime(System.currentTimeMillis());
                onUpdate(b.build());
            }

            @Override
            public void onError(int i, String s) {
                onLndError(ut, i, s);
            }
        });
    }

    private void sendMany(WalletData.Transaction t) {

        WalletData.Transaction.Builder b = t.toBuilder();

        // convert to lnd request
        Data.SendManyRequest r = new Data.SendManyRequest();

        // bad payment?
        if (!LightningCodec.SendCoinsCodec.encode(t, r)) {
            Log.e(TAG, "send coins error bad request");
            b.setErrorCode(Errors.PLUGIN_INPUT);
            b.setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT));
            b.setState(WalletData.TRANSACTION_STATE_FAILED);
            onUpdate(b.build());
            return;
        }

        final WalletData.Transaction ut = prepare(b);

        // write opening state
        onUpdate(ut);

        // send
        lnd_.client().sendMany(r, new ILightningCallback<Data.SendManyResponse>() {
            @Override
            public void onResponse(Data.SendManyResponse r) {
                Log.i(TAG, "send coins response "+r);
                WalletData.Transaction.Builder b = ut.toBuilder();
                LightningCodec.SendCoinsCodec.decode(r, b);
                b.setState(WalletData.TRANSACTION_STATE_SENT);
                b.setSendTime(System.currentTimeMillis());
                onUpdate(b.build());
            }

            @Override
            public void onError(int i, String s) {
                onLndError(ut, i, s);
            }
        });
    }


    @Override
    public void work() {

        // lnd must be ready
        if (!lnd_.isRpcReady())
            return;

        if (!notified_ && nextWorkTime_ > System.currentTimeMillis())
            return;

        // reset
        notified_ = false;

        // check retried txs, move to pending or failed
        List<WalletData.Transaction> retry = dao_.getRetryTransactions();
        for (WalletData.Transaction t: retry) {
            onLndError(t, -1, "Unknown lnd error");
        }

        // exec
        List<WalletData.Transaction> pending = dao_.getSendingTransactions();
        for (WalletData.Transaction t: pending) {
            if (t.sendAll())
                sendCoins(t);
            else
                sendMany(t);
        }

        nextWorkTime_ = System.currentTimeMillis() + WORK_INTERVAL;
    }

    @Override
    public void auth(WalletData.AuthRequest ar, WalletData.AuthResponse r) {
        throw new RuntimeException("Unexpected auth");
    }

    @Override
    public boolean isUserPrivileged(WalletData.User user, String requestType) {
        throw new RuntimeException("Unexpected priv check");
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.NEW_TRANSACTION);
    }

    @Override
    public void notify(String topic, Object data) {
        notified_ = true;
    }
}
