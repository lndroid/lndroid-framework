package org.lndroid.framework.dao;

import androidx.annotation.Nullable;

import org.lndroid.framework.WalletData;

public interface IAuthDao {
    void init();
    @Nullable WalletData.User get(long id);
    @Nullable WalletData.User getByAppPubkey(String pubkey);
    @Nullable WalletData.User getAuthInfo(long id);
}
