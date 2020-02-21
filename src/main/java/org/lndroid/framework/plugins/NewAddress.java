package org.lndroid.framework.plugins;

import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.lnd.LightningCodec;

public class NewAddress extends
        LndActionBase<WalletData.NewAddressRequest, Data.NewAddressRequest,
                WalletData.NewAddress, Data.NewAddressResponse>
{
    // plugin's Dao must extend this class
    public interface IDao extends ILndActionDao<WalletData.NewAddressRequest, WalletData.NewAddress> {};

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 600000; // 10 min

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected Data.NewAddressRequest createLndRequest(PluginContext ctx, WalletData.NewAddressRequest req) {
        Data.NewAddressRequest r = new Data.NewAddressRequest();
        LightningCodec.NewAddressCodec.encode(req, r);
        return r;
    }

    @Override
    protected WalletData.NewAddress createResponse(
            PluginContext ctx, WalletData.NewAddressRequest req, long authUserId, Data.NewAddressResponse r) {
        WalletData.NewAddress.Builder b = WalletData.NewAddress.builder();
        LightningCodec.NewAddressCodec.decode(r, b);
        return b.build();
    }

    @Override
    protected void execute(Data.NewAddressRequest r, ILightningCallback<Data.NewAddressResponse> cb) {
        lnd().client().newAddress(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, WalletData.NewAddressRequest req, WalletData.NewAddress rep) {
        engine().onSignal(id(), DefaultTopics.NEW_ADDRESS, null);
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.NewAddressRequest> tx) {
        // FIXME check limits
        return user.isRoot();
    }

    @Override
    protected WalletData.NewAddressRequest getData(IPluginData in) {
        in.assignDataType(WalletData.NewAddressRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.NewAddress.class;
    }

    @Override
    public String id() {
        return DefaultPlugins.NEW_ADDRESS;
    }
}

