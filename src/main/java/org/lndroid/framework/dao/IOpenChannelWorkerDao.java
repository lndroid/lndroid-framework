package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface IOpenChannelWorkerDao {

    List<WalletData.Channel> getOpeningChannels();
    List<WalletData.Channel> getRetryChannels();
    List<WalletData.Channel> getNewChannels();
    void updateChannel(WalletData.Channel c);
}
