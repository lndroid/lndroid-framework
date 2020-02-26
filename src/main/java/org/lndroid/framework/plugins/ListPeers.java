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

public class ListPeers extends ListBase<WalletData.ListPeersRequest, WalletData.Peer> {

    public interface IDao extends IListDao<WalletData.ListPeersRequest, WalletData.ListPeersResult> {};

    private IDao dao_;

    public ListPeers() {
        super(DefaultPlugins.LIST_PEERS);
    }

    @Override
    protected WalletData.ListPeersResult listEntities(
            WalletData.ListPeersRequest req, WalletData.ListPage page, WalletData.User user) {
        return dao_.list(req, page, user);
    }

    @Override
    protected WalletData.ListPeersRequest getData(IPluginData in) {
        in.assignDataType(WalletData.ListPeersRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.ListPeersResult.class;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.ListPeersRequest req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        topics.add(DefaultTopics.PEER_STATE);
    }
}


