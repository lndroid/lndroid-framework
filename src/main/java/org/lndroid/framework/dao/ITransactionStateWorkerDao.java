package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

import java.util.List;

public interface ITransactionStateWorkerDao {
    WalletData.Transaction getTransaction(String txHash);
    List<WalletData.Transaction> getPendingOrLostTransactions();
    void updateTransaction(WalletData.Transaction t);
}
