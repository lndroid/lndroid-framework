package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface ISubscribeSendPaymentsDao {
    WalletData.SendPayment getPayment(long id);
    List<WalletData.SendPayment> getActivePayments(long userId);
}
