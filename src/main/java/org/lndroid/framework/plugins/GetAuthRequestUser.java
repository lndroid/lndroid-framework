package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.IAuthRequestDao;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public class GetAuthRequestUser extends GetBase<Long> {

    public interface IDao extends IAuthRequestDao{};

    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IDao dao_;

    public GetAuthRequestUser() {
        // FIXME add topic which covers creation of any auth request!
        super(DefaultPlugins.GET_AUTH_REQUEST_USER, DefaultTopics.USER_STATE);
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
    protected boolean isUserPrivileged(PluginContext ctx, WalletDataDecl.GetRequestTmpl<Long> req, WalletData.User user) {
        return !user.isApp();
    }

    @Override
    protected Object get(Long id) {
        return dao_.getAuthRequestUser(id);
    }

    @Override
    protected Type getType() {
        return WalletData.User.class;
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
