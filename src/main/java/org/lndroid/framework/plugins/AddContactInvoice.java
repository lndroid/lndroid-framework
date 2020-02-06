package org.lndroid.framework.plugins;

import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.DefaultPlugins;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.dao.ILndActionDao;
import org.lndroid.framework.engine.PluginContext;
import org.lndroid.framework.room.AddContactInvoiceDao;

public class AddContactInvoice extends
        LndActionBase<WalletData.AddContactInvoiceRequest, Data.Invoice, WalletData.AddContactInvoiceResponse, Data.AddInvoiceResponse> {

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
    protected Data.Invoice createLndRequest(ILndActionDao<WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse> actionDao, PluginContext ctx, WalletData.AddContactInvoiceRequest req) {

        AddContactInvoiceDao dao = (AddContactInvoiceDao)actionDao;

        Data.Invoice i = new Data.Invoice();
        i.value = 0; // FIXME maybe min-payment-amount?
        i.expiry = 1; // essentially non-payable invoice
        i.isPrivate = true; // ask to include private channels as route hints
        i.memo = dao.getWalletContactName();

/*        final String pubkey = dao.getWalletPubkey();
        if (pubkey != null) {
            List<WalletData.ChannelEdge> cs = dao.getChannels(pubkey);
            if (cs != null && !cs.isEmpty()) {
                i.routeHints = new ArrayList<>();
                for (WalletData.ChannelEdge c : cs) {
                    final boolean reverse = c.node2Pubkey().equals(pubkey);

                    Data.HopHint hh = new Data.HopHint();
                    hh.chanId = c.channelId();
                    hh.nodeId = reverse ? c.node1Pubkey() : c.node2Pubkey();

                    WalletData.RoutingPolicy rp = reverse ? c.node1Policy() : c.node2Policy();
                    hh.feeBaseMsat = (int)rp.feeBaseMsat();
                    hh.feeProportionalMillionths = (int)rp.feeRateMilliMsat();
                    hh.cltvExpiryDelta = rp.timeLockDelta();

                    Data.RouteHint rh = new Data.RouteHint();
                    rh.hopHints = new ArrayList<>();
                    rh.hopHints.add(hh);
                    i.routeHints.add(rh);
                }
            }
        }
 */

        return i;
    }

    @Override
    protected WalletData.AddContactInvoiceResponse createResponse(
            PluginContext ctx, WalletData.AddContactInvoiceRequest request, int authUserId, Data.AddInvoiceResponse r) {
        return WalletData.AddContactInvoiceResponse.builder()
                .setPaymentRequest(r.paymentRequest)
                .build();
    }

    @Override
    protected void execute(Data.Invoice r, ILightningCallback<Data.AddInvoiceResponse> cb) {
        lnd().client().addInvoice(r, cb);
    }

    @Override
    protected void signal(PluginContext ctx, WalletData.AddContactInvoiceRequest req, WalletData.AddContactInvoiceResponse rep) {
        // we don't store the created invoice, so probably makes no sense to notify anyone atm
    }

    @Override
    protected boolean isUserPrivileged(WalletData.User user, Transaction<WalletData.AddContactInvoiceRequest, WalletData.AddContactInvoiceResponse> tx) {
        return user.isRoot();
    }

    @Override
    protected WalletData.AddContactInvoiceRequest getData(IPluginData in) {
        in.assignDataType(WalletData.AddContactInvoiceRequest.class);
        try {
            return in.getData();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Type getResponseType() {
        return WalletData.AddContactInvoiceResponse.class;
    }

    @Override
    public String id() {
        return DefaultPlugins.ADD_CONTACT_INVOICE;
    }
}


