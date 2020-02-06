package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IGetContactDao {
    WalletData.Contact get(long id);
}
