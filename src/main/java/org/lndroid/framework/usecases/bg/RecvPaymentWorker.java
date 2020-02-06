package org.lndroid.framework.usecases.bg;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import org.lndroid.framework.client.IPluginClient;

public abstract class RecvPaymentWorker extends androidx.work.Worker {
    public static final String WORK_ID = "org.lndroid.framework.work.RECV_PAYMENT_WORK";
    private static final String TAG = "RecvPaymentWorker";

    private static final long MIN_SYNC_TIME = 5 * 60 * 1000; // 5 min
    private static final long MAX_SYNC_TIME = 15 * 60 * 1000; // 15 min
    private static final long WORK_INTERVAL = 60 * 60 * 1000; // 1 h
    private static final long FLEX_INTERVAL = 15 * 60 * 1000; // 15 min

    public RecvPaymentWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // NOTE: override to provide plugin client instance to the worker when it starts
    public abstract IPluginClient getPluginClient();

    @Override
    @NonNull
    public Result doWork() {
        Log.i(TAG, "starting");

        SyncWorkerImpl impl = new SyncWorkerImpl(getPluginClient(), TAG);
        if (impl.execute(MIN_SYNC_TIME, MAX_SYNC_TIME))
            return Result.success();
        else
            return Result.retry();
    }

    public static <Worker extends RecvPaymentWorker>
    void schedule(Context ctx, Class<Worker> workerClass, int version) {
        Constraints constraints = new Constraints.Builder()
                // maybe we don't want to spend user's money
                .setRequiredNetworkType(NetworkType.NOT_ROAMING)
                .build();
        schedule(ctx, workerClass, constraints, version);
    }

    public static <Worker extends RecvPaymentWorker>
    void schedule(Context ctx, Class<Worker> workerClass, Constraints constraints, int version) {

        PeriodicWorkRequest work =
                // run for up to 'flex' minutes at the end of every 'interval'
                new PeriodicWorkRequest.Builder(workerClass,
                        WORK_INTERVAL, TimeUnit.MILLISECONDS,
                        FLEX_INTERVAL, TimeUnit.MILLISECONDS)
                        .addTag(SyncWorkerImpl.getVersionTag(WORK_ID, version))
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                        .build();

        SyncWorkerImpl.schedule(ctx, work, WORK_ID, version);
    }

}
