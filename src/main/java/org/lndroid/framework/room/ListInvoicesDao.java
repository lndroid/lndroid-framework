package org.lndroid.framework.room;

import android.database.DatabaseUtils;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;

public class ListInvoicesDao implements IPluginDao {

    private ListInvoicesDaoRoom dao_;

    ListInvoicesDao(ListInvoicesDaoRoom dao) {
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

    public WalletData.ListInvoicesResult list(
            WalletData.ListInvoicesRequest req,
            WalletData.ListPage page,
            int callerUserId) {

        String where = "";
        if (req.invoiceId() != 0)
            where = and(where, "id_ = "+req.invoiceId());
        if (req.txId() != null && !req.txId().equals(""))
            where = and(where, "txId = "+DatabaseUtils.sqlEscapeString(req.txId()));
        if (req.preimageHex() != null && !req.preimageHex().equals(""))
            where = and(where, "preimageHex = "+DatabaseUtils.sqlEscapeString(req.preimageHex()));
        if (req.preimageHashHex() != null && !req.preimageHashHex().equals(""))
            where = and(where, "preimageHashHex = "+DatabaseUtils.sqlEscapeString(req.preimageHashHex()));
        if (req.authUserId() != 0)
            where = and(where, "authUserId = "+req.authUserId());
        if (req.createFrom() != 0)
            where = and(where, "createTime >= "+req.createFrom());
        if (req.createTill() != 0)
            where = and(where, "createTime <= "+req.createTill());
        if (req.settleFrom() != 0)
            where = and(where, "settleTime <= "+req.settleFrom());
        if (req.settleTill() != 0)
            where = and(where, "settleTime <= "+req.settleTill());
        if (req.description() != null && !req.description().equals(""))
            where = and(where, "description LIKE '%"+ DatabaseUtils.sqlEscapeString(req.description())+"%'");
        if (req.purpose() != null && !req.purpose().equals(""))
            where = and(where, "purpose LIKE '%"+ DatabaseUtils.sqlEscapeString(req.purpose())+"%'");
        if (req.onlyOwn())
            where = and(where, "userId = "+callerUserId);
        if (req.states() != null && !req.states().isEmpty()) {
            where = and(where, "state IN (-1, ");
            for(int state: req.states()) {
                where += ","+state;
            }
            where += ")";
        }

        String sort = "id_"; // NOTE, id_ not id
        if ("settleTime".equals(req.sort()))
            sort = "settleTime";
        else if ("valueSat".equals(req.sort()))
            sort = "valueSat";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id_ FROM Invoice "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.listInvoices(new SimpleSQLiteQuery(query), page);
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class ListInvoicesDaoRoom {

    @RawQuery
    abstract long[] listInvoiceIds(SupportSQLiteQuery query);

    @Query("SELECT * FROM Invoice WHERE id_ IN(:ids)")
    abstract List<RoomData.Invoice> listInvoices(List<Long> ids);

    @Transaction
    WalletData.ListInvoicesResult listInvoices(SupportSQLiteQuery query, WalletData.ListPage page) {
        long[] ids = listInvoiceIds(query);

        List<Long> pageIds = new ArrayList<>();
        final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

        // read matching page ids
        List<RoomData.Invoice> items = listInvoices(pageIds);

        // sort
        RoomUtils.sortPage(items, pageIds);

        // prepare list result
        ImmutableList.Builder<WalletData.Invoice> builder = ImmutableList.builder();
        for(RoomData.Invoice in: items)
            builder.add(in.getData());

        return WalletData.ListInvoicesResult.builder()
                .setCount(ids.length)
                .setPosition(fromPos)
                .setItems(builder.build())
                .build();
    }
}

