package org.lndroid.framework.dao;

import androidx.annotation.Nullable;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface IAuthRequestDao {
    void init();
    @Nullable WalletData.AuthRequest get(long id);
    @Nullable WalletData.AuthRequest get(long userId, String txId);
    @Nullable WalletData.User getAuthRequestUser(long authRequestId);
    WalletData.AuthRequest insert(WalletData.AuthRequest r);
    void delete(long id);
    // bg requests are not persistent
    void deleteBackgroundRequests();
    List<WalletData.AuthRequest> getBackgroundRequests();
    <T> T getTransactionRequest(long userId, String txId, Class<T> cls);
}
