package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IListDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginServer;

public class ListInvoices extends ListBase<WalletData.ListInvoicesRequest, WalletData.Invoice> {

    public interface IDao extends IListDao<WalletData.ListInvoicesRequest, WalletData.ListInvoicesResult>{};

    private IDao dao_;

    public ListInvoices() {
        super(DefaultPlugins.LIST_INVOICES);
    }

    @Override
    protected WalletData.ListInvoicesResult listEntities(
            WalletData.ListInvoicesRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user);
    }

    @Override
    protected WalletData.ListInvoicesRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ListInvoicesRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ListInvoicesResult.class;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListInvoicesRequest req, WalletData.User user) {
        return user.isRoot() || req.onlyOwn();
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.INVOICE_STATE);
    }
}

