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

public class ListUtxoDao implements
        IListDao<WalletData.ListUtxoRequest, WalletData.ListUtxoResult>, IPluginDao {

    private Room dao_;

    ListUtxoDao(Room dao) {
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
    public WalletData.ListUtxoResult list(
            WalletData.ListUtxoRequest req,
            WalletData.ListPage page,
            WalletData.User user) {

        String where = "";
        if (req.maxConfirmations() != 0)
            where = and(where, "confirmations <= "+req.maxConfirmations());
        if (req.minConfirmations() != 0)
            where = and(where, "confirmations >= "+req.minConfirmations());

        String sort = "id";
        if ("amount".equals(req.sort()))
            sort = "amount";
        else if ("confirmations".equals(req.sort()))
            sort = "confirmations";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id FROM Utxo "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.list(new SimpleSQLiteQuery(query), page);
    }

    @Override
    public boolean hasPrivilege(WalletData.ListUtxoRequest req, WalletData.User user) {
        return false;
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    static abstract class Room {

        @RawQuery
        abstract long[] listIds(SupportSQLiteQuery query);

        @Query("SELECT * FROM Utxo WHERE id_ IN(:ids)")
        abstract List<RoomData.Utxo> list(List<Long> ids);

        @Transaction
        WalletData.ListUtxoResult list(SupportSQLiteQuery query, WalletData.ListPage page) {
            long[] ids = listIds(query);

            List<Long> pageIds = new ArrayList<>();
            final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

            // read matching page ids
            List<RoomData.Utxo> items = list(pageIds);

            // sort
            RoomUtils.sortPage(items, pageIds);

            // prepare list result
            ImmutableList.Builder<WalletData.Utxo> builder = ImmutableList.builder();
            for(RoomData.Utxo d: items) {
                builder.add(d.getData());
            }

            return WalletData.ListUtxoResult.builder()
                    .setCount(ids.length)
                    .setPosition(fromPos)
                    .setItems(builder.build())
                    .build();
        }
    }




}

