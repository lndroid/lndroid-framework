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
        public String txError; // error code
    }

    static class RoomTransactionBase<Request, Response> implements IRoomTransaction<Request, Response> {
        @Embedded @NonNull
        public TransactionData txData;
        @Embedded(prefix="req")
        public Request request;
        @Embedded(prefix="rep")
        public Response response;

        @Override
        public Request getRequest() {
            return request;
        }

        @Override
        public Response getResponse() {
            return response;
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
        public void setResponse(Response r) {
            response = r;
        }

        @Override
        public void setTxData(TransactionData t) {
            txData = t;
        }
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddUserTransaction extends RoomTransactionBase<WalletData.AddUserRequest, WalletData.User> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class NewAddressTransaction extends RoomTransactionBase<WalletData.NewAddressRequest, WalletData.NewAddress> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.DestTLVConverter.class,
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class DecodePayReqTransaction extends RoomTransactionBase<String, WalletData.SendPayment> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class AddInvoiceTransaction extends RoomTransactionBase<WalletData.AddInvoiceRequest, WalletData.Invoice> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.DestTLVConverter.class,
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class SendPaymentTransaction extends RoomTransactionBase<WalletData.SendPaymentRequest, WalletData.SendPayment> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class OpenChannelTransaction extends RoomTransactionBase<WalletData.OpenChannelRequest, WalletData.Channel> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class AddContactTransaction extends RoomTransactionBase<WalletData.Contact, WalletData.Contact> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddListContactsPrivilegeTransaction
            extends RoomTransactionBase<WalletData.ListContactsPrivilege, WalletData.ListContactsPrivilege> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddContactPaymentsPrivilegeTransaction
            extends RoomTransactionBase<WalletData.ContactPaymentsPrivilege, WalletData.ContactPaymentsPrivilege> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class ConnectPeerTransaction
            extends RoomTransactionBase<WalletData.ConnectPeerRequest, WalletData.ConnectPeerResponse> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class ShareContactTransaction
            extends RoomTransactionBase<WalletData.ShareContactRequest, WalletData.ShareContactResponse> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    static class AddContactInvoiceTransaction
            extends RoomTransactionBase<WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.ImmutableStringLongMapConverter.class,
            RoomConverters.ImmutableStringListConverter.class,
    })
    static class SendCoinsTransaction
            extends RoomTransactionBase<WalletData.SendCoinsRequest, WalletData.Transaction> {
    }

    @Entity(primaryKeys = {"txUserId", "txId"})
    @TypeConverters({
            RoomConverters.ImmutableStringLongMapConverter.class,
    })
    static class EstimateFeeTransaction extends RoomTransactionBase<WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse> {
    }


}
