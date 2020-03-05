package org.lndroid.framework.defaults;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.lndroid.framework.engine.IPlugin;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.engine.IPluginForeground;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginProvider;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.plugins.AddAppContact;
import org.lndroid.framework.plugins.AddContact;
import org.lndroid.framework.plugins.AddContactInvoice;
import org.lndroid.framework.plugins.AddContactPaymentsPrivilege;
import org.lndroid.framework.plugins.AddInvoice;
import org.lndroid.framework.plugins.AddListContactsPrivilege;
import org.lndroid.framework.plugins.AddUser;
import org.lndroid.framework.plugins.ChannelBackupWorker;
import org.lndroid.framework.plugins.ChannelBalanceWorker;
import org.lndroid.framework.plugins.ChannelStateWorker;
import org.lndroid.framework.plugins.CloseChannel;
import org.lndroid.framework.plugins.CloseChannelWorker;
import org.lndroid.framework.plugins.ConnectPeer;
import org.lndroid.framework.plugins.DecodePayReq;
import org.lndroid.framework.plugins.DisconnectPeer;
import org.lndroid.framework.plugins.EstimateFee;
import org.lndroid.framework.plugins.GetAppUser;
import org.lndroid.framework.plugins.GetAuthRequestUser;
import org.lndroid.framework.plugins.GetChannel;
import org.lndroid.framework.plugins.GetChannelBalance;
import org.lndroid.framework.plugins.GetContact;
import org.lndroid.framework.plugins.GetInvoice;
import org.lndroid.framework.plugins.GetPeer;
import org.lndroid.framework.plugins.GetSendPayment;
import org.lndroid.framework.plugins.GetTransaction;
import org.lndroid.framework.plugins.GetUser;
import org.lndroid.framework.plugins.GetUtxo;
import org.lndroid.framework.plugins.GetWalletBalance;
import org.lndroid.framework.plugins.GetWalletInfo;
import org.lndroid.framework.plugins.InvoiceStateWorker;
import org.lndroid.framework.plugins.ListChannels;
import org.lndroid.framework.plugins.ListContacts;
import org.lndroid.framework.plugins.ListInvoices;
import org.lndroid.framework.plugins.ListPayments;
import org.lndroid.framework.plugins.ListPeers;
import org.lndroid.framework.plugins.ListTransactions;
import org.lndroid.framework.plugins.ListUsers;
import org.lndroid.framework.plugins.ListUtxo;
import org.lndroid.framework.plugins.NewAddress;
import org.lndroid.framework.plugins.NodeInfoWorker;
import org.lndroid.framework.plugins.OpenChannel;
import org.lndroid.framework.plugins.OpenChannelWorker;
import org.lndroid.framework.plugins.PeerStateWorker;
import org.lndroid.framework.plugins.SendCoins;
import org.lndroid.framework.plugins.SendCoinsWorker;
import org.lndroid.framework.plugins.SendPayment;
import org.lndroid.framework.plugins.SendPaymentWorker;
import org.lndroid.framework.plugins.SetNotifiedInvoices;
import org.lndroid.framework.plugins.ShareContact;
import org.lndroid.framework.plugins.SubscribeNewPaidInvoices;
import org.lndroid.framework.plugins.SubscribeSendPayments;
import org.lndroid.framework.plugins.TransactionStateWorker;
import org.lndroid.framework.plugins.UtxoWorker;
import org.lndroid.framework.plugins.WalletBalanceWorker;
import org.lndroid.framework.plugins.WalletInfoWorker;

public class DefaultPluginProvider implements IPluginProvider {

    class ForegroundPlugin implements IPlugin {

        private IPluginForeground plugin_;

        ForegroundPlugin(IPluginForeground p) {
            plugin_ = p;
        }

        @Override
        public String id() {
            return plugin_.id();
        }

        @Override
        public void init(IPluginServer server, IPluginForegroundCallback fcb, IPluginBackgroundCallback bcb) {
            plugin_.init(server, fcb);
        }

        @Override
        public IPluginForeground getForeground() {
            return plugin_;
        }

