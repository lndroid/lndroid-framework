package org.lndroid.framework.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 64, exportSchema = true, entities = {
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
        RoomData.Transaction.class,
        RoomData.WalletBalance.class,
        RoomData.WalletInfo.class,
        RoomData.User.class,
        RoomData.Utxo.class,
        RoomTransactions.RoomTransaction.class,
        RoomTransactions.Contact.class,
        RoomTransactions.AddContactInvoiceRequest.class,
        RoomTransactions.AddInvoiceRequest.class,
        RoomTransactions.AddUserRequest.class,
        RoomTransactions.CloseChannelRequest.class,
        RoomTransactions.ConnectPeerRequest.class,
        RoomTransactions.ContactPaymentsPrivilege.class,
        RoomTransactions.EstimateFeeRequest.class,
        RoomTransactions.ListContactsPrivilege.class,
        RoomTransactions.NewAddressRequest.class,
        RoomTransactions.OpenChannelRequest.class,
        RoomTransactions.PayReqString.class,
        RoomTransactions.SendCoinsRequest.class,
        RoomTransactions.SendPaymentRequest.class,
        RoomTransactions.ShareContactRequest.class,
})
abstract class RoomDB extends RoomDatabase {

    abstract RawQueryDao.DaoRoom rawQueryDao();
    abstract RoomTransactions.TransactionDao txDao();

    abstract AuthDao.DaoRoom authDao();

    abstract AuthRequestDao.DaoRoom authRequestDao();

    abstract AddUserDao.DaoRoom userAddDao();

    abstract WalletBalanceDao.DaoRoom walletBalanceDao();

    abstract WalletInfoDao.DaoRoom walletInfoDao();

    abstract ChannelBalanceDao.DaoRoom channelBalanceDao();

    abstract NewAddressDao.DaoRoom newAddressDao();

    abstract ConnectPeerDao.DaoRoom connectPeerDao();

    abstract DecodePayReqDao.DaoRoom decodePayReqDao();
    abstract EstimateFeeDao.DaoRoom estimateFeeDao();

    abstract AddInvoiceDao.DaoRoom addInvoiceDao();
    abstract InvoiceStateWorkerDao.DaoRoom invoiceStateWorkerDao();
    abstract ListInvoicesDao.DaoRoom listInvoicesDao();
    abstract GetInvoiceDao.DaoRoom getInvoiceDao();

    abstract OpenChannelDao.DaoRoom openChannelDao();
    abstract OpenChannelWorkerDao.DaoRoom openChannelWorkerDao();
    abstract GetChannelDao.DaoRoom getChannelDao();
    abstract ChannelStateWorkerDao.DaoRoom channelStateWorkerDao();
    abstract ListChannelsDao.DaoRoom listChannelsDao();

    abstract SendPaymentDao.DaoRoom sendPaymentDao();
    abstract SendPaymentWorkerDao.DaoRoom sendPaymentWorkerDao();
    abstract GetSendPaymentDao.DaoRoom getSendPaymentDao();
    abstract SubscribeSendPaymentsDao.DaoRoom subscribeSendPaymentsDao();

    abstract ListPaymentsDao.DaoRoom listPaymentsDao();

    abstract AddAppContactDao.DaoRoom addContactDao();
    abstract GetContactDao.DaoRoom getContactDao();
    abstract ListContactsDao.DaoRoom listContactsDao();

    abstract AddListContactsPrivilegeDao.DaoRoom addListContactsPrivilegeDao();

    abstract AddContactPaymentsPrivilegeDao.DaoRoom addContactPaymentsPrivilegeDao();

    abstract ShareContactDao.DaoRoom shareContactDao();
    abstract AddContactInvoiceDao.DaoRoom addContactInvoiceDao();

    abstract RouteHintsDaoRoom routeHintsDao();

    abstract NodeInfoDao.DaoRoom nodeInfoDao();

    abstract SendCoinsDao.DaoRoom sendCoinsDao();
    abstract SendCoinsWorkerDao.DaoRoom sendCoinsWorkerDao();
    abstract TransactionStateWorkerDao.DaoRoom transactionStateWorkerDao();
    abstract GetTransactionDao.DaoRoom getTransactionDao();
    abstract ListTransactionsDao.DaoRoom listTransactionsDao();

    abstract UtxoWorkerDao.DaoRoom utxoWorkerDao();
    abstract GetUtxoDao.DaoRoom getUtxoDao();
    abstract ListUtxoDao.DaoRoom listUtxoDao();
}
