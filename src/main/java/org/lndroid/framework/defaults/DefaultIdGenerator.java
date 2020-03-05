package org.lndroid.framework.defaults;

import org.lndroid.framework.engine.IDaoProvider;
import org.lndroid.framework.engine.IIdGenerator;

public class DefaultIdGenerator implements IIdGenerator {

    private IDaoProvider daos_;
    private long nextId_ = 2; // 1 is reserved for root-user

    public DefaultIdGenerator(IDaoProvider daos) {
        daos_ = daos;
    }

    private void putNextId(long id) {
        daos_.getRawQueryDao().execute("UPDATE NextId SET id = "+id+" WHERE pk_ = 0");
    }

    private long getNextId() {
        return daos_.getRawQueryDao().getLong("SELECT id FROM NextId WHERE pk_ = 0");
    }

    @Override
    public void init() {
        daos_.getRawQueryDao().execute("INSERT OR IGNORE INTO NextId (pk_, id) VALUES (0, "+nextId_+")");
        nextId_ = getNextId();
    }

    @Override
    public <T> long generateId(Class<T> cls) {
        return generateIds(cls, 1)[0];
    }

    @Override
    public <T> long[] generateIds(Class<T> cls, int count) {
        if (count <= 0)
            throw new RuntimeException("Bad generateIds count");

        long[] ids = new long[count];
        for(long i = 0; i < count; i++)
            ids[(int)i] = nextId_ + i;

        nextId_ += count;

        putNextId(nextId_);

        return ids;
    }
}
