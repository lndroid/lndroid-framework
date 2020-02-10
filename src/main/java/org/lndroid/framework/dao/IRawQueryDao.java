package org.lndroid.framework.dao;

public interface IRawQueryDao {
    void init();

    // readers
    long getLong(String query);
    String getString(String query);

    // writers
    void execute(String query);
}
