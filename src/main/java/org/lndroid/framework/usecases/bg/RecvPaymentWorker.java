package org.lndroid.framework.usecases.bg;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkerParameters;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.lndroid.framework.client.IPluginClient;

public abstract class RecvPaymentWorker extends androidx.work.Worker {
    public static final String WORK_ID = "org.lndroid.framework.work.RECV_PAYMENT_WORK";
    private static final String TAG = "RecvPaymentWorker";

    private static final long MIN_SYNC_TIME = 3 * 60 * 1000; // 3 min
    private static final long MAX_SYNC_TIME = 6 * 60 * 1000; // 6 min
    private static final long WORK_INTERVAL = 15 * 60 * 1000; // 30 min
    private static final long FLEX_INTERVAL = 7 * 60 * 1000; // 10 min

    public RecvPaymentWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // NOTE: override to provide plugin client instance to the worker when it starts
    public abstract IPluginClient getPluginClient();
    public abstract ISyncNotificationManager getNotificationManager();

    @Override
    @NonNull
    public Result doWork() {
        Log.i(TAG, "starting");

        FutureTask<Result> f = new FutureTask<>(new Callable<Result>() {
            @Override
            public Result call() throws Exception {
                SyncWorkerImpl impl = new SyncWorkerImpl(getPluginClient(), TAG);

                ISyncNotificationManager nm = getNotificationManager();
                nm.showNotification(ISyncNotificationManager.SYNC_TYPE_RECV_PAYMENTS);

                Result r;
                if (impl.execute(MIN_SYNC_TIME, MAX_SYNC_TIME))
                    r = Result.success();
                else
                    r = Result.retry();

                nm.hideNotification(ISyncNotificationManager.SYNC_TYPE_RECV_PAYMENTS);
                return r;
            }
        });

        Thread t = new Thread(f);
        t.start();

        while (true) {
            try {
                Result r = f.get();
                Log.i(TAG, "done "+r);
                return r;
            } catch (ExecutionException e) {
                Log.e(TAG, "execution error " + e);
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                // ignore
            }
        }
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
