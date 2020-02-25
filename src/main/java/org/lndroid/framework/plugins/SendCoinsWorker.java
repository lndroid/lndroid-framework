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

        List<Job> getNewJobs(long now);
        List<Job> getSendingJobs();
        List<Job> getRetryJobs();

        void updateJob(Job j);
        void updateTransaction(Job j, WalletData.Transaction t);
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
        List<Job> sending = dao_.getSendingJobs();
        for (Job j: sending) {
            j.job.jobState = Transaction.JOB_STATE_LOST;
            dao_.updateJob(j);
        }
    }

    private void onUpdate(Job job, WalletData.Transaction t) {
        dao_.updateTransaction(job, t);
        engine_.onSignal(id(), DefaultTopics.TRANSACTION_STATE, null);
    }

    private void onFailed(Job job, WalletData.Transaction.Builder b) {
        // FIXME check if it's permanent failure or not

        job.job.tries++;

        if (job.job.tries >= job.job.maxTries || System.currentTimeMillis() > job.job.maxTryTime) {
            job.job.jobState = Transaction.JOB_STATE_FAILED;
            job.job.jobErrorMessage = b.errorMessage();
            job.job.jobErrorCode = b.errorCode();
            b.setState(WalletData.TRANSACTION_STATE_FAILED);
        } else {
            job.job.jobState = Transaction.JOB_STATE_NEW;
            job.job.nextTryTime = System.currentTimeMillis() + TRY_INTERVAL;
        }
    }

    private void writeStartJob(Job job){

        // mark as opening
        job.job.jobState = Transaction.JOB_STATE_EXECUTING;
        job.job.lastTryTime = System.currentTimeMillis();

        // ensure deadline
        if (job.job.maxTryTime == 0) {
            job.job.maxTryTime = job.job.lastTryTime + DEFAULT_EXPIRY;
        }

        dao_.updateJob(job);
    }

    private void onLndError(Job job, WalletData.Transaction ut, int code, String error) {
        Log.e(TAG, "send coins error "+code+" err "+error);

        WalletData.Transaction.Builder b = ut.toBuilder();
        b.setErrorCode(Errors.LND_ERROR);
        b.setErrorMessage(error);

        onFailed(job, b);

        // write
        onUpdate(job, b.build());
    }

    private void sendCoins(final Job job, final WalletData.Transaction t) {

        // convert to lnd request
        Data.SendCoinsRequest r = new Data.SendCoinsRequest();

        // bad payment?
        if (!LightningCodec.SendCoinsCodec.encode(t, r)) {
            Log.e(TAG, "send coins error bad request");
            WalletData.Transaction.Builder b = t.toBuilder();
            b.setErrorCode(Errors.PLUGIN_INPUT);
            b.setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT));
            onFailed(job, b);
            onUpdate(job, b.build());
            return;
        }

        // write opening state
        writeStartJob(job);

        // send
        lnd_.client().sendCoins(r, new ILightningCallback<Data.SendCoinsResponse>() {
            @Override
            public void onResponse(Data.SendCoinsResponse r) {
                Log.i(TAG, "send coins response "+r);
                WalletData.Transaction.Builder b = t.toBuilder();
                LightningCodec.SendCoinsCodec.decode(r, b);
                b.setState(WalletData.TRANSACTION_STATE_SENT);
                b.setSendTime(System.currentTimeMillis());

                job.job.jobState = Transaction.JOB_STATE_DONE;
                onUpdate(job, b.build());
            }

            @Override
            public void onError(int i, String s) {
                onLndError(job, t, i, s);
            }
        });
    }

    private void sendMany(final Job job, final WalletData.Transaction t) {


        // convert to lnd request
        Data.SendManyRequest r = new Data.SendManyRequest();

        // bad payment?
        if (!LightningCodec.SendCoinsCodec.encode(t, r)) {
            Log.e(TAG, "send coins error bad request");
            WalletData.Transaction.Builder b = t.toBuilder();
            b.setErrorCode(Errors.PLUGIN_INPUT);
            b.setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT));
            onFailed(job, b);
            onUpdate(job, b.build());
            return;
        }

        // write opening state
        writeStartJob(job);

        // send
        lnd_.client().sendMany(r, new ILightningCallback<Data.SendManyResponse>() {
            @Override
            public void onResponse(Data.SendManyResponse r) {
                Log.i(TAG, "send coins response "+r);
                WalletData.Transaction.Builder b = t.toBuilder();
                LightningCodec.SendCoinsCodec.decode(r, b);
                b.setState(WalletData.TRANSACTION_STATE_SENT);
                b.setSendTime(System.currentTimeMillis());

                job.job.jobState = Transaction.JOB_STATE_DONE;
                onUpdate(job, b.build());
            }

            @Override
            public void onError(int i, String s) {
                onLndError(job, t, i, s);
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
        List<Job> retry = dao_.getRetryJobs();
        for (Job job: retry) {
            WalletData.Transaction t = (WalletData.Transaction)job.objects.get(0);
            onLndError(job, t, -1, "Unknown lnd error");
        }

        // exec
        List<Job> pending = dao_.getNewJobs(System.currentTimeMillis());
        for (Job job: pending) {
            WalletData.Transaction t = (WalletData.Transaction)job.objects.get(0);
            if (t.sendAll())
                sendCoins(job, t);
            else
                sendMany(job, t);
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
