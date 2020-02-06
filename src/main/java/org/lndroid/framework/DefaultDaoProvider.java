package org.lndroid.framework;

import android.util.Log;

import org.lndroid.lnd.daemon.LightningException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.lndroid.framework.dao.IAuthDao;
import org.lndroid.framework.dao.IAuthRequestDao;
import org.lndroid.framework.room.RoomDaoProvider;
import org.lndroid.framework.engine.IDaoProvider;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningDao;

public class DefaultDaoProvider implements IDaoProvider {

    private static final String TAG = "DefaultDaoProvider";
    private static final int MAX_PASSWORD_SIZE = 4096;

    private IDaoConfig config_;
    private LightningDao lightningDao_;
    private List<IWalletStateCallback> walletStateCallbacks_ = new ArrayList<>();
    private boolean unlocked_;
    private WalletData.WalletState state_;
    private RoomDaoProvider roomDaos_;
    private byte[] updatedPassword_;

    public DefaultDaoProvider(IDaoConfig cfg) {
        config_ = cfg;
        roomDaos_ = new RoomDaoProvider(cfg);
    }

    private String getPasswordFile() {
        return config_.getDataPath() + "/" + config_.getPasswordFileName();
    }

    private byte[] readEncryptedPassword() {
        try {
            FileInputStream s = new FileInputStream(getPasswordFile());
            byte[] buffer = new byte[MAX_PASSWORD_SIZE];
            final int r = s.read(buffer);
            if (r > 0)
                return Arrays.copyOf(buffer, r);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private byte[] decryptPassword(byte[] p) {
        if (p.length < 3)
            return null;

        final int ivSize = p[0];
        if ((ivSize + 2) > p.length)
            return null;

        // parse p into iv and data: iv_size+iv+data
        IKeyStore.EncryptedData ed = new IKeyStore.EncryptedData();
        ed.iv = Arrays.copyOfRange(p, 1, 1 + ivSize);
        ed.data = Arrays.copyOfRange(p, 1 + ivSize, p.length);

        // decrypt using wallet password key
        return config_.getKeyStore().decryptWalletPassword(ed);
    }

    private byte[] encryptPassword(byte[] p) {
        // encrypt using keystore
        IKeyStore.EncryptedData ed = config_.getKeyStore().encryptWalletPassword(p);
        if (ed.iv.length > 127)
            throw new RuntimeException("IV too big");

        // serialize: iv_size+iv+data
        byte[] e = new byte[1 + ed.data.length + ed.iv.length];
        e[0] = (byte) ed.iv.length;
        for (int i = 0; i < ed.iv.length; i++)
            e[1 + i] = ed.iv[i];
        for (int i = 0; i < ed.data.length; i++)
            e[1 + ed.iv.length + i] = ed.data[i];

//        Log.i(TAG, "p "+ LightningCodec.bytesToHex(p)+
//                " ed iv "+LightningCodec.bytesToHex(ed.iv)+
//                " data "+LightningCodec.bytesToHex(ed.data)+
//                " enc "+LightningCodec.bytesToHex(e));

        return e;
    }

    private void writeWalletPassword(byte[] password) {
        // encrypt using wallet password key
        byte[] encPassword = encryptPassword(password);

        File file = new File(getPasswordFile());

        try {
            FileOutputStream f = new FileOutputStream(file);
            f.write(encPassword);
            f.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setWalletPassword(byte[] password) {
        // store until 'init' is retried, to decrypt database
        updatedPassword_ = password;

        // no need to unlock when 'init' is retried
        unlocked_ = true;

        // update state
        state_ = WalletData.WalletState.builder()
                .setState(WalletData.WALLET_STATE_OK)
                .build();

        writeWalletPassword(password);
    }

    private boolean deleteDir(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDir(file);
            }
        }
        return dir.delete();
    }

    private String getLndDir() {
        return config_.getDataPath()+"/"+config_.getLndDirName();
//        return config_.getFilesPath() + "/" + config_.getLndDirName();
    }

    private void cleanData() {
        String dbPath = config_.getDatabasePath();
        Log.i(TAG, "clean up database path " + dbPath);
        Log.i(TAG, "clean up lnd path " + getLndDir());
//        FIXME delete

        File dataDir = new File(config_.getDataPath());
        dataDir.mkdirs();
    }

    @Override
    public void subscribeWalletState(IWalletStateCallback cb) {
        walletStateCallbacks_.add(cb);
        if (state_ != null)
            cb.onWalletState(state_);
    }

    private void startLndExt(IResponseCallback<Object> unlockReadyCallback) {
        lightningDao_ = new LightningDao();
        try {
            lightningDao_.start(getLndDir(), unlockReadyCallback);
        } catch (LightningException e) {
            throw new RuntimeException(e);
        }
    }

    private void startLnd(final byte[] password) {
        startLndExt(new IResponseCallback<Object>() {
            @Override
            public void onResponse(Object r) {
                // unlock lnd in bg if it's not already explicitly unlocked
                // after initWallet/unlockWallet calls
                if (!unlocked_ && password != null)
                    unlockLndAsync(password);
            }

            @Override
            public void onError(String code, String e) {
                Log.e(TAG, "unlockReady error "+code+" e "+e);
            }
        });
    }

    private void notifyWalletState() {
        for(IWalletStateCallback cb: walletStateCallbacks_) {
            cb.onWalletState(state_);
        }
    }

    private void unlockLndAsync(byte[] password) {
        WalletData.UnlockWalletRequest r = new WalletData.UnlockWalletRequest();
        r.walletPassword = password;
        lightningDao_.unlockWallet(r, new IResponseCallback<WalletData.UnlockWalletResponse>() {
            @Override
            public void onResponse(WalletData.UnlockWalletResponse r) {
                Log.i(TAG, "lnd unlock ok");
                state_ = WalletData.WalletState.builder()
                        .setState(WalletData.WALLET_STATE_OK)
                        .build();
                notifyWalletState();
            }

            @Override
            public void onError(String code, String s) {
                Log.e(TAG, "lnd unlock failed "+code+" msg "+s+" "+new Date());
                state_ = WalletData.WalletState.builder()
                        .setState(WalletData.WALLET_STATE_ERROR)
                        .setCode(code)
                        .setMessage(s)
                        .build();
                notifyWalletState();
            }
        });
    }

    private class InitUnlockCallback<Response> implements IResponseCallback<Response> {

        private byte[] password_;
        private IResponseCallback<Response> cb_;

        InitUnlockCallback(byte[] password, IResponseCallback<Response> cb) {
            password_ = password;
            cb_ = cb;
        }

        @Override
        public void onResponse(Response r) {
            Log.i(TAG, "init/unlock ok");

            // write password
            setWalletPassword(password_);

            // exec server unlock/init callback first
            cb_.onResponse(r);

            // notify server second
            notifyWalletState();
        }

        @Override
        public void onError(String code, String err) {
            Log.i(TAG, "init/unlock error "+code+" msg "+err);

            state_ = WalletData.WalletState.builder()
                    .setState(WalletData.WALLET_STATE_ERROR)
                    .setCode(code)
                    .setMessage(err)
                    .build();

            // pass error
            cb_.onError(code, err);

            // notify new state
            notifyWalletState();
        }
    }

    @Override
    public void initWallet(WalletData.InitWalletRequest r, IResponseCallback<WalletData.InitWalletResponse> cb) {
        getLightningDao().initWallet(r, new InitUnlockCallback<>(r.walletPassword, cb));
    }

    @Override
    public void unlockWallet(WalletData.UnlockWalletRequest r, IResponseCallback<WalletData.UnlockWalletResponse> cb) {
        getLightningDao().unlockWallet(r, new InitUnlockCallback<>(r.walletPassword, cb));
    }

    // NOTE: this call might be called twice,
    // once for initial try (succeeds if wallet exists and password is available),
    // and then after initWallet/unlockWallet was called,
    // so code should handle that properly
    @Override
    public void init() {

        // ensure keystore is initialized
        config_.getKeyStore().init();

        // try to use user-provided password first
        byte[] password = updatedPassword_;
        updatedPassword_ = null; // don't store it any longer

        // try to get password from encrypted store
        if (password == null) {

            // do we have password stored?
            final byte[] encPassword = readEncryptedPassword();
            if (encPassword == null) {
                // clean-up state
                cleanData();
                // start lnd to make it usable for genSeed
                startLnd(null);
                // inform server that we need INIT
                state_ = WalletData.WalletState.builder()
                        .setState(WalletData.WALLET_STATE_INIT)
                        .build();
                notifyWalletState();
                return;
            }
//            Log.i(TAG, "encPassword " + LightningCodec.bytesToHex(encPassword));

            // do we have keys to decrypt the password?
            password = decryptPassword(encPassword);
            if (password == null) {
                // start lnd to make it usable for unlockWallet
                startLnd(null);
                // inform server that we need AUTH
                state_ = WalletData.WalletState.builder()
                        .setState(WalletData.WALLET_STATE_AUTH)
                        .build();
                notifyWalletState();
                return;
            }
        }

        // as we'll need it for init/unlock
        // ready to start db
        roomDaos_.init(config_.getDatabaseName(), password);

        // ensure lnd daemon is started,
        startLnd(password);

        // tell server we're ready.
        // NOTE: unlock might turn state to ERROR later,
        // server should handle that properly
        state_ = WalletData.WalletState.builder()
                .setState(WalletData.WALLET_STATE_OK)
                .build();
        notifyWalletState();
    }

    @Override
    public IAuthDao getAuthDao() {
        return roomDaos_.getAuthDao();
    }

    @Override
    public IAuthRequestDao getAuthRequestDao() {
        return roomDaos_.getAuthRequestDao();
    }

    @Override
    public ILightningDao getLightningDao() {
        return lightningDao_;
    }

    @Override
    public IPluginDao getPluginDao(String pluginId) {
        return roomDaos_.getPluginDao(pluginId);
    }
}
