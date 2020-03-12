package org.lndroid.framework.usecases.bg;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.usecases.GetDataBg;

public class SyncWorkerImpl {

    private IPluginClient client_;
    private String tag_;
    private boolean error_;
    private boolean restart_;

    public SyncWorkerImpl(IPluginClient client, String tag) {
        client_ = client;
        tag_ = tag;
    }

    private void stopLooper() {
        Log.i(tag_, "stopping");
        Looper.myLooper().quit();
    }

    static class GetWalletInfoBg extends GetDataBg<WalletData.WalletInfo, Long> {

        public GetWalletInfoBg(IPluginClient client, String pluginId) {
            super(client, pluginId);
        }

        @Override
        protected WalletData.WalletInfo getData(IPluginData in) {
            in.assignDataType(WalletData.WalletInfo.class);
            try {
                return in.getData();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected Type getRequestType() {
            return WalletData.GetRequestLong.class;
        }
    }

    private boolean run(final long startTime, final long minTime, final long maxTime) {
        // reset
        error_ = false;
        restart_ = false;

        // subscribe to wallet info
        GetWalletInfoBg info = new GetWalletInfoBg(client_, DefaultPlugins.GET_WALLET_INFO);
        WalletData.GetRequestLong r = WalletData.GetRequestLong.builder()
                .setNoAuth(true)
                .setSubscribe(true)
                .build();
        info.setRequest(r);

        // on each wallet info update, check if we're synched,
        // if not - extend work time, if synched and worked enough - exit looper
        info.setCallback(new IResponseCallback<WalletData.WalletInfo>() {
            @Override
            public void onResponse(WalletData.WalletInfo r) {
                // might happen on a new wallet
                if (r == null)
                    return;
                boolean synched = r.syncedToChain() && r.syncedToGraph();
                Log.i(tag_, "synched " + synched);
                long workTime = synched ? minTime : maxTime;
                final long stopTime = startTime + workTime;
                if (System.currentTimeMillis() > stopTime)
                    stopLooper();
            }

            @Override
            public void onError(String code, String e) {
                // NOTE that we should end up here if wallet is locked
                // so that worker doesn't burn cycles w/o a reason
                Log.e(tag_, "get info failed " + code + " err " + e);
                restart_ = Errors.TX_TIMEOUT.equals(code);
                error_ = !restart_;
                stopLooper();
            }
        });
        info.start();

        // start looping
        Looper.loop();

        // clean up
        info.destroy();

        return !restart_;
    }

    public boolean execute(final long minTime, final long maxTime) {

        // ensure looper exists in our thread
        Log.i(tag_, "starting on thread " + Thread.currentThread().getId());
        if (Looper.myLooper() == null)
            Looper.prepare();

        // store start time
        final long startTime = System.currentTimeMillis();

        // listen to the wallet state until minTime of
        // listening has passed
        while (!run(startTime, minTime, maxTime)) {
            // repeat
        }

        return !error_;
    }

    public static String getVersionTag(String workId, int version) {
        return workId + ".v"+version;
    }

    public static void schedule(Context ctx, PeriodicWorkRequest work, String workId, int version) {

        WorkManager wm = WorkManager.getInstance(ctx);

        // cancel all work up to current version, to allow
        // client code to update the schedule if it changes
        // interval settings or constraints
        for (int v = 0; v < version; v++)
            wm.cancelAllWorkByTag(getVersionTag(workId, version));

        // KEEP existing work (w/ same version) to not break the schedule,
        // otherwise SyncWorker will start immediately on every wallet launch
        wm.enqueueUniquePeriodicWork(workId, ExistingPeriodicWorkPolicy.KEEP, work);
    }

}
