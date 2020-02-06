package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IWalletBalanceDao {
    WalletData.WalletBalance get();
    void update(WalletData.WalletBalance b);
}
