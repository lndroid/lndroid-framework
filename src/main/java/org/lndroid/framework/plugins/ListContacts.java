package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.room.ListContactsDao;

public class ListContacts extends ListBase<WalletData.ListContactsRequest, WalletData.Contact> {

    private ListContactsDao dao_;

    public ListContacts() {
        super(DefaultPlugins.LIST_CONTACTS);
    }

    @Override
    protected WalletData.ListContactsResult listEntities(
            WalletData.ListContactsRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user.id(), user.isApp());
    }

    @Override
    protected WalletData.ListContactsRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ListContactsRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ListContactsResult.class;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListContactsRequest req, WalletData.User user) {
        return user.isRoot() || dao_.hasPrivilege(user.id());
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (ListContactsDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.CONTACT_STATE);
    }
}
