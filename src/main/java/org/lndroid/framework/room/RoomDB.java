package org.lndroid.framework.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 61, exportSchema = true, entities = {
        RoomData.AuthRequest.class,
        RoomData.Channel.class,
        RoomData.ChannelBalance.class,
        RoomData.ChannelEdge.class,
        RoomData.Contact.class,
        RoomData.ContactPaymentsPrivilege.class,
        RoomData.Invoice.class,
        RoomData.InvoiceHTLC.class,
        RoomData.HopHint.class,
        RoomData.HTLCAttempt.class,
        RoomData.LightningNode.class,
        RoomData.ListContactsPrivilege.class,
        RoomData.NextId.class,
        RoomData.SendPayment.class,
        RoomData.Payment.class,
        RoomData.RoutingPolicy.class,
        RoomData.RouteHint.class,
        RoomData.WalletBalance.class,
        RoomData.WalletInfo.class,
        RoomData.User.class,
        RoomTransactions.AddContactInvoiceTransaction.class,
        RoomTransactions.AddContactTransaction.class,
        RoomTransactions.AddContactPaymentsPrivilegeTransaction.class,
        RoomTransactions.AddInvoiceTransaction.class,
        RoomTransactions.AddListContactsPrivilegeTransaction.class,
        RoomTransactions.AddUserTransaction.class,
        RoomTransactions.ConnectPeerTransaction.class,
        RoomTransactions.DecodePayReqTransaction.class,
        RoomTransactions.NewAddressTransaction.class,
        RoomTransactions.OpenChannelTransaction.class,
        RoomTransactions.SendPaymentTransaction.class,
        RoomTransactions.ShareContactTransaction.class,
})
abstract class RoomDB extends RoomDatabase {

    abstract RawQueryDaoRoom rawQueryDao();

    abstract AuthDaoRoom authDao();

    abstract AuthRequestDaoRoom authRequestDao();

    abstract AddUserDaoRoom userAddDao();

    abstract WalletBalanceDaoRoom walletBalanceDao();

    abstract WalletInfoDaoRoom walletInfoDao();

    abstract ChannelBalanceDaoRoom channelBalanceDao();

    abstract NewAddressDaoRoom newAddressDao();

    abstract ConnectPeerDaoRoom connectPeerDao();

    abstract DecodePayReqDaoRoom decodePayReqDao();

    abstract AddInvoiceDaoRoom addInvoiceDao();
    abstract InvoiceStateWorkerDaoRoom invoiceStateWorkerDao();
    abstract ListInvoicesDaoRoom listInvoicesDao();
    abstract GetInvoiceDaoRoom getInvoiceDao();

    abstract OpenChannelDaoRoom openChannelDao();
    abstract OpenChannelWorkerDaoRoom openChannelWorkerDao();
    abstract GetChannelDaoRoom getChannelDao();
    abstract ChannelStateWorkerDaoRoom channelStateWorkerDao();

    abstract SendPaymentDaoRoom sendPaymentDao();
    abstract SendPaymentWorkerDaoRoom sendPaymentWorkerDao();
    abstract GetSendPaymentDaoRoom getSendPaymentDao();
    abstract SubscribeSendPaymentsDaoRoom subscribeSendPaymentsDao();

    abstract ListPaymentsDaoRoom listPaymentsDao();

    abstract AddContactDaoRoom addContactDao();
    abstract GetContactDaoRoom getContactDao();
    abstract ListContactsDaoRoom listContactsDao();

    abstract AddListContactsPrivilegeDaoRoom addListContactsPrivilegeDao();

    abstract AddContactPaymentsPrivilegeDaoRoom addContactPaymentsPrivilegeDao();

    abstract ShareContactDaoRoom shareContactDao();
    abstract AddContactInvoiceDaoRoom addContactInvoiceDao();

    abstract RouteHintsDaoRoom routeHintsDao();

    abstract NodeInfoDaoRoom nodeInfoDao();
}
