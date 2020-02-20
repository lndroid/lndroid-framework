package org.lndroid.framework.room;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IAuthRequestDao;
import org.lndroid.framework.engine.IPluginDao;

class AuthRequestDao implements IAuthRequestDao, IPluginDao {

    private AuthRequestDaoRoom dao_;

    AuthRequestDao(AuthRequestDaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public WalletData.AuthRequest get(long id) {
        RoomData.AuthRequest r = dao_.get(id);
        return r != null ? r.getData() : null;
    }

    @Override
    public WalletData.AuthRequest get(long userId, String txId) {
        RoomData.AuthRequest r = dao_.get(userId, txId);
        return r != null ? r.getData() : null;
    }

    @Nullable
    @Override
    public WalletData.User getAuthRequestUser(long authRequestId) {
        RoomData.User r = dao_.getAuthRequestUser(authRequestId);
        return r != null ? r.getData() : null;
    }

    @Override
    public WalletData.AuthRequest insert(WalletData.AuthRequest r) {
        RoomData.AuthRequest d = new RoomData.AuthRequest();
        d.setData(r);
        dao_.insert(d);
        return r;
    }

    @Override
    public void delete(long id) {
        dao_.delete(id);
    }

    @Override
    public void deleteBackgroundRequests() {
        dao_.deleteBackgroundRequests();
    }

    @Override
    public List<WalletData.AuthRequest> getBackgroundRequests() {
        List<WalletData.AuthRequest> r = new ArrayList<>();
        for (RoomData.AuthRequest ar: dao_.getBackgroundRequests())
            r.add(ar.getData());
        return r;
    }

    private <T, TX> boolean isTxRequestClass(Class<T> cls, Class<TX> tx) {
        Type[] tv = ((ParameterizedType)tx.getGenericSuperclass()).getActualTypeArguments();
        return cls.equals(tv[0]);
    }

    @Override
    public <T> T getTransactionRequest(long userId, String txId, Class<T> cls) {
        RoomTransactions.RoomTransactionBase<?> tx = null;
        if (isTxRequestClass(cls, RoomTransactions.AddUserTransaction.class))
            tx = dao_.getAddUserTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.NewAddressTransaction.class))
            tx = dao_.getNewAddressTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.DecodePayReqTransaction.class))
            tx = dao_.getDecodePayReqTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.AddInvoiceTransaction.class))
            tx = dao_.getAddInvoiceTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.SendPaymentTransaction.class))
            tx = dao_.getSendPaymentTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.OpenChannelTransaction.class))
            tx = dao_.getOpenChannelTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.AddContactTransaction.class))
            tx = dao_.getAddContactTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.AddListContactsPrivilegeTransaction.class))
            tx = dao_.getAddListContactsPrivilegeTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.AddContactPaymentsPrivilegeTransaction.class))
            tx = dao_.getAddContactPaymentsPrivilegeTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.ConnectPeerTransaction.class))
            tx = dao_.getConnectPeerTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.ShareContactTransaction.class))
            tx = dao_.getShareContactTransaction(userId, txId);
        else if (isTxRequestClass(cls, RoomTransactions.AddContactInvoiceTransaction.class))
            tx = dao_.getAddContactInvoiceTransaction(userId, txId);

        if (tx != null)
            return (T)tx.request;

        return null;
    }
}

@Dao
abstract class AuthRequestDaoRoom {
    @Query("SELECT * FROM AuthRequest WHERE id = :id")
    abstract RoomData.AuthRequest get(long id);

    @Query("SELECT * FROM AuthRequest WHERE userId = :userId AND txId = :txId")
    abstract RoomData.AuthRequest get(long userId, String txId);

    @Query("SELECT * FROM User WHERE id = (SELECT userId FROM AuthRequest WHERE id = :authRequestId)")
    abstract RoomData.User getAuthRequestUser(long authRequestId);

    @Insert
    abstract void insert(RoomData.AuthRequest r);

    @Query("DELETE FROM AuthRequest WHERE id = :id")
    abstract void delete(long id);

    @Query("DELETE FROM AuthRequest WHERE background != 0")
    abstract void deleteBackgroundRequests();

    @Query("SELECT * FROM AuthRequest WHERE background != 0")
    abstract List<RoomData.AuthRequest> getBackgroundRequests();

    // FIXME it is not a great place to put these
    @Query("SELECT * FROM AddUserTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.AddUserTransaction getAddUserTransaction(long userId, String txId);

    @Query("SELECT * FROM NewAddressTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.NewAddressTransaction getNewAddressTransaction(long userId, String txId);

    @Query("SELECT * FROM DecodePayReqTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.DecodePayReqTransaction getDecodePayReqTransaction(long userId, String txId);

    @Query("SELECT * FROM AddInvoiceTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.AddInvoiceTransaction getAddInvoiceTransaction(long userId, String txId);

    @Query("SELECT * FROM SendPaymentTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.SendPaymentTransaction getSendPaymentTransaction(long userId, String txId);

    @Query("SELECT * FROM OpenChannelTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.OpenChannelTransaction getOpenChannelTransaction(long userId, String txId);

    @Query("SELECT * FROM AddContactTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.AddContactTransaction getAddContactTransaction(long userId, String txId);

    @Query("SELECT * FROM AddListContactsPrivilegeTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.AddListContactsPrivilegeTransaction getAddListContactsPrivilegeTransaction(long userId, String txId);

    @Query("SELECT * FROM AddContactPaymentsPrivilegeTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.AddContactPaymentsPrivilegeTransaction getAddContactPaymentsPrivilegeTransaction(long userId, String txId);

    @Query("SELECT * FROM ConnectPeerTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.ConnectPeerTransaction getConnectPeerTransaction(long userId, String txId);

    @Query("SELECT * FROM ShareContactTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.ShareContactTransaction getShareContactTransaction(long userId, String txId);

    @Query("SELECT * FROM AddContactInvoiceTransaction WHERE txUserId = :userId AND txId = :txId")
    abstract RoomTransactions.AddContactInvoiceTransaction getAddContactInvoiceTransaction(long userId, String txId);


}
