package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ChannelBackupWorker implements IPluginBackground {
    private static final String TAG = "ChannelBackupWorker";

    private IPluginServer server_;
    private ILightningDao lnd_;
    private IPluginBackgroundCallback engine_;
    private boolean started_;

    @Override
    public String id() {
        return DefaultPlugins.INVOICE_STATE_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        server_ = server;
        lnd_ = server.getDaoProvider().getLightningDao();
        engine_ = callback;
    }

    private void onUpdate(Data.ChanBackupSnapshot r) {
        if (server_.getDaoConfig().getBackupPath() == null) {
            Log.w(TAG, "no backup path");
            return;
        }

        try {
            File file = new File(server_.getDaoConfig().getBackupPath()+"~");

            FileOutputStream f = new FileOutputStream(file);
            f.write(r.multiChanBackup.multiChanBackup);
            f.close();

            file.renameTo(new File (server_.getDaoConfig().getBackupPath()));

        } catch (Exception e) {
            Log.e(TAG, "writing channel backup error "+e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public void work() {
        if (!lnd_.isRpcReady())
            return;

        if (started_)
            return;

        started_ = true;

        Data.ChannelBackupSubscription s = new Data.ChannelBackupSubscription();
        lnd_.client().subscribeChannelBackupsStream(s, new ILightningCallback<Data.ChanBackupSnapshot>() {

            @Override
            public void onResponse(Data.ChanBackupSnapshot b) {
                Log.i(TAG, "subscribe channel backup snapshot");
                onUpdate(b);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "subscribe channel backup error "+i+" err "+s);
                throw new RuntimeException("SubscribeChannelBackups failed");
            }
        });
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
        // noop
    }

    @Override
    public void notify(String topic, Object data) {
        // noop
    }
}

