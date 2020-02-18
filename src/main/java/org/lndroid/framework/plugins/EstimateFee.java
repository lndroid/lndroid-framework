package org.lndroid.framework.plugins;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.lnd.LightningCodec;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.io.IOException;
import java.lang.reflect.Type;

public class EstimateFee extends
        LndActionBase<WalletData.EstimateFeeRequest, Data.EstimateFeeRequest,
                WalletData.EstimateFeeResponse, Data.EstimateFeeResponse> {

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
    protected Data.EstimateFeeRequest createLndRequest(
            ILndActionDao<WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse> dao,
            PluginContext ctx,
            WalletData.EstimateFeeRequest req) {
        Data.EstimateFeeRequest p = new Data.EstimateFeeRequest();
        LightningCodec.EstimateFeeCodec.encode(req, p);
        return p;
    }

    @Override
    protected WalletData.EstimateFeeResponse createResponse(
            PluginContext ctx, WalletData.EstimateFeeRequest req, long authUserId, Data.EstimateFeeResponse r) {
        WalletData.EstimateFeeResponse.Builder b = WalletData.EstimateFeeResponse.builder();
        LightningCodec.EstimateFeeCodec.decode(r, b);
        return b.build();
    }

    @Override
    protected void execute(Data.EstimateFeeRequest r, ILightningCallback<Data.EstimateFeeResponse> cb) {
        lnd().client().estimateFee(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, WalletData.EstimateFeeRequest req, WalletData.EstimateFeeResponse rep) {
        // noop
    }

    @Override
    protected boolean isUserPrivileged(
            WalletData.User user, Transaction<WalletData.EstimateFeeRequest, WalletData.EstimateFeeResponse> tx) {

        return !user.isApp();
    }

    @Override
    protected WalletData.EstimateFeeRequest getData(IPluginData in) {
        in.assignDataType(WalletData.EstimateFeeRequest.class);
        String req = null;
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.EstimateFeeRequest.class;
    }

    @Override
    public String id() {
        return DefaultPlugins.ESTIMATE_FEE;
    }
}

