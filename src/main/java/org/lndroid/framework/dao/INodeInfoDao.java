package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface INodeInfoDao {

    String getWalletPubkey();
    void updateNode(WalletData.LightningNode node);
    void updateChannels(List<WalletData.ChannelEdge> channels);
}
