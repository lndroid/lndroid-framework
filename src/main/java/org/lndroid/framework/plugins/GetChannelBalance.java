package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.dao.IChannelBalanceDao;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public class GetChannelBalance extends GetBase<Long> {
    // Dao must implement this
    public interface IDao extends IChannelBalanceDao {};

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IDao dao_;

    public GetChannelBalance() {
        super(DefaultPlugins.GET_CHANNEL_BALANCE, DefaultTopics.CHANNEL_BALANCE);
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback engine) {
        super.init(engine);
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    protected long defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(PluginContext ctx, WalletDataDecl.GetRequestTmpl req, WalletData.User user) {
        // FIXME add limits checks
        return user.isRoot();
    }

    @Override
    protected Object get(Long id) {
        return dao_.get();
    }

    @Override
    protected Type getType() {
        return WalletData.ChannelBalance.class;
    }

    @Override
    protected WalletData.GetRequestLong getInputData(IPluginData in) {
        in.assignDataType(WalletData.GetRequestLong.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

}
