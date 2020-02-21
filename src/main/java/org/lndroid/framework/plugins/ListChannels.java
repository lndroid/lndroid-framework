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

public class ListChannels extends ListBase<WalletData.ListChannelsRequest, WalletData.Channel> {

    public interface IDao extends IListDao<WalletData.ListChannelsRequest, WalletData.ListChannelsResult>{};

    private IDao dao_;

    public ListChannels() {
        super(DefaultPlugins.LIST_CHANNELS);
    }

    @Override
    protected WalletData.ListChannelsResult listEntities(
            WalletData.ListChannelsRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user);
    }

    @Override
    protected WalletData.ListChannelsRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ListChannelsRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ListChannelsResult.class;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListChannelsRequest req, WalletData.User user) {
        return user.isRoot() || dao_.hasPrivilege(req, user);
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (IDao)server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.CHANNEL_STATE);
    }
}

