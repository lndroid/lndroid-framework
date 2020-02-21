package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.dao.IWalletBalanceDao;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public class GetWalletBalance extends GetBase<Long> {

    // plugin's Dao must implement this
    public interface IDao extends IWalletBalanceDao {};

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IDao dao_;

    public GetWalletBalance() {
        super(DefaultPlugins.GET_WALLET_BALANCE, DefaultTopics.WALLET_BALANCE);
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
        return WalletData.WalletBalance.class;
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
