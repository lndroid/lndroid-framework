package org.lndroid.framework.dao;

import org.lndroid.framework.WalletData;

public interface IGetInvoiceDao {
    WalletData.Invoice get(long id);
}
