package org.lndroid.framework.engine;

public interface IIdGenerator {

    void init();
    <T> long generateId(Class<T> cls);
    <T> long[] generateIds(Class<T> cls, int count);
}
