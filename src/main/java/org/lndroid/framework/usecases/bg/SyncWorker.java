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

public abstract class SyncWorker extends androidx.work.Worker {
    public static final String WORK_ID = "org.lndroid.framework.work.SYNC_WORK";
    private static final String TAG = "SyncWorker";

    private static final long SYNC_TIME = 60 * 60 * 1000; // 1h
    private static final long WORK_INTERVAL = 24 * 60 * 60 * 1000;  // 1d

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // NOTE: override to provide plugin client instance to the worker when it starts
    public abstract IPluginClient getPluginClient();

    @Override
    @NonNull
    public Result doWork() {
        Log.i(TAG, "starting");

        SyncWorkerImpl impl = new SyncWorkerImpl(getPluginClient(), TAG);
        if (impl.execute(SYNC_TIME, SYNC_TIME))
            return Result.success();
        else
            return Result.retry();
    }

    public static <Worker extends SyncWorker>
    void schedule(Context ctx, Class<Worker> workerClass, int version) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build();
        schedule(ctx, workerClass, constraints, version);
    }

    public static <Worker extends SyncWorker>
    void schedule(Context ctx, Class<Worker> workerClass, Constraints constraints, int version) {

        // run for 1h every 1d to make sure we have all channel announcements
        // received and our graph updated
        PeriodicWorkRequest work =
                new PeriodicWorkRequest.Builder(workerClass,
                        WORK_INTERVAL, TimeUnit.MILLISECONDS)
                        .addTag(SyncWorkerImpl.getVersionTag(WORK_ID, version))
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                        .build();

        SyncWorkerImpl.schedule(ctx, work, WORK_ID, version);
    }

}
