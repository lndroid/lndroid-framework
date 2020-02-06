package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface ISendPaymentWorkerDao {

    List<WalletData.SendPayment> getSendingPayments();
    List<WalletData.SendPayment> getPendingPayments(long now);
    WalletData.Contact getContact(String contactPubkey);
    void updatePayment(WalletData.SendPayment p);
    void settlePayment(WalletData.SendPayment sp, WalletData.HTLCAttempt htlc);

}
