package org.lndroid.framework.dao;

import androidx.annotation.Nullable;

import org.lndroid.framework.WalletData;

public interface IAuthDao {
    void init();
    @Nullable WalletData.User get(int id);
    @Nullable WalletData.User getByAppPubkey(String pubkey);
}
