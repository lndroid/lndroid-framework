package org.lndroid.framework.dao;

import java.util.List;

import org.lndroid.framework.WalletData;

public interface IInvoiceStateWorkerDao {

    WalletData.Invoice getInvoiceByHash(String hashHex);
    List<WalletData.InvoiceHTLC> getInvoiceHTLCs(long invoiceId);
    long getMaxAddIndex();
    long getMaxSettleIndex();
    long insertInvoice(WalletData.Invoice invoice);
    void updateInvoiceState(WalletData.Invoice invoice, List<WalletData.InvoiceHTLC> htlcs);
    void settleInvoice(WalletData.Invoice invoice, List<WalletData.InvoiceHTLC> htlcs, WalletData.Payment p);
}
