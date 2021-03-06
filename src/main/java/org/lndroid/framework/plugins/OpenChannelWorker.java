package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class OpenChannelWorker implements IPluginBackground {

    public interface IDao {
        List<Job> getNewJobs(long now);
        List<Job> getExecutingJobs();
        List<Job> getRetryJobs();

        void updateJob(Job j);
        void updateChannel(Job j, WalletData.Channel c);
    }

    private static final String TAG = "OpenChannelWorker";
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
        return DefaultPlugins.OPEN_CHANNEL_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        engine_ = callback;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();

        // NOTE: start protocol to recover in-flight state:
        // - get all records w/ 'opening' state from db
        // - mark as Lost, let StateWorker sync w/ lnd and
        //   either update this channel to proper state, or set state to RETRY
        List<Job> executing = dao_.getExecutingJobs();
        for (Job job: executing) {
            job.job.jobState = Transaction.JOB_STATE_LOST;
            dao_.updateJob(job);
        }
    }

    private void onUpdate(Job job, WalletData.Channel c) {
        dao_.updateChannel(job, c);
        engine_.onSignal(id(), DefaultTopics.CHANNEL_STATE, null);
    }

    private void onFailed(Job job, WalletData.Channel.Builder b) {
        // FIXME check if it's permanent failure or not

        job.job.tries++;

        if (job.job.tries >= job.job.maxTries || System.currentTimeMillis() > job.job.maxTryTime) {
            job.job.jobState = Transaction.JOB_STATE_FAILED;
            job.job.jobErrorMessage = b.errorMessage();
            job.job.jobErrorCode = b.errorCode();
            b.setState(WalletData.CHANNEL_STATE_FAILED);
        } else {
            job.job.jobState = Transaction.JOB_STATE_NEW;
            job.job.nextTryTime = System.currentTimeMillis() + TRY_INTERVAL;
        }
    }

    private void onLndError(Job job, WalletData.Channel c, int code, String message) {
        Log.e(TAG, "open channel error "+code+" err "+message);

        WalletData.Channel.Builder b = c.toBuilder();
        b.setErrorCode(Errors.LND_ERROR);
        b.setErrorMessage(message);

        onFailed(job, b);

        // write
        onUpdate(job, b.build());
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

    private void openChannel(final Job job, final WalletData.Channel c) {

        // convert to lnd request
        Data.OpenChannelRequest r = new Data.OpenChannelRequest();

        // bad payment?
        if (!LightningCodec.OpenChannelCodec.encode(c, r)) {
            Log.e(TAG, "open channel error bad request");
            WalletData.Channel.Builder b = c.toBuilder();
            b.setErrorCode(Errors.PLUGIN_INPUT);
            b.setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT));
            onFailed(job, b);
            onUpdate(job, b.build());
            return;
        }

        // mark as opening
        writeStartJob(job);

        // send
        lnd_.client().openChannel(r, new ILightningCallback<Data.ChannelPoint>() {
            @Override
            public void onResponse(Data.ChannelPoint r) {
                Log.i(TAG, "open channel response "+r);
                WalletData.Channel.Builder b = c.toBuilder();
                LightningCodec.OpenChannelCodec.decode(r, b);
                b.setState(WalletData.CHANNEL_STATE_PENDING_OPEN);
                b.setOpenTime(System.currentTimeMillis());

                job.job.jobState = Transaction.JOB_STATE_DONE;
                onUpdate(job, b.build());
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "open channel error "+i+" err "+s);
                onLndError(job, c, i, s);
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

        // move 'retry' to 'new' or 'failed'
        List<Job> retry = dao_.getRetryJobs();
        for (Job job: retry) {
            WalletData.Channel c = (WalletData.Channel)job.objects.get(0);
            onLndError(job, c, -1, "Unknown lnd error");
        }

        // open/retry channels
        List<Job> jobs = dao_.getNewJobs(System.currentTimeMillis());
        for (Job job: jobs) {
            WalletData.Channel c = (WalletData.Channel)job.objects.get(0);
            openChannel(job, c);
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
        topics.add(DefaultTopics.NEW_CHANNEL);
    }

    @Override
    public void notify(String topic, Object data) {
        notified_ = true;
    }
}
