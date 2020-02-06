package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IChannelBalanceDao {
    WalletData.ChannelBalance get();
    void update(WalletData.ChannelBalance b);
}
