package org.lndroid.framework.room;

import android.database.DatabaseUtils;

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
import org.lndroid.framework.plugins.ListUsers;

import java.util.ArrayList;
import java.util.List;

public class ListUsersDao implements
        IListDao<WalletData.ListUsersRequest, WalletData.ListUsersResult>, IPluginDao,
        ListUsers.IDao
{

    private DaoRoom dao_;

    ListUsersDao(DaoRoom dao) {
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
    public WalletData.ListUsersResult list(
            WalletData.ListUsersRequest req,
            WalletData.ListPage page,
            WalletData.User user) {

        String where = "";
        // FIXME add userId field first!
//        if (req.onlyOwn())
//            where = and(where, "userId = "+user.id());
        if (req.role() != null)
            where = and(where, "role = "+ DatabaseUtils.sqlEscapeString(req.role()));

        String sort = "id";
        if ("name".equals(req.sort()))
            sort = "appLabel";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id_ FROM User "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.list(new SimpleSQLiteQuery(query), page);
    }

    @Override
    public boolean hasPrivilege(WalletData.ListUsersRequest req, WalletData.User user) {
        return false;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {

        @RawQuery
        abstract long[] listIds(SupportSQLiteQuery query);

        @Query("SELECT * FROM User WHERE id_ IN(:ids)")
        abstract List<RoomData.User> list(List<Long> ids);

        @Transaction
        WalletData.ListUsersResult list(SupportSQLiteQuery query, WalletData.ListPage page) {
            long[] ids = listIds(query);

            List<Long> pageIds = new ArrayList<>();
            final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

            // read matching page ids
            List<RoomData.User> items = list(pageIds);

            // sort
            RoomUtils.sortPage(items, pageIds);

            // prepare list result
            ImmutableList.Builder<WalletData.User> builder = ImmutableList.builder();
            for(RoomData.User t: items)
                builder.add(t.getData());

            return WalletData.ListUsersResult.builder()
                    .setCount(ids.length)
                    .setPosition(fromPos)
                    .setItems(builder.build())
                    .build();
        }
    }
}
