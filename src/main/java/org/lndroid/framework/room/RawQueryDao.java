package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;

import org.lndroid.framework.dao.IRawQueryDao;

public class RawQueryDao implements IRawQueryDao {

    private RawQueryDaoRoom dao_;
    private SupportSQLiteOpenHelper writer_;

    RawQueryDao(RawQueryDaoRoom dao, SupportSQLiteOpenHelper writer) {
        dao_ = dao;
        writer_ = writer;
    }

    @Override
    public void init() {

    }

    @Override
    public long getLong(String query) {
        return dao_.getLong(new SimpleSQLiteQuery(query));
    }

    @Override
    public String getString(String query) {
        return dao_.getString(new SimpleSQLiteQuery(query));
    }

    @Override
    public void execute(String query) {
        writer_.getWritableDatabase().execSQL(query);
    }
}

@Dao
abstract class RawQueryDaoRoom {
    @RawQuery
    abstract long getLong(SupportSQLiteQuery query);
    @RawQuery
    abstract String getString(SupportSQLiteQuery query);
}
