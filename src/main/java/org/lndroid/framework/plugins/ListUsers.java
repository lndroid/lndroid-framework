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

public class ListUsers extends ListBase<WalletData.ListUsersRequest, WalletData.User> {

    public interface IDao extends IListDao<WalletData.ListUsersRequest, WalletData.ListUsersResult> {};

    private IDao dao_;

    public ListUsers() {
        super(DefaultPlugins.LIST_USERS);
    }

    @Override
    protected WalletData.ListUsersResult listEntities(
            WalletData.ListUsersRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user);
    }

    @Override
    protected WalletData.ListUsersRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ListUsersRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ListUsersResult.class;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListUsersRequest req, WalletData.User user) {
        return user.isRoot() || dao_.hasPrivilege(req, user);
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.USER_STATE);
    }
}
