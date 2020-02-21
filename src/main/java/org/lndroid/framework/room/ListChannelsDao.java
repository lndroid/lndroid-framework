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
import org.lndroid.framework.plugins.ListChannels;

import java.util.ArrayList;
import java.util.List;

class ListChannelsDao implements
        IListDao<WalletData.ListChannelsRequest, WalletData.ListChannelsResult>, IPluginDao,
        ListChannels.IDao
{
    private DaoRoom dao_;

    ListChannelsDao(DaoRoom dao) {
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
    public WalletData.ListChannelsResult list(
            WalletData.ListChannelsRequest req,
            WalletData.ListPage page,
            WalletData.User user) {

        String where = "";
        if (req.onlyOwn())
            where = and(where, "userId = "+user.id());

        if ("open".equals(req.stateFilter()))
            where = and(where, "state = "+WalletData.CHANNEL_STATE_OPEN);
        else if ("failed".equals(req.stateFilter()))
            where = and(where, "state = "+WalletData.CHANNEL_STATE_FAILED);
        else if ("pending".equals(req.stateFilter()))
            where = and(where, "state != "+WalletData.CHANNEL_STATE_OPEN+
                    " AND state != "+WalletData.CHANNEL_STATE_FAILED);

        String sort = "id";
        if ("active".equals(req.sort()))
            sort = "active";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id FROM Channel "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.list(new SimpleSQLiteQuery(query), page);
    }

    @Override
    public boolean hasPrivilege(WalletData.ListChannelsRequest req, WalletData.User user) {
        return false;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    static abstract class DaoRoom {

        @RawQuery
        abstract long[] listIds(SupportSQLiteQuery query);

        @Query("SELECT * FROM Channel WHERE id_ IN(:ids)")
        abstract List<RoomData.Channel> list(List<Long> ids);

        @Transaction
        WalletData.ListChannelsResult list(SupportSQLiteQuery query, WalletData.ListPage page) {
            long[] ids = listIds(query);

            List<Long> pageIds = new ArrayList<>();
            final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

            // read matching page ids
            List<RoomData.Channel> items = list(pageIds);

            // sort
            RoomUtils.sortPage(items, pageIds);

            // prepare list result
            ImmutableList.Builder<WalletData.Channel> builder = ImmutableList.builder();
            for(RoomData.Channel d: items) {
                builder.add(d.getData());
            }

            return WalletData.ListChannelsResult.builder()
                    .setCount(ids.length)
                    .setPosition(fromPos)
                    .setItems(builder.build())
                    .build();
        }
    }




}

