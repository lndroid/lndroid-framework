package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IGetSendPaymentDao {
    WalletData.SendPayment get(long id);
}
