package org.lndroid.framework.room;

import android.database.DatabaseUtils;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IListDao;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.ListPayments;

class ListPaymentsDao implements
        IListDao<WalletData.ListPaymentsRequest, WalletData.ListPaymentsResult>, IPluginDao,
        ListPayments.IDao
{
    private DaoRoom dao_;

    ListPaymentsDao(DaoRoom dao) {
        dao_ = dao;
    }

    private static String and(String where, String cond) {
        if (where.isEmpty())
            where += " WHERE ";
        else
            where += " AND ";
        where += cond;
        return where;
    }

    @Override
    public WalletData.ListPaymentsResult list(WalletData.ListPaymentsRequest req,
                                              WalletData.ListPage page,
                                              WalletData.User user) {
        String where = "";

        if (req.contactId() != 0) {
            String pubkey = dao_.getContactPubkey(req.contactId());
            if (pubkey != null && !pubkey.isEmpty()) {
                where = and(where, "peerPubkey = " + DatabaseUtils.sqlEscapeString(pubkey));
            } else {
                ImmutableList.Builder<WalletData.Payment> ib = ImmutableList.builder();
                return WalletData.ListPaymentsResult.builder()
                        .setItems(ib.build())
                        .build();
            }
        }

        // onlyOwn takes priority
        if (req.onlyOwn())
            where = and(where, "userId = "+user.id());
        else if (req.userId() != 0)
            where = and(where, "userId = "+req.userId());

        if (req.type() != 0)
            where = and(where, "type = "+req.type());
        if (req.sourceId() != 0)
            where = and(where, "sourceId = "+req.sourceId());
        if (req.timeFrom() != 0)
            where = and(where, "time <= "+req.timeFrom());
        if (req.timeTill() != 0)
            where = and(where, "time <= "+req.timeTill());
        if (req.onlyMessages())
            where = and(where, "message IS NOT NULL");

        String sort = "id_";
        if ("time".equals(req.sort()))
            sort = "time";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id_ FROM Payment "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.listPayments(new SimpleSQLiteQuery(query), page);
    }

    @Override
    public boolean hasPrivilege(WalletData.ListPaymentsRequest req, WalletData.User user) {
        if (req.contactId() != 0)
            return dao_.hasContactPaymentsPrivilege(user.id(), req.contactId());
        return false;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {

        @RawQuery
        abstract long[] listPaymentIds(SupportSQLiteQuery query);

        @Query("SELECT * FROM Payment WHERE id_ IN(:ids)")
        abstract List<RoomData.Payment> listPayments(List<Long> ids);

        @Query("SELECT id FROM ContactPaymentsPrivilege WHERE userId = :userId AND contactId = :contactId")
        abstract boolean hasContactPaymentsPrivilege(long userId, long contactId);

        @Query("SELECT pubkey FROM Contact WHERE id = :contactId")
        abstract String getContactPubkey(long contactId);

        @Query("SELECT * FROM SendPayment WHERE id_ IN(:ids)")
        abstract List<RoomData.SendPayment> listSendPayments(List<Long> ids);

        @Query("SELECT * FROM Invoice WHERE id_ IN(:ids)")
        abstract List<RoomData.Invoice> listInvoices(List<Long> ids);

        @Transaction
        WalletData.ListPaymentsResult listPayments(SupportSQLiteQuery query, WalletData.ListPage page) {
            long[] ids = listPaymentIds(query);

            List<Long> pageIds = new ArrayList<>();
            final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

            // read items matching page ids
            List<RoomData.Payment> items = listPayments(pageIds);

            // sort properly
            RoomUtils.sortPage(items, pageIds);

            // collect invoice and sendpayment ids
            List<Long> invoiceIds = new ArrayList<>();
            List<Long> sendPaymentIds = new ArrayList<>();
            for(RoomData.Payment rp: items) {
                WalletData.Payment p = rp.getData();

                switch (p.type()) {
                    case WalletData.PAYMENT_TYPE_INVOICE:
                        invoiceIds.add(p.sourceId());
                        break;
                    case WalletData.PAYMENT_TYPE_SENDPAYMENT:
                        sendPaymentIds.add(p.sourceId());
                        break;
                    default:
                        throw new RuntimeException("Unknown payment type");
                }
            }

            // get invoices and put to map
            Map<Long, WalletData.Invoice> invoices = new HashMap<>();
            for(RoomData.Invoice i: listInvoices(invoiceIds)) {
                invoices.put(i.id_, i.getData());
            }

            // get sendpayments and put to map
            Map<Long, WalletData.SendPayment> sendPayments = new HashMap<>();
            for(RoomData.SendPayment p: listSendPayments(sendPaymentIds)) {
                sendPayments.put(p.id_, p.getData());
            }

            // scan payments in sort order and attach invoice or sendpayment,
            // then put to result list
            ImmutableList.Builder<WalletData.Payment> builder = ImmutableList.builder();
            for(RoomData.Payment rp: items) {
                WalletData.Payment p = rp.getData();

                switch (p.type()) {
                    case WalletData.PAYMENT_TYPE_INVOICE:
                        ImmutableMap.Builder<Long,WalletData.Invoice> ib = ImmutableMap.builder();
                        ib.put(p.sourceId(), invoices.get(p.sourceId()));
                        // FIXME attach InvoiceHTLCs too!
                        p = p.toBuilder().setInvoices(ib.build()).build();
                        break;
                    case WalletData.PAYMENT_TYPE_SENDPAYMENT:
                        ImmutableMap.Builder<Long,WalletData.SendPayment> spb = ImmutableMap.builder();
                        spb.put(p.sourceId(), sendPayments.get(p.sourceId()));
                        // FIXME attach HTLCAttempts too!
                        p = p.toBuilder().setSendPayments(spb.build()).build();
                        break;
                    default:
                        throw new RuntimeException("Unknown payment type");
                }

                builder.add(p);
            }

            // prepare list result
            return WalletData.ListPaymentsResult.builder()
                    .setCount(ids.length)
                    .setPosition(fromPos)
                    .setItems(builder.build())
                    .build();
        }
    }
}