        @Override
        public IPluginBackground getBackground() {
            return null;
        }
    }

    class BackgroundPlugin implements IPlugin {

        private IPluginBackground plugin_;

        BackgroundPlugin(IPluginBackground p) {
            plugin_ = p;
        }

        @Override
        public String id() {
            return plugin_.id();
        }

        @Override
        public void init(IPluginServer server, IPluginForegroundCallback fcb, IPluginBackgroundCallback bcb) {
            plugin_.init(server, bcb);
        }

        @Override
        public IPluginForeground getForeground() {
            return null;
        }

        @Override
        public IPluginBackground getBackground() {
            return plugin_;
        }
    }

    private Map<String, IPlugin> plugins_ = new HashMap<>();

    @Override
    public void init() {
        plugins_.put(DefaultPlugins.ADD_USER, new ForegroundPlugin(new AddUser()));
        plugins_.put(DefaultPlugins.GET_APP_USER, new ForegroundPlugin(new GetAppUser()));
        plugins_.put(DefaultPlugins.GET_USER, new ForegroundPlugin(new GetUser()));
        plugins_.put(DefaultPlugins.GET_AUTH_REQUEST_USER, new ForegroundPlugin(new GetAuthRequestUser()));
        plugins_.put(DefaultPlugins.LIST_USERS, new ForegroundPlugin(new ListUsers()));

        plugins_.put(DefaultPlugins.GET_WALLET_BALANCE, new ForegroundPlugin(new GetWalletBalance()));
        plugins_.put(DefaultPlugins.WALLET_BALANCE_WORKER, new BackgroundPlugin(new WalletBalanceWorker()));

        plugins_.put(DefaultPlugins.GET_WALLET_INFO, new ForegroundPlugin(new GetWalletInfo()));
        plugins_.put(DefaultPlugins.WALLET_INFO_WORKER, new BackgroundPlugin(new WalletInfoWorker()));

        plugins_.put(DefaultPlugins.GET_CHANNEL_BALANCE, new ForegroundPlugin(new GetChannelBalance()));
        plugins_.put(DefaultPlugins.CHANNEL_BALANCE_WORKER, new BackgroundPlugin(new ChannelBalanceWorker()));

        plugins_.put(DefaultPlugins.NEW_ADDRESS, new ForegroundPlugin(new NewAddress()));
        plugins_.put(DefaultPlugins.ESTIMATE_FEE, new ForegroundPlugin(new EstimateFee()));

        plugins_.put(DefaultPlugins.CONNECT_PEER, new ForegroundPlugin(new ConnectPeer()));
        plugins_.put(DefaultPlugins.DISCONNECT_PEER, new ForegroundPlugin(new DisconnectPeer()));
        plugins_.put(DefaultPlugins.GET_PEER, new ForegroundPlugin(new GetPeer()));
        plugins_.put(DefaultPlugins.LIST_PEERS, new ForegroundPlugin(new ListPeers()));
        plugins_.put(DefaultPlugins.PEER_STATE_WORKER, new BackgroundPlugin(new PeerStateWorker()));

        plugins_.put(DefaultPlugins.DECODE_PAYREQ, new ForegroundPlugin(new DecodePayReq()));

        plugins_.put(DefaultPlugins.ADD_INVOICE, new ForegroundPlugin(new AddInvoice()));
        plugins_.put(DefaultPlugins.INVOICE_STATE_WORKER, new BackgroundPlugin(new InvoiceStateWorker()));
        plugins_.put(DefaultPlugins.LIST_INVOICES, new ForegroundPlugin(new ListInvoices()));
        plugins_.put(DefaultPlugins.GET_INVOICE, new ForegroundPlugin(new GetInvoice()));
        plugins_.put(DefaultPlugins.SET_NOTIFIED_INVOICES, new ForegroundPlugin(new SetNotifiedInvoices()));
        plugins_.put(DefaultPlugins.SUBSCRIBE_NEW_PAID_INVOICES, new ForegroundPlugin(new SubscribeNewPaidInvoices()));

        plugins_.put(DefaultPlugins.OPEN_CHANNEL, new ForegroundPlugin(new OpenChannel()));
        plugins_.put(DefaultPlugins.OPEN_CHANNEL_WORKER, new BackgroundPlugin(new OpenChannelWorker()));
        plugins_.put(DefaultPlugins.CLOSE_CHANNEL, new ForegroundPlugin(new CloseChannel()));
        plugins_.put(DefaultPlugins.CLOSE_CHANNEL_WORKER, new BackgroundPlugin(new CloseChannelWorker()));
        plugins_.put(DefaultPlugins.CHANNEL_STATE_WORKER, new BackgroundPlugin(new ChannelStateWorker()));
        plugins_.put(DefaultPlugins.GET_CHANNEL, new ForegroundPlugin(new GetChannel()));
        plugins_.put(DefaultPlugins.LIST_CHANNELS, new ForegroundPlugin(new ListChannels()));

        plugins_.put(DefaultPlugins.SEND_PAYMENT, new ForegroundPlugin(new SendPayment()));
        plugins_.put(DefaultPlugins.SEND_PAYMENT_WORKER, new BackgroundPlugin(new SendPaymentWorker()));
        plugins_.put(DefaultPlugins.GET_SEND_PAYMENT, new ForegroundPlugin(new GetSendPayment()));
        plugins_.put(DefaultPlugins.SUBSCRIBE_SEND_PAYMENTS, new ForegroundPlugin(new SubscribeSendPayments()));

        plugins_.put(DefaultPlugins.LIST_PAYMENTS, new ForegroundPlugin(new ListPayments()));

        plugins_.put(DefaultPlugins.ADD_CONTACT, new ForegroundPlugin(new AddContact()));
        plugins_.put(DefaultPlugins.ADD_APP_CONTACT, new ForegroundPlugin(new AddAppContact()));
        plugins_.put(DefaultPlugins.GET_CONTACT, new ForegroundPlugin(new GetContact()));
        plugins_.put(DefaultPlugins.LIST_CONTACTS, new ForegroundPlugin(new ListContacts()));

        plugins_.put(DefaultPlugins.ADD_LIST_CONTACTS_PRIVILEGE, new ForegroundPlugin(new AddListContactsPrivilege()));
        plugins_.put(DefaultPlugins.ADD_CONTACT_PAYMENTS_PRIVILEGE, new ForegroundPlugin(new AddContactPaymentsPrivilege()));

        plugins_.put(DefaultPlugins.SHARE_CONTACT, new ForegroundPlugin(new ShareContact()));
        plugins_.put(DefaultPlugins.ADD_CONTACT_INVOICE, new ForegroundPlugin(new AddContactInvoice()));

        plugins_.put(DefaultPlugins.NODE_INFO_WORKER, new BackgroundPlugin(new NodeInfoWorker()));

        plugins_.put(DefaultPlugins.CHANNEL_BACKUP_WORKER, new BackgroundPlugin(new ChannelBackupWorker()));

        plugins_.put(DefaultPlugins.SEND_COINS, new ForegroundPlugin(new SendCoins()));
        plugins_.put(DefaultPlugins.SEND_COINS_WORKER, new BackgroundPlugin(new SendCoinsWorker()));
        plugins_.put(DefaultPlugins.TRANSACTION_STATE_WORKER, new BackgroundPlugin(new TransactionStateWorker()));
        plugins_.put(DefaultPlugins.GET_TRANSACTION, new ForegroundPlugin(new GetTransaction()));
        plugins_.put(DefaultPlugins.LIST_TRANSACTIONS, new ForegroundPlugin(new ListTransactions()));

        plugins_.put(DefaultPlugins.UTXO_WORKER, new BackgroundPlugin(new UtxoWorker()));
        plugins_.put(DefaultPlugins.GET_UTXO, new ForegroundPlugin(new GetUtxo()));
        plugins_.put(DefaultPlugins.LIST_UTXO, new ForegroundPlugin(new ListUtxo()));

    }

    @Override
    public Set<String> getPluginIds() {
        return plugins_.keySet();
    }

    @Override
    public IPlugin getPlugin(String pluginId) {
        return plugins_.get(pluginId);
    }
}
