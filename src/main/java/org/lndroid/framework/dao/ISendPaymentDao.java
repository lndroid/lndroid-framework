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
    Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> getTransaction(long txUserId, String txId);

    // start tx
    void startTransaction(Transaction<WalletData.SendPaymentRequest, WalletData.SendPayment> t);

    // write response to db (if required), attach response to tx, set to COMMITTED state,
    // resp.id will be initialized after this call.
    WalletData.Payment commitTransaction(long txUserId, String txId, long txAuthUserId, WalletData.Payment p);

    // set auth user/time, set to REJECTED state
    void rejectTransaction(long txUserId, String txId, long txAuthUserId);

    // mark as timed out, set to TX_TIMEOUT state
    void timeoutTransaction(long txUserId, String txId);
}
