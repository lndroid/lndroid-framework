package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

import java.util.List;

public interface ISendCoinsWorkerDao {
    List<WalletData.Transaction> getPendingTransactions();
    void updateTransaction(WalletData.Transaction t);
}
