package org.lndroid.framework.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.TypeConverters;

import org.lndroid.framework.WalletData;

public class RoomTransactions {

    // to be embedded to all transactions
    static class TransactionData {
        public long txUserId;
        @NonNull
        public String txId;

        public long txAuthUserId;
        public int txState; // plugins.Transaction.STATES
        public long txCreateTime;
        public long txDeadlineTime;
        public long txAuthTime;
        public long txDoneTime;

        @Nullable
        public String txErrorCode;
        @Nullable
        public String txErrorMessage;


        public String responseClass;
        public long responseId;


        // current number of tries
        public int tries;

        // max number of tries
        public int maxTries;

        // deadline for retries, in ms
        public long maxTryTime;

        // last time we retried, in ms
        public long lastTryTime;

        // next time we'll retry, in ms
        public long nextTryTime;

        // see above
        public int jobState;

        // error code if state=failed
        public String jobErrorCode;

        // error message
        public String jobErrorMessage;


    }

    static class RoomTransactionBase<Request> implements IRoomTransaction<Request> {
        @Embedded @NonNull
        public TransactionData txData;
        @Embedded(prefix="req")
        public Request request;

        @Override
        public Request getRequest() {
            return request;
        }

        @Override
        public TransactionData getTxData() {
            return txData;
        }

        @Override
        public void setRequest(Request r) {
            request = r;
        }

        @Override
        public void setResponse(Class<?> cls, long id) {
            txData.responseId = id;
            txData.responseClass = cls.getName();
        }

        @Override
        public void setTxData(TransactionData t) {
            txData = t;
        }
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddUserTransaction extends RoomTransactionBase<WalletData.AddUserRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class NewAddressTransaction extends RoomTransactionBase<WalletData.NewAddressRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class DecodePayReqTransaction extends RoomTransactionBase<String> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddInvoiceTransaction extends RoomTransactionBase<WalletData.AddInvoiceRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.DestTLVConverter.class,
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class SendPaymentTransaction extends RoomTransactionBase<WalletData.SendPaymentRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class OpenChannelTransaction extends RoomTransactionBase<WalletData.OpenChannelRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class CloseChannelTransaction extends RoomTransactionBase<WalletData.CloseChannelRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class AddContactTransaction extends RoomTransactionBase<WalletData.Contact> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddListContactsPrivilegeTransaction
            extends RoomTransactionBase<WalletData.ListContactsPrivilege> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddContactPaymentsPrivilegeTransaction
            extends RoomTransactionBase<WalletData.ContactPaymentsPrivilege> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class ConnectPeerTransaction
            extends RoomTransactionBase<WalletData.ConnectPeerRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class ShareContactTransaction
            extends RoomTransactionBase<WalletData.ShareContactRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddContactInvoiceTransaction
            extends RoomTransactionBase<WalletData.AddContactInvoiceRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.ImmutableStringLongMapConverter.class,
            RoomConverters.ImmutableStringListConverter.class,
    })
    static class SendCoinsTransaction
            extends RoomTransactionBase<WalletData.SendCoinsRequest> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.ImmutableStringLongMapConverter.class,
    })
    static class EstimateFeeTransaction extends RoomTransactionBase<WalletData.EstimateFeeRequest> {
    }


}
