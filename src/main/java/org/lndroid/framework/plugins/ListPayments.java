package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.room.ListPaymentsDao;

public class ListPayments extends ListBase<WalletData.ListPaymentsRequest, WalletData.Payment> {

    private ListPaymentsDao dao_;

    public ListPayments() {
        super(DefaultPlugins.LIST_PAYMENTS);
    }

    @Override
    protected WalletData.ListPaymentsResult listEntities(
            WalletData.ListPaymentsRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user.id());
    }

    @Override
    protected WalletData.ListPaymentsRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ListPaymentsRequest.class);
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
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (ListPaymentsDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.SEND_PAYMENT_STATE);
        topics.add(DefaultTopics.INVOICE_STATE);
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListPaymentsRequest req, WalletData.User user) {
        return user.isRoot() || req.onlyOwn() || dao_.hasPrivilege(req, user.id());
    }

}
