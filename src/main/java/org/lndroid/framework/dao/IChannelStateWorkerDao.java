package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IChannelStateWorkerDao {
    WalletData.Channel getChannelByChannelPoint(String channelPoint);
    void updateChannel(WalletData.Channel c);
    void setChannelActive(String channelPoint, boolean active);
}
