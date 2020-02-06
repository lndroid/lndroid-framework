package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.plugins.Transaction;

public interface ISendPaymentDao {

    String walletPubkey();

    WalletData.Contact getContact(long contactId);

    boolean hasPrivilege(WalletData.SendPaymentRequest req, WalletData.User user);

    // get all active txs
    List<Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment>> getTransactions();

    // get tx
    Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> getTransaction(int txUserId, String txId);

    // start tx
    void startTransaction(Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> t);

    // write response to db (if required), attach response to tx, set to COMMITTED state,
    // resp.id will be initialized after this call.
    WalletData.Payment commitTransaction(int txUserId, String txId, int txAuthUserId, WalletData.Payment p);

    // set auth user/time, set to REJECTED state
    void rejectTransaction(int txUserId, String txId, int txAuthUserId);

    // mark as timed out, set to TX_TIMEOUT state
    void timeoutTransaction(int txUserId, String txId);
}
