package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

import java.util.List;

public interface ISendCoinsWorkerDao {
    List<WalletData.Transaction> getNewTransactions();
    List<WalletData.Transaction> getSendingTransactions();
    List<WalletData.Transaction> getRetryTransactions();
    void updateTransaction(WalletData.Transaction t);
}
