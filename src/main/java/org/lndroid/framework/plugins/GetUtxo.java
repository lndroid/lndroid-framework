package org.lndroid.framework.plugins;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.IGetDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

import java.io.IOException;
import java.lang.reflect.Type;

public class GetUtxo extends GetBase<Long> {
    private static final String TAG = "GetUtxo";
    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IGetDao<WalletData.Utxo> dao_;

    public GetUtxo() {
        super(DefaultPlugins.GET_UTXO, DefaultTopics.UTXO_STATE);
    }

    @Override
    protected long defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(
            PluginContext ctx, WalletDataDecl.GetRequestTmpl<Long> req, WalletData.User user) {
        return user.isRoot();
    }

    @Override
    protected Object get(Long id) {
        return dao_.get(id);
    }

    @Override
    protected Type getType() {
        return WalletData.Utxo.class;
    }

    @Override
    protected WalletData.GetRequestLong getInputData(IPluginData in) {
        // this is the way to get the type of a generic parameterized class
        in.assignDataType(WalletData.GetRequestLong.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (IGetDao<WalletData.Utxo>) server.getDaoProvider().getPluginDao(id());
    }
}
