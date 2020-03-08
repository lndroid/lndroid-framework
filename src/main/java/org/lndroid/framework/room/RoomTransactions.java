package org.lndroid.framework.room;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.TypeConverters;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.ITransactionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.plugins.Transaction;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class RoomTransactions {

    @Entity(
            primaryKeys = {"pluginId", "userId", "txId"},
            indices = {
                @Index({"pluginId", "state"}),
            }
    )
    static class RoomTransaction {
        @Embedded @NonNull
        public Transaction.TransactionData txData;
        @Embedded @NonNull
        public Transaction.JobData jobData;

        public void setResponse(Class<?> cls, long id) {
            txData.responseId = id;
            txData.responseClass = cls.getName();
        }
    }

    static class TransactionRequestBase<Request> {
        @PrimaryKey(autoGenerate = true)
        long id_;
        @Embedded
        Request data;
    }

    @Entity(tableName = "txAddUserRequest")
    static class AddUserRequest extends TransactionRequestBase<WalletData.AddUserRequest> {
    }

    @Entity(tableName = "txAddContactRequest")
    @TypeConverters({
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class AddContactRequest extends TransactionRequestBase<WalletData.AddContactRequest> {
    }

    @Entity(tableName = "txNewAddressRequest")
    static class NewAddressRequest extends TransactionRequestBase<WalletData.NewAddressRequest> {
    }

    @Entity(tableName = "txPayReqString")
    static class PayReqString extends TransactionRequestBase<String> {
    }

    @Entity(tableName = "txAddInvoiceRequest")
    static class AddInvoiceRequest extends TransactionRequestBase<WalletData.AddInvoiceRequest> {
    }

    @Entity(tableName = "txSendPaymentRequest")
    @TypeConverters({
            RoomConverters.DestTLVConverter.class,
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class SendPaymentRequest extends TransactionRequestBase<WalletData.SendPaymentRequest> {
    }

    @Entity(tableName = "txOpenChannelRequest")
    static class OpenChannelRequest extends TransactionRequestBase<WalletData.OpenChannelRequest> {
    }

    @Entity(tableName = "txCloseChannelRequest")
    static class CloseChannelRequest extends TransactionRequestBase<WalletData.CloseChannelRequest> {
    }

    @Entity(tableName = "txListContactsPrivilege")
    static class ListContactsPrivilege extends TransactionRequestBase<WalletData.ListContactsPrivilege> {
    }

    @Entity(tableName = "txContactPaymentsPrivilege")
    static class ContactPaymentsPrivilege extends TransactionRequestBase<WalletData.ContactPaymentsPrivilege> {
    }

    @Entity(tableName = "txConnectPeerRequest")
    static class ConnectPeerRequest extends TransactionRequestBase<WalletData.ConnectPeerRequest> {
    }

    @Entity(tableName = "txDisconnectPeerRequest")
    static class DisconnectPeerRequest extends TransactionRequestBase<WalletData.DisconnectPeerRequest> {
    }

    @Entity(tableName = "txShareContactRequest")
    static class ShareContactRequest extends TransactionRequestBase<WalletData.ShareContactRequest> {
    }

    @Entity(tableName = "txAddContactInvoiceRequest")
    static class AddContactInvoiceRequest extends TransactionRequestBase<WalletData.AddContactInvoiceRequest> {
    }

    @Entity(tableName = "txSendCoinsRequest")
    @TypeConverters({
            RoomConverters.ImmutableStringLongMapConverter.class,
            RoomConverters.ImmutableStringListConverter.class,
    })
    static class SendCoinsRequest extends TransactionRequestBase<WalletData.SendCoinsRequest> {
    }

    @Entity(tableName = "txEstimateFeeRequest")
    @TypeConverters({
            RoomConverters.ImmutableStringLongMapConverter.class,
    })
    static class EstimateFeeRequest extends TransactionRequestBase<WalletData.EstimateFeeRequest> {
    }

    @Entity(tableName = "txNotifiedInvoicesRequest")
    @TypeConverters({
            RoomConverters.ImmutableLongListConverter.class,
    })
    static class NotifiedInvoicesRequest extends TransactionRequestBase<WalletData.NotifiedInvoicesRequest> {
    }

    @Entity(tableName = "txSubscribeNewPaidInvoicesRequest")
    static class SubscribeNewPaidInvoicesRequest
            extends TransactionRequestBase<WalletData.SubscribeNewPaidInvoicesRequest> {
    }

    @Dao
    abstract static class TransactionDao implements ITransactionDao {
        @Override
        public void init() {}

        @Query("SELECT * FROM RoomTransaction WHERE pluginId = :pluginId AND state = 0")
        public abstract List<RoomTransaction> getTransactions(String pluginId);

        @Query("SELECT * FROM RoomTransaction WHERE pluginId = :pluginId "+
                "AND jobState = :state AND nextTryTime <= :now")
        public abstract List<RoomTransaction> getReadyJobTransactions(String pluginId, int state, long now);

        @Query("SELECT * FROM RoomTransaction WHERE jobState IN (:states)")
        public abstract List<RoomTransaction> getJobTransactions(int[] states);

        @Query("SELECT * FROM RoomTransaction WHERE pluginId = :pluginId AND userId = :userId AND txId = :txId")
        public abstract RoomTransactions.RoomTransaction getTransaction(String pluginId, long userId, String txId);

        @Query("SELECT requestId FROM RoomTransaction WHERE pluginId = :pluginId AND userId = :userId AND txId = :txId")
        public abstract long getTransactionRequestId(String pluginId, long userId, String txId);

        @Insert
        public abstract void createTransaction(RoomTransaction tx);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract void updateTransaction(RoomTransaction tx);

        @Query("UPDATE RoomTransaction " +
                "SET jobState = :jobState, maxTries = :maxTries, maxTryTime = :maxTryTime " +
                "WHERE pluginId = :pluginId AND userId = :userId AND txId = :txId")
        public abstract void initTransactionJob(String pluginId, long userId, String txId,
                                                int jobState, int maxTries, long maxTryTime);

        @Query("UPDATE RoomTransaction " +
                "SET authTime = :time, authUserId = :authUserId " +
                "WHERE pluginId = :pluginId AND userId = :userId AND txId = :txId")
        public abstract void confirmTransaction(String pluginId, long userId, String txId,
                                                long authUserId, long time);

        @Query("UPDATE RoomTransaction " +
                "SET state = :state, doneTime = :time, authTime = :time, authUserId = :authUserId " +
                "WHERE pluginId = :pluginId AND userId = :userId AND txId = :txId")
        public abstract void rejectTransaction(String pluginId, long userId, String txId,
                                               long authUserId, int state, long time);

        @Query("UPDATE RoomTransaction " +
                "SET state = :state, doneTime = :time, errorCode = :errorCode, errorMessage = :errorMessage " +
                "WHERE pluginId = :pluginId AND userId = :userId AND txId = :txId")
        public abstract void failTransaction(String pluginId, long userId, String txId,
                                             int state, long time, String errorCode, String errorMessage);

        @Query("UPDATE RoomTransaction " +
                "SET state = :state, doneTime = :time, " +
                "    responseClass = :responseClass, responseId = :responseId "+
                "WHERE pluginId = :pluginId AND userId = :userId AND txId = :txId")
        public abstract void commitTransaction(
                String pluginId, long userId, String txId,
                int state, long time,
                String responseClass, long responseId);

        @androidx.room.Transaction
        public void updateJob(String pluginId, long userId, String txId, Transaction.JobData job) {
            RoomTransactions.RoomTransaction tx = getTransaction(pluginId, userId, txId);
            tx.jobData = job;
            updateTransaction(tx);
        }

        @Override
        public <T> T getTransactionRequest(String pluginId, long userId, String txId, Class<T> cls) {
            if (pluginId == null)
                return null;

            RoomTransaction tx = getTransaction(pluginId, userId, txId);
            if (tx == null || tx.txData.requestId == 0)
                return null;

            final long id = tx.txData.requestId;

            TransactionRequestBase r = null;
            if (pluginId.equals(DefaultPlugins.ADD_USER))
                r = getAddUserRequest(id);
            else if (pluginId.equals(DefaultPlugins.NEW_ADDRESS))
                r = getNewAddressRequest(id);
            else if (pluginId.equals(DefaultPlugins.DECODE_PAYREQ))
                r = getDecodePayReqRequest(id);
            else if (pluginId.equals(DefaultPlugins.ADD_INVOICE))
                r = getAddInvoiceRequest(id);
            else if (pluginId.equals(DefaultPlugins.SEND_PAYMENT))
                r = getSendPaymentRequest(id);
            else if (pluginId.equals(DefaultPlugins.OPEN_CHANNEL))
                r = getOpenChannelRequest(id);
            else if (pluginId.equals(DefaultPlugins.ADD_APP_CONTACT))
                r = getAddAppContactRequest(id);
            else if (pluginId.equals(DefaultPlugins.ADD_CONTACT))
                r = getAddContactRequest(id);
            else if (pluginId.equals(DefaultPlugins.ADD_LIST_CONTACTS_PRIVILEGE))
                r = getAddListContactsPrivilegeRequest(id);
            else if (pluginId.equals(DefaultPlugins.ADD_CONTACT_PAYMENTS_PRIVILEGE))
                r = getAddContactPaymentsPrivilegeRequest(id);
            else if (pluginId.equals(DefaultPlugins.CONNECT_PEER))
                r = getConnectPeerRequest(id);
            else if (pluginId.equals(DefaultPlugins.DISCONNECT_PEER))
                r = getDisconnectPeerRequest(id);
            else if (pluginId.equals(DefaultPlugins.SHARE_CONTACT))
                r = getShareContactRequest(id);
            else if (pluginId.equals(DefaultPlugins.ADD_CONTACT_INVOICE))
                r = getAddContactInvoiceRequest(id);

            // r.data might be null if all fields of the instance were
            // null (in which case Room doesn't instanciate the fields)
            if (r == null)
                return null;

            if (r.data == null) {
                try {
                    Object builder = cls.getMethod("builder").invoke(null);
                    return (T)builder.getClass().getMethod("build").invoke(builder);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Tx request builder not supported");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Tx request builder failed");
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Tx request builder failed");
                }
            }

            if (!r.data.getClass().getName().equals(tx.txData.requestClass))
                return null;

            return (T)r.data;
        }

        @Query("SELECT * FROM txAddUserRequest WHERE id_ = :id")
        abstract RoomTransactions.AddUserRequest getAddUserRequest(long id);
        @Query("SELECT * FROM txNewAddressRequest WHERE id_ = :id")
        abstract RoomTransactions.NewAddressRequest getNewAddressRequest(long id);
        @Query("SELECT * FROM txPayReqString WHERE id_ = :id")
        abstract RoomTransactions.PayReqString getDecodePayReqRequest(long id);
        @Query("SELECT * FROM txAddInvoiceRequest WHERE id_ = :id")
        abstract RoomTransactions.AddInvoiceRequest getAddInvoiceRequest(long id);
        @Query("SELECT * FROM txSendPaymentRequest WHERE id_ = :id")
        abstract RoomTransactions.SendPaymentRequest getSendPaymentRequest(long id);
        @Query("SELECT * FROM txOpenChannelRequest WHERE id_ = :id")
        abstract RoomTransactions.OpenChannelRequest getOpenChannelRequest(long id);
        @Query("SELECT * FROM txAddContactRequest WHERE id_ = :id")
        abstract RoomTransactions.AddContactRequest getAddAppContactRequest(long id);
        @Query("SELECT * FROM txAddContactRequest WHERE id_ = :id")
        abstract RoomTransactions.AddContactRequest getAddContactRequest(long id);
        @Query("SELECT * FROM txListContactsPrivilege WHERE id_ = :id")
        abstract RoomTransactions.ListContactsPrivilege getAddListContactsPrivilegeRequest(long id);
        @Query("SELECT * FROM txContactPaymentsPrivilege WHERE id_ = :id")
        abstract RoomTransactions.ContactPaymentsPrivilege getAddContactPaymentsPrivilegeRequest(long id);
        @Query("SELECT * FROM txConnectPeerRequest WHERE id_ = :id")
        abstract RoomTransactions.ConnectPeerRequest getConnectPeerRequest(long id);
        @Query("SELECT * FROM txDisconnectPeerRequest WHERE id_ = :id")
        abstract RoomTransactions.DisconnectPeerRequest getDisconnectPeerRequest(long id);
        @Query("SELECT * FROM txShareContactRequest WHERE id_ = :id")
        abstract RoomTransactions.ShareContactRequest getShareContactRequest(long id);
        @Query("SELECT * FROM txAddContactInvoiceRequest WHERE id_ = :id")
        abstract RoomTransactions.AddContactInvoiceRequest getAddContactInvoiceRequest(long id);



    }

}
