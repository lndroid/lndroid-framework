package org.lndroid.framework.engine;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IAuthDao;
import org.lndroid.framework.dao.IAuthRequestDao;
import org.lndroid.framework.dao.IRawQueryDao;
import org.lndroid.framework.lnd.ILightningDao;

public interface IDaoProvider {

    interface IWalletStateCallback {
        void onWalletState(WalletData.WalletState state);
    }

    // first method called by server to get notified
    // when provider state changes, must be called
    // before 'init' to get notified about init result
    void subscribeWalletState(IWalletStateCallback cb);

    // in addition to unlocking these will also store the updated password,
    // server will then repeat calling 'init' so that dao would reuse the stored
    // password to decrypt the database
    void initWallet(WalletData.InitWalletRequest r, IResponseCallback<WalletData.InitWalletResponse> cb);
    void unlockWallet(WalletData.UnlockWalletRequest r, IResponseCallback<WalletData.UnlockWalletResponse> cb);

    // open databases and unlock lnd,
    // if state!=STATE_OK server+UI need to deal with it (initWallet/unlockWallet),
    // and then plugin server will retry
    void init();

    // these will be used while wallet state is OK
    IAuthDao getAuthDao();
    IAuthRequestDao getAuthRequestDao();
    ILightningDao getLightningDao();
    IRawQueryDao getRawQueryDao();
    IPluginDao getPluginDao(String pluginId);
}
