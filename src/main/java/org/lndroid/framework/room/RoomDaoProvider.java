package org.lndroid.framework.room;

import androidx.room.Room;

import java.util.HashMap;
import java.util.Map;

import org.lndroid.framework.dao.IRawQueryDao;
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

        pluginDaos_.put(DefaultPlugins.ADD_USER, new AddUserDao(db_.userAddDao()));
        pluginDaos_.put(DefaultPlugins.GET_APP_USER, authDao_);
        pluginDaos_.put(DefaultPlugins.GET_USER, authDao_);
        pluginDaos_.put(DefaultPlugins.GET_AUTH_REQUEST_USER, authRequestDao_);

        pluginDaos_.put(DefaultPlugins.GET_WALLET_BALANCE, new WalletBalanceDao(db_.walletBalanceDao()));
        pluginDaos_.put(DefaultPlugins.WALLET_BALANCE_WORKER,
                pluginDaos_.get(DefaultPlugins.GET_WALLET_BALANCE));

        pluginDaos_.put(DefaultPlugins.GET_CHANNEL_BALANCE, new ChannelBalanceDao(db_.channelBalanceDao()));
        pluginDaos_.put(DefaultPlugins.CHANNEL_BALANCE_WORKER,
                pluginDaos_.get(DefaultPlugins.GET_CHANNEL_BALANCE));

        pluginDaos_.put(DefaultPlugins.GET_WALLET_INFO, new WalletInfoDao(db_.walletInfoDao()));
        pluginDaos_.put(DefaultPlugins.WALLET_INFO_WORKER,
                pluginDaos_.get(DefaultPlugins.GET_WALLET_INFO));

        pluginDaos_.put(DefaultPlugins.NEW_ADDRESS, new NewAddressDao(db_.newAddressDao()));
        pluginDaos_.put(DefaultPlugins.CONNECT_PEER, new ConnectPeerDao(db_.connectPeerDao()));
        pluginDaos_.put(DefaultPlugins.DECODE_PAYREQ, new DecodePayReqDao(db_.decodePayReqDao()));

        pluginDaos_.put(DefaultPlugins.ADD_INVOICE, new AddInvoiceDao(db_.addInvoiceDao()));
        pluginDaos_.put(DefaultPlugins.INVOICE_STATE_WORKER, new InvoiceStateWorkerDao(db_.invoiceStateWorkerDao()));
        pluginDaos_.put(DefaultPlugins.LIST_INVOICES, new ListInvoicesDao(db_.listInvoicesDao()));
        pluginDaos_.put(DefaultPlugins.GET_INVOICE, new GetInvoiceDao(db_.getInvoiceDao()));

        pluginDaos_.put(DefaultPlugins.SEND_PAYMENT, new SendPaymentDao(
                db_.sendPaymentDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.SEND_PAYMENT_WORKER, new SendPaymentWorkerDao(
                db_.sendPaymentWorkerDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.GET_SEND_PAYMENT, new GetSendPaymentDao(db_.getSendPaymentDao()));
        pluginDaos_.put(DefaultPlugins.SUBSCRIBE_SEND_PAYMENTS, new SubscribeSendPaymentsDao(db_.subscribeSendPaymentsDao()));

        pluginDaos_.put(DefaultPlugins.OPEN_CHANNEL, new OpenChannelDao(db_.openChannelDao()));
        pluginDaos_.put(DefaultPlugins.OPEN_CHANNEL_WORKER, new OpenChannelWorkerDao(db_.openChannelWorkerDao()));
        pluginDaos_.put(DefaultPlugins.GET_CHANNEL, new GetChannelDao(db_.getChannelDao()));
        pluginDaos_.put(DefaultPlugins.CHANNEL_STATE_WORKER, new ChannelStateWorkerDao(db_.channelStateWorkerDao()));

        pluginDaos_.put(DefaultPlugins.LIST_PAYMENTS, new ListPaymentsDao(db_.listPaymentsDao()));

        pluginDaos_.put(DefaultPlugins.ADD_CONTACT_APP, new AddContactDao(
                db_.addContactDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.GET_CONTACT, new GetContactDao(
                db_.getContactDao(), db_.routeHintsDao()));
        pluginDaos_.put(DefaultPlugins.LIST_CONTACTS, new ListContactsDao(db_.listContactsDao()));

        pluginDaos_.put(DefaultPlugins.ADD_LIST_CONTACTS_PRIVILEGE,
                new AddListContactsPrivilegeDao(db_.addListContactsPrivilegeDao()));
        pluginDaos_.put(DefaultPlugins.ADD_CONTACT_PAYMENTS_PRIVILEGE,
                new AddContactPaymentsPrivilegeDao(db_.addContactPaymentsPrivilegeDao()));

        pluginDaos_.put(DefaultPlugins.SHARE_CONTACT,
                new ShareContactDao(db_.shareContactDao()));
        pluginDaos_.put(DefaultPlugins.ADD_CONTACT_INVOICE,
                new AddContactInvoiceDao(db_.addContactInvoiceDao()));

        pluginDaos_.put(DefaultPlugins.NODE_INFO_WORKER,
                new NodeInfoDao(db_.nodeInfoDao()));

        if (cb != null) {
            cb.onOpen();
        }
    }

    @Override
    public void insertUser(WalletData.User user) {
        RoomData.User ru = new RoomData.User();
        ru.setData(user);
        db_.userAddDao().insertUser(ru);
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
    public IPluginDao getPluginDao(String pluginId) {
        return pluginDaos_.get(pluginId);
    }
}
