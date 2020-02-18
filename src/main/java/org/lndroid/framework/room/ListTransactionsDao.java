package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IListDao;
import org.lndroid.framework.engine.IPluginDao;

import java.util.ArrayList;
import java.util.List;

public class ListTransactionsDao implements
        IListDao<WalletData.ListTransactionsRequest, WalletData.ListTransactionsResult>, IPluginDao {

    private ListTransactionsDaoRoom dao_;

    ListTransactionsDao(ListTransactionsDaoRoom dao) {
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
    public WalletData.ListTransactionsResult list(
            WalletData.ListTransactionsRequest req,
            WalletData.ListPage page,
            WalletData.User user) {

        String where = "";
        if (req.onlyOwn())
            where = and(where, "userId = "+user.id());

        String sort = "id_"; // NOTE, id_ not id
        if ("createTime".equals(req.sort()))
            sort = "createTime";
        else if ("amount".equals(req.sort()))
            sort = "amount";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id_ FROM 'Transaction' "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.list(new SimpleSQLiteQuery(query), page);
    }

    @Override
    public boolean hasPrivilege(WalletData.ListTransactionsRequest req, WalletData.User user) {
        return false;
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class ListTransactionsDaoRoom {

    @RawQuery
    abstract long[] listIds(SupportSQLiteQuery query);

    @Query("SELECT * FROM 'Transaction' WHERE id_ IN(:ids)")
    abstract List<RoomData.Transaction> list(List<Long> ids);

    @Transaction
    WalletData.ListTransactionsResult list(SupportSQLiteQuery query, WalletData.ListPage page) {
        long[] ids = listIds(query);

        List<Long> pageIds = new ArrayList<>();
        final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

        // read matching page ids
        List<RoomData.Transaction> items = list(pageIds);

        // sort
        RoomUtils.sortPage(items, pageIds);

        // prepare list result
        ImmutableList.Builder<WalletData.Transaction> builder = ImmutableList.builder();
        for(RoomData.Transaction t: items)
            builder.add(t.getData());

        return WalletData.ListTransactionsResult.builder()
                .setCount(ids.length)
                .setPosition(fromPos)
                .setItems(builder.build())
                .build();
    }
}



