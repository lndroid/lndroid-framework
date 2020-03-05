package org.lndroid.framework.engine;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;

public interface IAuthClient {

    // subscribe to wallet state updates
    void subscribeWalletState(IResponseCallback<WalletData.WalletState> cb);

    // get info about how to perform user auth
    void getUserAuthInfo(long userId, IResponseCallback<WalletData.User> cb);

    // generate seed if wallet state is INIT
    void genSeed(WalletData.GenSeedRequest r, IResponseCallback<WalletData.GenSeedResponse> cb);

    // init wallet after genSeed if wallet state is INIT
    void initWallet(WalletData.InitWalletRequest r, IResponseCallback<WalletData.InitWalletResponse> cb);

    // unlock wallet if wallet state is AUTH
    void unlockWallet(WalletData.UnlockWalletRequest r, IResponseCallback<WalletData.UnlockWalletResponse> cb);

    // subscribe to get notifications about background auth requests
    void subscribeBackgroundAuthRequests(IResponseCallback<WalletData.AuthRequest> cb);

    // check if user has privileges to auth the tx
    //FIXME remove pluginId from here, authId should be enough
    void isUserPrivileged(String pluginId, long authUserId, long authId, IResponseCallback<Boolean> cb);

    // tell server about user's tx authorization results
    void authorize(WalletData.AuthResponse res, IResponseCallback<Boolean> cb);

    // get auth request, used by auth activities
    void getAuthRequest(long id, IResponseCallback<WalletData.AuthRequest> cb);

    // get auth tx request, used by auth activities
    <T> void getAuthTransactionRequest(long authId, Class<T> cls, IResponseCallback<T> cb);

    // supposedly would be used to display list of user txs,
    // but for now left for later!
//    <T> void getTransactionRequest(String pluginId, long userId, String txId, IResponseCallback<T> cb);

}
