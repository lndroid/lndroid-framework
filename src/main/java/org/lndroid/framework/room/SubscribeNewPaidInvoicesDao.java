package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.google.common.collect.ImmutableMap;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.SubscribeNewPaidInvoices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscribeNewPaidInvoicesDao implements SubscribeNewPaidInvoices.IDao, IPluginDao {

    private DaoRoom dao_;

    SubscribeNewPaidInvoicesDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Payment getPayment(String protocol, long invoiceId) {
        // FIXME only messages are supported atm
        if (!WalletData.PROTOCOL_MESSAGES.equals(protocol))
            return null;

        RoomData.Payment p = dao_.getMessagePayment(WalletData.PAYMENT_TYPE_INVOICE, invoiceId);
        return p != null ? p.getData() : null;
    }

    @Override
    public List<WalletData.Payment> getPayments(String protocol) {
        if (WalletData.PROTOCOL_MESSAGES.equals(protocol))
            return null;

        return dao_.getMessagePayments();
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {
        @Query("SELECT * FROM Payment WHERE message != null AND type = :type AND sourceId = :invoiceId")
        abstract RoomData.Payment getMessagePayment(int type, long invoiceId);

        @Query("SELECT * FROM Payment WHERE type = :type AND sourceId IN (:invoiceIds)")
        abstract List<RoomData.Payment> getPayments(int type, List<Long> invoiceIds);

        @Query("SELECT * FROM Invoice WHERE settleTime != 0 AND notifyTime = 0 AND isKeysend != 0")
        abstract List<RoomData.Invoice> getKeysendInvoices();

        @Transaction
        List<WalletData.Payment> getMessagePayments() {
            List<RoomData.Invoice> ins = getKeysendInvoices();
            Map<Long, ImmutableMap<Long, WalletData.Invoice>> invoices = new HashMap<>();
            List<Long> invoiceIds = new ArrayList<>();
            for(RoomData.Invoice i: ins) {
                ImmutableMap.Builder<Long, WalletData.Invoice> ib = ImmutableMap.builder();
                ib.put(i.getData().id(), i.getData());
                invoices.put(i.getData().id(), ib.build());
                invoiceIds.add(i.getData().id());
            }

            List<RoomData.Payment> ps = getPayments(WalletData.PAYMENT_TYPE_INVOICE, invoiceIds);
            List<WalletData.Payment> payments = new ArrayList<>();
            for(RoomData.Payment rp: ps) {
                WalletData.Payment p = rp.getData();
                if (p.message() == null)
                    continue;

                payments.add(p.toBuilder().setInvoices(invoices.get(p.sourceId())).build());
            }

            return payments;

        }
    }
}

