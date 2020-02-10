package org.lndroid.framework.lnd;

import org.lndroid.lnd.daemon.ILightningClient;
import org.lndroid.lnd.daemon.LightningException;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IResponseCallback;

public interface ILightningDao {
    void init();

    boolean isStarted();
    boolean isUnlocked();
    boolean isUnlockReady();
    boolean isRpcReady();

    void start(String dir, IResponseCallback<Object> unlockReadyCallback) throws LightningException;

    void genSeed(WalletData.GenSeedRequest r, IResponseCallback<WalletData.GenSeedResponse> cb);
    void initWallet(WalletData.InitWalletRequest r, IResponseCallback<WalletData.InitWalletResponse> cb);
    void unlockWallet(WalletData.UnlockWalletRequest r, IResponseCallback<WalletData.UnlockWalletResponse> cb);

    ILightningClient client();
}
