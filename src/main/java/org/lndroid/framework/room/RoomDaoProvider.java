package org.lndroid.framework.room;

import androidx.room.Room;

import java.util.HashMap;
import java.util.Map;

import org.lndroid.framework.dao.IRawQueryDao;
import org.lndroid.framework.dao.ITransactionDao;
import org.lndroid.framework.engine.IDaoConfig;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IAuthDao;
import org.lndroid.framework.dao.IAuthRequestDao;
import org.lndroid.framework.dao.IDBDaoProvider;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.defaults.DefaultPlugins;

public class RoomDaoProvider implements IDBDaoProvider {

    private IDaoConfig config_;
    private RoomDB db_;

    private RawQueryDao rawQueryDao_;

    private AuthDao authDao_;
    private AuthRequestDao authRequestDao_;

    private Map<String, IPluginDao> pluginDaos_ = new HashMap<>();

    public RoomDaoProvider(IDaoConfig c) {
        config_ = c;
    }

    @Override
    public void init(String db, byte[] password, OpenCallback cb) {

        // FIXME apply password to decrypt db

        db_ = Room.databaseBuilder(config_.getContext(), RoomDB.class, db)
                // FIXME implement migrations after the first release
                .fallbackToDestructiveMigration()
                .build();

        rawQueryDao_ = new RawQueryDao(db_.rawQueryDao(), db_.getOpenHelper());

        authDao_ = new AuthDao(db_.authDao());
        authRequestDao_ = new AuthRequestDao(db_.authRequestDao());

        pluginDaos_.put(DefaultPlugins.ADD_USER, new AddUserDao(
                db_.userAddDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.GET_APP_USER, authDao_);
        pluginDaos_.put(DefaultPlugins.GET_USER, authDao_);
        pluginDaos_.put(DefaultPlugins.GET_AUTH_REQUEST_USER, authRequestDao_);
        pluginDaos_.put(DefaultPlugins.LIST_USERS, new ListUsersDao(
                db_.listUsersDao()));

        pluginDaos_.put(DefaultPlugins.GET_WALLET_BALANCE, new WalletBalanceDao(
                db_.walletBalanceDao()));
        pluginDaos_.put(DefaultPlugins.WALLET_BALANCE_WORKER,
                pluginDaos_.get(DefaultPlugins.GET_WALLET_BALANCE));

        pluginDaos_.put(DefaultPlugins.GET_CHANNEL_BALANCE, new ChannelBalanceDao(
                db_.channelBalanceDao()));
        pluginDaos_.put(DefaultPlugins.CHANNEL_BALANCE_WORKER,
                pluginDaos_.get(DefaultPlugins.GET_CHANNEL_BALANCE));

        pluginDaos_.put(DefaultPlugins.GET_WALLET_INFO, new WalletInfoDao(
                db_.walletInfoDao()));
        pluginDaos_.put(DefaultPlugins.WALLET_INFO_WORKER,
                pluginDaos_.get(DefaultPlugins.GET_WALLET_INFO));

        pluginDaos_.put(DefaultPlugins.CONNECT_PEER, new ConnectPeerDao(
                db_.connectPeerDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.DISCONNECT_PEER, new DisconnectPeerDao(
                db_.disconnectPeerDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.GET_PEER, new GetPeerDao(
                db_.getPeerDao()));
        pluginDaos_.put(DefaultPlugins.LIST_PEERS, new ListPeersDao(
                db_.listPeersDao()));
        pluginDaos_.put(DefaultPlugins.PEER_STATE_WORKER, new PeerStateWorkerDao(
                db_.peerStateWorkerDao()));

        pluginDaos_.put(DefaultPlugins.NEW_ADDRESS, new NewAddressDao(
                db_.newAddressDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.DECODE_PAYREQ, new DecodePayReqDao(
                db_.decodePayReqDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.ESTIMATE_FEE, new EstimateFeeDao(
                db_.estimateFeeDao(), db_.txDao()));

        pluginDaos_.put(DefaultPlugins.ADD_INVOICE, new AddInvoiceDao(
                db_.addInvoiceDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.INVOICE_STATE_WORKER, new InvoiceStateWorkerDao(
                db_.invoiceStateWorkerDao()));
        pluginDaos_.put(DefaultPlugins.LIST_INVOICES, new ListInvoicesDao(
                db_.listInvoicesDao()));
        pluginDaos_.put(DefaultPlugins.GET_INVOICE, new GetInvoiceDao(
                db_.getInvoiceDao()));

        pluginDaos_.put(DefaultPlugins.SEND_PAYMENT, new SendPaymentDao(
                db_.sendPaymentDao(), db_.txDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.SEND_PAYMENT_WORKER, new SendPaymentWorkerDao(
                db_.sendPaymentWorkerDao(), db_.txDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.GET_SEND_PAYMENT, new GetSendPaymentDao(
                db_.getSendPaymentDao()));
        pluginDaos_.put(DefaultPlugins.SUBSCRIBE_SEND_PAYMENTS, new SubscribeSendPaymentsDao(
                db_.subscribeSendPaymentsDao()));

        pluginDaos_.put(DefaultPlugins.OPEN_CHANNEL, new OpenChannelDao(
                db_.openChannelDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.OPEN_CHANNEL_WORKER, new OpenChannelWorkerDao(
                db_.openChannelWorkerDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.CLOSE_CHANNEL, new CloseChannelDao(
                db_.closeChannelDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.CLOSE_CHANNEL_WORKER, new CloseChannelWorkerDao(
                db_.closeChannelWorkerDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.GET_CHANNEL, new GetChannelDao(
                db_.getChannelDao()));
        pluginDaos_.put(DefaultPlugins.CHANNEL_STATE_WORKER, new ChannelStateWorkerDao(
                db_.channelStateWorkerDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.LIST_CHANNELS, new ListChannelsDao(
                db_.listChannelsDao()));

        pluginDaos_.put(DefaultPlugins.LIST_PAYMENTS, new ListPaymentsDao(
                db_.listPaymentsDao()));

        pluginDaos_.put(DefaultPlugins.ADD_APP_CONTACT, new AddAppContactDao(
                db_.addContactDao(), db_.txDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.GET_CONTACT, new GetContactDao(
                db_.getContactDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.LIST_CONTACTS, new ListContactsDao(
                db_.listContactsDao()));

        pluginDaos_.put(DefaultPlugins.ADD_LIST_CONTACTS_PRIVILEGE, new AddListContactsPrivilegeDao(
                db_.addListContactsPrivilegeDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.ADD_CONTACT_PAYMENTS_PRIVILEGE, new AddContactPaymentsPrivilegeDao(
                db_.addContactPaymentsPrivilegeDao(), db_.txDao()));

        pluginDaos_.put(DefaultPlugins.SHARE_CONTACT, new ShareContactDao(
                db_.shareContactDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.ADD_CONTACT_INVOICE, new AddContactInvoiceDao(
                db_.addContactInvoiceDao(), db_.txDao()));

        pluginDaos_.put(DefaultPlugins.NODE_INFO_WORKER, new NodeInfoDao(
                db_.nodeInfoDao()));

        pluginDaos_.put(DefaultPlugins.SEND_COINS, new SendCoinsDao(
                db_.sendCoinsDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.SEND_COINS_WORKER, new SendCoinsWorkerDao(
                db_.sendCoinsWorkerDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.TRANSACTION_STATE_WORKER, new TransactionStateWorkerDao(
                db_.transactionStateWorkerDao(), db_.txDao()));
        pluginDaos_.put(DefaultPlugins.GET_TRANSACTION, new GetTransactionDao(
                db_.getTransactionDao()));
        pluginDaos_.put(DefaultPlugins.LIST_TRANSACTIONS, new ListTransactionsDao(
                db_.listTransactionsDao()));

        pluginDaos_.put(DefaultPlugins.UTXO_WORKER, new UtxoWorkerDao(
                db_.utxoWorkerDao()));
        pluginDaos_.put(DefaultPlugins.GET_UTXO, new GetUtxoDao(
                db_.getUtxoDao()));
        pluginDaos_.put(DefaultPlugins.LIST_UTXO, new ListUtxoDao(
                db_.listUtxoDao()));

        if (cb != null) {
            cb.onOpen();
        }
    }

    @Override
    public void insertUser(WalletData.User user) {
        db_.userAddDao().insertResponse(user, null);
    }

    @Override
    public IAuthDao getAuthDao() {
        return authDao_;
    }

    @Override
    public IAuthRequestDao getAuthRequestDao() {
        return authRequestDao_;
    }

    @Override
    public IRawQueryDao getRawQueryDao() {
        return rawQueryDao_;
    }

    @Override
    public ITransactionDao getTxDao() {
        return db_.txDao();
    }

    @Override
    public IPluginDao getPluginDao(String pluginId) {
        return pluginDaos_.get(pluginId);
    }
}
