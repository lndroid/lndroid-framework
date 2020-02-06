package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.IAuthDao;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public class GetAppUser extends GetBase<String> {

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IAuthDao dao_;

    public GetAppUser() {
        super(DefaultPlugins.GET_APP_USER, DefaultTopics.USER_STATE);
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback engine) {
        super.init(engine);
        dao_ = (IAuthDao) server.getDaoProvider().getPluginDao(id());
    }

    @Override
    protected long defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(PluginContext ctx, WalletDataDecl.GetRequestTmpl req, WalletData.User user) {
        return !user.isApp();
    }

    @Override
    protected Object get(String appPubkey) {
        return dao_.getByAppPubkey(appPubkey);
    }

    @Override
    protected Type getType() {
        return WalletData.User.class;
    }

    @Override
    protected WalletData.GetRequestString getInputData(IPluginData in) {
        in.assignDataType(WalletData.GetRequestString.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }
}
