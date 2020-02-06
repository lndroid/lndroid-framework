package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IGetChannelDao {
    WalletData.Channel get(long id);
}
