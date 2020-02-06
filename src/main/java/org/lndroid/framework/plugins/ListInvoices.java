package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.engine.IDaoProvider;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.room.ListInvoicesDao;

public class ListInvoices extends ListBase<WalletData.ListInvoicesRequest, WalletData.Invoice> {

    private ListInvoicesDao dao_;

    public ListInvoices() {
        super(DefaultPlugins.LIST_INVOICES);
    }

    @Override
    protected WalletData.ListInvoicesResult listEntities(
            WalletData.ListInvoicesRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user.id());
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
        return WalletData.ListPaymentsResult.class;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListInvoicesRequest req, WalletData.User user) {
        // FIXME
        return user.isRoot() || req.onlyOwn();
    }

    @Override
    public void init(IDaoProvider dp, IPluginForegroundCallback cb) {
        super.init(cb);
        dao_ = (ListInvoicesDao)dp.getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.INVOICE_STATE);
    }
}

