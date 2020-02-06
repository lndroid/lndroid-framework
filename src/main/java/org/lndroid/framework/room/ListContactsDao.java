package org.lndroid.framework.room;

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

public class ListContactsDao implements IPluginDao {

    private ListContactsDaoRoom dao_;

    ListContactsDao(ListContactsDaoRoom dao) {
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

    public WalletData.ListContactsResult list(
            WalletData.ListContactsRequest req,
            WalletData.ListPage page,
            int callerUserId,
            boolean clearPubkey) {

        String where = "";
        if (req.onlyOwn())
            where = and(where, "userId = "+callerUserId);

        String sort = "id_"; // NOTE, id_ not id
        if ("createTime".equals(req.sort()))
            sort = "createTime";
        else if ("name".equals(req.sort()))
            sort = "name";

        final String desc = req.sortDesc() ? "DESC" : "ASC";
        final String query = "SELECT id_ FROM Contact "+where+
                " ORDER BY "+sort+" "+desc;
        return dao_.listContacts(new SimpleSQLiteQuery(query), page, clearPubkey);
    }

    public boolean hasPrivilege(int userId) {
        return dao_.hasListContactsPrivilege(userId);
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class ListContactsDaoRoom {

    @RawQuery
    abstract long[] listContactIds(SupportSQLiteQuery query);

    @Query("SELECT * FROM Contact WHERE id_ IN(:ids)")
    abstract List<RoomData.Contact> listContacts(List<Long> ids);

    @Query("SELECT id FROM ListContactsPrivilege WHERE userId = :userId")
    abstract boolean hasListContactsPrivilege(int userId);

    @Transaction
    WalletData.ListContactsResult listContacts(SupportSQLiteQuery query, WalletData.ListPage page, boolean clearPubkey) {
        long[] ids = listContactIds(query);

        List<Long> pageIds = new ArrayList<>();
        final int fromPos = RoomUtils.preparePageIds(ids, page, pageIds);

        // read matching page ids
        List<RoomData.Contact> items = listContacts(pageIds);

        // sort
        RoomUtils.sortPage(items, pageIds);

        // prepare list result
        ImmutableList.Builder<WalletData.Contact> builder = ImmutableList.builder();
        for(RoomData.Contact in: items) {
            if (clearPubkey) {
                builder.add(in.getData().toBuilder().setPubkey(null).build());
            } else {
                builder.add(in.getData());
            }
        }

        return WalletData.ListContactsResult.builder()
                .setCount(ids.length)
                .setPosition(fromPos)
                .setItems(builder.build())
                .build();
    }
}


