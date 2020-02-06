package org.lndroid.framework.lnd;

import android.util.Log;

import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.daemon.ILightningCallbackMT;
import org.lndroid.lnd.daemon.ILightningClient;
import org.lndroid.lnd.daemon.LightningDaemon;
import org.lndroid.lnd.daemon.LightningException;
import org.lndroid.lnd.data.Data;

import java.security.SecureRandom;
import java.util.ArrayList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.IResponseCallback;

public class LightningDao implements ILightningDao {

    private static final String TAG = "LightningDao";
    private ILightningClient client_;

    @Override
    public void init() {

    }

    @Override
    public boolean isStarted() {
        return LightningDaemon.isStarted();
    }

    @Override
    public boolean isUnlocked() {
        return LightningDaemon.isUnlocked();
    }

    @Override
    public boolean isUnlockReady() {
        return LightningDaemon.isUnlockReady();
    }

    @Override
    public boolean isRpcReady() {
        return LightningDaemon.isRpcReady();
    }

    @Override
    public void start(String dir,
                      final IResponseCallback<Object> unlockReadyCallback) throws LightningException {
        Log.i(TAG, "lnd dir " +dir);

        // since start is supposed to be exec-once, sync in this logic
        synchronized (this) {
            if (client_ == null) {
                Log.i(TAG, "creating lightning client on thread " + Thread.currentThread().getId());
                client_ = LightningDaemon.createClient();
            }
        }

        LightningDaemon.Init init = new LightningDaemon.Init();
//        init.noMacaroons = true;
        init.debugLevel = "info";
        init.dir = dir;
        init.acceptKeysend = true;
//        init.banDuration = "10m";
        init.connectPeers = new ArrayList<>();
        init.connectPeers.add("176.9.28.137:18333");

        // NOTE: start callbacks are called in daemon thread. We need
        // unlockReadyCallback to be called on this thread. We use client
        // that has built-in dispatcher, so we use it to create daemon callback
        // that will be called by delivering a message through client's handler.
        LightningDaemon.start(init, client_.createDaemonCallback(new ILightningCallback<Object>() {
            @Override
            public void onError(int i, String s) {
                Log.i(TAG, "unlock ready error " + i + " e " + s);
                unlockReadyCallback.onError(Errors.LND_ERROR, s);
            }

            @Override
            public void onResponse(Object o) {
                Log.i(TAG, "unlock ready");
                unlockReadyCallback.onResponse(o);
            }
        }), new ILightningCallbackMT() {
            @Override
            public void onError(int i, String s) {
                Log.i(TAG, "rpc ready error " + i + " e " + s);
            }

            @Override
            public void onResponse(Object o) {
                Log.i(TAG, "rpc ready");
            }
        });
    }

    @Override
    public void genSeed(WalletData.GenSeedRequest r, final IResponseCallback<WalletData.GenSeedResponse> cb) {
        if (!isUnlockReady())
            throw new RuntimeException("GenSeed needs unlocker ready");
        Data.GenSeedRequest req = new Data.GenSeedRequest();
        req.aezeedPassphrase = r.aezeedPassphrase;
        req.seedEntropy = r.seedEntropy;
        if (req.seedEntropy == null) {
            SecureRandom random = new SecureRandom();
            req.seedEntropy = new byte[16];
            random.nextBytes(req.seedEntropy);
        }

        client().genSeed(req, new ILightningCallback<Data.GenSeedResponse>() {
            @Override
            public void onResponse(Data.GenSeedResponse response) {
                WalletData.GenSeedResponse resp = new WalletData.GenSeedResponse();
                resp.cipherSeedMnemonic = response.cipherSeedMnemonic;
                cb.onResponse(resp);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "genSeed error "+i+" err "+s);
                cb.onError(Errors.WALLET_ERROR, Errors.errorMessage(Errors.WALLET_ERROR));
            }
        });
    }

    @Override
    public void initWallet(WalletData.InitWalletRequest r, final IResponseCallback<WalletData.InitWalletResponse> cb) {
        if (!isUnlockReady())
            throw new RuntimeException("InitWallet needs unlocker ready");
        Data.InitWalletRequest req = new Data.InitWalletRequest();
        req.aezeedPassphrase = r.aezeedPassphrase;
        req.cipherSeedMnemonic = r.cipherSeedMnemonic;
        req.walletPassword = r.walletPassword;
        client().initWallet(req, new ILightningCallback<Data.InitWalletResponse>() {
            @Override
            public void onResponse(Data.InitWalletResponse response) {
                cb.onResponse(new WalletData.InitWalletResponse());
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "initWallet error "+i+" err "+s);
                cb.onError(Errors.WALLET_ERROR, Errors.errorMessage(Errors.WALLET_ERROR));
            }
        });
    }

    private void onUnlock(IResponseCallback<WalletData.UnlockWalletResponse> cb) {
        Log.i(TAG, "unlocked");
        cb.onResponse(new WalletData.UnlockWalletResponse());
    }

    @Override
    public void unlockWallet(WalletData.UnlockWalletRequest r, final IResponseCallback<WalletData.UnlockWalletResponse> cb) {
        if (!isUnlockReady())
            throw new RuntimeException("Unlock needs unlocker ready");

        Data.UnlockWalletRequest req = new Data.UnlockWalletRequest();
        req.walletPassword = r.walletPassword;
        client().unlockWallet(req, new ILightningCallback<Data.UnlockWalletResponse>() {
            @Override
            public void onResponse(Data.UnlockWalletResponse unlockWalletResponse) {
                onUnlock(cb);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "unlockWallet error "+i+" err "+s);
                if (s.contains("transport is closing")) {
                    // ignore this error FIXME file an issue!
                    onUnlock(cb);
                } else if (s.contains("not found")) {
                    cb.onError(Errors.NO_WALLET, Errors.errorMessage(Errors.NO_WALLET));
                } else {
                    cb.onError(Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
                }
            }
        });
    }

    @Override
    public ILightningClient client() {
        if (!isStarted())
            throw new RuntimeException("Lightning not started");

        return client_;
    }
}
