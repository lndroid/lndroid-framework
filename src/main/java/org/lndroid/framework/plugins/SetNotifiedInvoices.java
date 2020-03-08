package org.lndroid.framework.plugins;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.IActionDao;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.PluginContext;

import java.io.IOException;
import java.lang.reflect.Type;

public class SetNotifiedInvoices
        extends ActionBase<WalletData.NotifiedInvoicesRequest, WalletData.NotifiedInvoicesResponse> {

    // plugin's Dao must implement this
    public interface IDao extends IActionDao<WalletData.NotifiedInvoicesRequest, WalletData.NotifiedInvoicesResponse> {
        void setNotifyTime(ImmutableList<Long> invoiceIds);
    };

    private static int DEFAULT_TIMEOUT = 60000; // 60 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    @Override
    protected boolean isAuthNotSupported() { return true; }

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected boolean isUserPrivileged(WalletData.NotifiedInvoicesRequest req, WalletData.User user) {
        // FIXME add some privilege?
        return !user.isAnonymous();
    }

    protected IDao dao() { return (IDao)super.dao();}

    @Override
    protected WalletData.NotifiedInvoicesResponse createResponse(
            PluginContext ctx, WalletData.NotifiedInvoicesRequest req, long authUserId) {

        dao().setNotifyTime(req.invoiceIds());

        return WalletData.NotifiedInvoicesResponse.builder().build();
    }

    @Override
    protected void signal(WalletData.NotifiedInvoicesResponse rep) {
    }

    @Override
    protected Type getResponseType() {
        return WalletData.NotifiedInvoicesResponse.class;
    }

    @Override
    protected boolean isValidUser(WalletData.User user) {
        return true;
    }

    @Override
    protected WalletData.NotifiedInvoicesRequest getRequestData(IPluginData in) {
        in.assignDataType(WalletData.NotifiedInvoicesRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String id() {
        return DefaultPlugins.SET_NOTIFIED_INVOICES;
    }

}
