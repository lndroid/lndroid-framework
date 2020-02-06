package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.IGetSendPaymentDao;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.engine.PluginContext;

public class GetSendPayment extends GetBase<Long> {
    private static final String TAG = "GetSendPayment";
    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IGetSendPaymentDao dao_;

    public GetSendPayment() {
        super(DefaultPlugins.GET_SEND_PAYMENT, DefaultTopics.SEND_PAYMENT_STATE);
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
        return WalletData.SendPayment.class;
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

    @Override
    public void init(IPluginServer server, IPluginForegroundCallback callback) {
        super.init(callback);
        dao_ = (IGetSendPaymentDao) server.getDaoProvider().getPluginDao(id());
    }
}
