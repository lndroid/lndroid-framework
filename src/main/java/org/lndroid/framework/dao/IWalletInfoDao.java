package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IWalletInfoDao {
    WalletData.WalletInfo get();
    void update(WalletData.WalletInfo r);
}
