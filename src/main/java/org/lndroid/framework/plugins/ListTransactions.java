package org.lndroid.framework.plugins;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.IListDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class ListTransactions extends ListBase<WalletData.ListTransactionsRequest, WalletData.Transaction> {

    private IListDao<WalletData.ListTransactionsRequest, WalletData.ListTransactionsResult> dao_;

    public ListTransactions() {
        super(DefaultPlugins.LIST_TRANSACTIONS);
    }

    @Override
    protected WalletData.ListTransactionsResult listEntities(
            WalletData.ListTransactionsRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user);
    }

    @Override
    protected WalletData.ListTransactionsRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ListTransactionsRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ListTransactionsResult.class;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListTransactionsRequest req, WalletData.User user) {
        return user.isRoot() || dao_.hasPrivilege(req, user);
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (IListDao<WalletData.ListTransactionsRequest, WalletData.ListTransactionsResult>) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.TRANSACTION_STATE);
    }
}
