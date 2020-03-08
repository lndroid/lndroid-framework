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

public class GetPaymentPeerContact extends GetBase<Long> {

    // plugin's Dao must implement this
    public interface IDao extends IGetDao<WalletData.Contact> {};

    private static final String TAG = "GetPaymentPeerContact";
    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IDao dao_;

    public GetPaymentPeerContact() {
        super(DefaultPlugins.GET_PAYMENT_PEER_CONTACT, DefaultTopics.CONTACT_STATE);
    }

    @Override
    protected long defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(
            PluginContext ctx, WalletDataDecl.GetRequestTmpl<Long> req, WalletData.User user) {
        return !user.isAnonymous();
    }

    @Override
    protected Object get(Long id) {
        return dao_.get(id);
    }

    @Override
    protected Type getType() {
        return WalletData.Contact.class;
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
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
    }
}

