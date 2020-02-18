package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IListDao<Request, Result> {
    Result list(Request req, WalletData.ListPage page, WalletData.User user);
    boolean hasPrivilege(Request req, WalletData.User user);
}
