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

public class AddInvoice extends
        LndActionBase<WalletData.AddInvoiceRequest, Data.Invoice, WalletData.Invoice, Data.AddInvoiceResponse>
{
    // plugin's Dao must extend this class
    public interface IDao extends ILndActionDao<WalletData.AddInvoiceRequest, WalletData.Invoice> {};

    private static int DEFAULT_TIMEOUT = 30000; // 30 sec
    private static int MAX_TIMEOUT = 300000; // 5 min

    @Override
    protected int defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected int maxTimeout() {
        return MAX_TIMEOUT;
    }

    @Override
    protected Data.Invoice createLndRequest(PluginContext ctx, WalletData.AddInvoiceRequest req) {
        Data.Invoice i = new Data.Invoice();
        LightningCodec.AddInvoiceCodec.encode(req, i);
        return i;
    }

    @Override
    protected WalletData.Invoice createResponse(
            PluginContext ctx, WalletData.AddInvoiceRequest request, long authUserId, Data.AddInvoiceResponse r) {
        // convert lndRequest to invoice
        WalletData.Invoice.Builder b = WalletData.Invoice.builder()
                .setId(server().getIdGenerator().generateId(WalletData.Invoice.class))
                .setTxId(ctx.txId)
                .setUserId(ctx.user.id())
                .setPurpose(request.purpose())
                .setPreimageHex(request.preimageHex())
                .setValueSat(request.valueSat())
                .setExpiry(request.expiry())
                .setDescription(request.description())
                .setDescriptionHashHex(request.descriptionHashHex())
                .setFallbackAddr(request.fallbackAddr())
                .setAuthUserId(authUserId);

        LightningCodec.AddInvoiceCodec.decode(r, b);

        return b.build();
    }

    @Override
    protected void execute(Data.Invoice r, ILightningCallback<Data.AddInvoiceResponse> cb) {
        lnd().client().addInvoice(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, WalletData.AddInvoiceRequest req, WalletData.Invoice rep) {
        engine().onSignal(id(), DefaultTopics.NEW_INVOICE, rep);
        engine().onSignal(id(), DefaultTopics.INVOICE_STATE, rep);
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.AddInvoiceRequest> tx) {
        return user.isRoot();
    }

    @Override
    protected WalletData.AddInvoiceRequest getData(IPluginData in) {
        in.assignDataType(WalletData.AddInvoiceRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.Invoice.class;
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_INVOICE;
    }
}

