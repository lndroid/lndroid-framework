package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.IGetChannelDao;
import org.lndroid.framework.engine.IDaoProvider;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.PluginContext;

public class GetChannel extends GetBase<Long> {
    private static final String TAG = "GetChannel";
    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IGetChannelDao dao_;

    public GetChannel() {
        super(DefaultPlugins.GET_CHANNEL, DefaultTopics.CHANNEL_STATE);
    }

    @Override
    protected long defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(PluginContext ctx, WalletDataDecl.GetRequestTmpl<Long> req, WalletData.User user) {
        // FIXME implement
        return user.isRoot();
    }

    @Override
    protected Object get(Long id) {
        return dao_.get(id);
    }

    @Override
    protected Type getType() {
        return WalletData.Channel.class;
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
    public void init(IDaoProvider dp, IPluginForegroundCallback cb) {
        super.init(cb);
        dao_ = (IGetChannelDao)dp.getPluginDao(id());
    }
}
