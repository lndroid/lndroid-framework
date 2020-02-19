package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

import java.util.List;

public interface IChannelStateWorkerDao {
    WalletData.Channel getChannelByChannelPoint(String channelPoint);
    List<WalletData.Channel> getOpeningChannels();
    void updateChannel(WalletData.Channel c);
    void setChannelActive(String channelPoint, boolean active);
}
