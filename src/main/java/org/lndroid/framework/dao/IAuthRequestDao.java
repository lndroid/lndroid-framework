package org.lndroid.framework.dao;

import androidx.annotation.Nullable;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface IAuthRequestDao {
    void init();
    @Nullable WalletData.AuthRequest get(int id);
    @Nullable WalletData.AuthRequest get(int userId, String txId);
    @Nullable WalletData.User getAuthRequestUser(int authRequestId);
    WalletData.AuthRequest insert(WalletData.AuthRequest r);
    void delete(int id);
    // bg requests are not persistent
    void deleteBackgroundRequests();
    List<WalletData.AuthRequest> getBackgroundRequests();
    <T> T getTransactionRequest(int userId, String txId, Class<T> cls);
}
