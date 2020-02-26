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
import org.lndroid.framework.plugins.ListPeers;

import java.util.ArrayList;
import java.util.List;

class ListPeersDao implements
        IListDao<WalletData.ListPeersRequest, WalletData.ListPeersResult>, IPluginDao,
        ListPeers.IDao
{
    private DaoRoom dao_;

    ListPeersDao(DaoRoom dao) {
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
    public WalletData.ListPeersResult list(
            WalletData.ListPeersRequest req,
            WalletData.ListPage page,
            WalletData.User user) {

        String where = "";
        if (req.authUserId() != 0)
            where = and(where, "authUserId = "+req.authUserId());
        if ("online".equals(req.stateFilter()))
            where = and(where, "online != 0");
        else if ("enabled".equals(req.stateFilter()))
            where = and(where, "disabled != 0");
        else if ("offline".equals(req.stateFilter()))
            where = and(where, "online = 0 AND disabled != 0");
        else if ("disabled".equals(req.stateFilter()))
            where = and(where, "disabled = 0");

        String sort = "id_"; // NOTE, id_ not id
        if ("pubkey".equals(req.sort()))
            sort = "pubkey";
        else if ("address".equals(req.sort()))
            sort = "address";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id FROM Peer "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.list(new SimpleSQLiteQuery(query), page);
    }

    @Override
    public boolean hasPrivilege(WalletData.ListPeersRequest req, WalletData.User user) {
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

        @Query("SELECT * FROM Peer WHERE id_ IN(:ids)")
        abstract List<RoomData.Peer> list(List<Long> ids);

        @Transaction
        WalletData.ListPeersResult list(SupportSQLiteQuery query, WalletData.ListPage page) {
            long[] ids = listIds(query);

            List<Long> pageIds = new ArrayList<>();
            final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

            // read matching page ids
            List<RoomData.Peer> items = list(pageIds);

            // sort
            RoomUtils.sortPage(items, pageIds);

            // prepare list result
            ImmutableList.Builder<WalletData.Peer> builder = ImmutableList.builder();
            for(RoomData.Peer i: items)
                builder.add(i.getData());

            return WalletData.ListPeersResult.builder()
                    .setCount(ids.length)
                    .setPosition(fromPos)
                    .setItems(builder.build())
                    .build();
        }
    }


}
