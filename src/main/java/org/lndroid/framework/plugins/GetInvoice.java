package org.lndroid.framework.plugins;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.dao.IGetInvoiceDao;
import org.lndroid.framework.engine.IDaoProvider;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.engine.IPluginForegroundCallback;
import org.lndroid.framework.engine.PluginContext;

public class GetInvoice extends GetBase<Long> {
    private static final String TAG = "GetWalletBalance";
    private static final long DEFAULT_TIMEOUT = 3600000; // 1h

    private IGetInvoiceDao dao_;

    public GetInvoice() {
        super(DefaultPlugins.GET_INVOICE, DefaultTopics.INVOICE_STATE);
    }

    @Override
    public void init(IDaoProvider dp, IPluginForegroundCallback engine) {
        super.init(engine);
        dao_ = (IGetInvoiceDao)dp.getPluginDao(id());
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
        return WalletData.Invoice.class;
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
