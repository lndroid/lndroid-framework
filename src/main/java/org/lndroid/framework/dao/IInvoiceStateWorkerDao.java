package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface IInvoiceStateWorkerDao {

    WalletData.Invoice getInvoiceByHash(String hashHex);
    List<WalletData.InvoiceHTLC> getInvoiceHTLCs(long invoiceId);
    List<WalletData.Payment> getInvoicePayments(long invoiceId);
    long getMaxAddIndex();
    long getMaxSettleIndex();
    void updateInvoiceState(WalletData.Invoice invoice,
                            List<WalletData.InvoiceHTLC> htlcs,
                            List<WalletData.Payment> payments);
}
