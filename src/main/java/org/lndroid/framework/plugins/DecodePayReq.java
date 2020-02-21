package org.lndroid.framework.plugins;

import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.lnd.LightningCodec;

public class DecodePayReq extends LndActionBase<String, Data.PayReqString, WalletData.SendPayment, Data.PayReq> {

    // plugin's Dao must extend this class
    public interface IDao extends ILndActionDao<String, WalletData.SendPayment> {};

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
    protected Data.PayReqString createLndRequest(PluginContext ctx, String req) {
        Data.PayReqString p = new Data.PayReqString();
        p.payReq = req;
        return p;
    }

    @Override
    protected WalletData.SendPayment createResponse(PluginContext ctx, String req, long authUserId, Data.PayReq r) {
        WalletData.SendPayment.Builder b = WalletData.SendPayment.builder();
        LightningCodec.PayReqConverter.decode(r, b);
        return b.build();
    }

    @Override
    protected void execute(Data.PayReqString r, ILightningCallback<Data.PayReq> cb) {
        lnd().client().decodePayReq(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, String req, WalletData.SendPayment rep) {
        // noop
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<String> tx) {
        return !user.isApp();
    }

    @Override
    protected String getData(IPluginData in) {
        in.assignDataType(String.class);
        String req = null;
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.SendPayment.class;
    }

    @Override
    public String id() {
        return DefaultPlugins.DECODE_PAYREQ;
    }
}
