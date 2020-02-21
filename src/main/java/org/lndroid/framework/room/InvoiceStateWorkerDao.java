package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.InvoiceStateWorker;

class InvoiceStateWorkerDao implements InvoiceStateWorker.IDao, IPluginDao {

    private DaoRoom dao_;

    InvoiceStateWorkerDao(DaoRoom dao) {
        dao_ = dao;
    }

    @Override
    public WalletData.Invoice getInvoiceByHash(String hashHex) {
        RoomData.Invoice r = dao_.getInvoiceByHash(hashHex);
        return r != null ? r.getData() : null;
    }

    @Override
    public List<WalletData.InvoiceHTLC> getInvoiceHTLCs(long invoiceId) {
        List<WalletData.InvoiceHTLC> htlcs = new ArrayList<>();
        List<RoomData.InvoiceHTLC> r = dao_.getInvoiceHTLCs(invoiceId);
        for(RoomData.InvoiceHTLC i: r)
            htlcs.add(i.getData());
        return htlcs;
    }

    @Override
    public List<WalletData.Payment> getInvoicePayments(long invoiceId) {
        List<WalletData.Payment> payments = new ArrayList<>();
        List<RoomData.Payment> r = dao_.getInvoicePayments(WalletData.PAYMENT_TYPE_INVOICE, invoiceId);
        for(RoomData.Payment i: r)
            payments.add(i.getData());
        return payments;
    }

    @Override
    public long getMaxAddIndex() {
        return dao_.getMaxAddIndex();
    }

    @Override
    public long getMaxSettleIndex() {
        return dao_.getMaxSettleIndex();
    }

    @Override
    public void updateInvoiceState(WalletData.Invoice invoice,
                                   List<WalletData.InvoiceHTLC> htlcs,
                                   List<WalletData.Payment> payments) {
        RoomData.Invoice r = new RoomData.Invoice();
        r.setData(invoice);

        List<RoomData.InvoiceHTLC> rhtlcs = new ArrayList<>();
        for (WalletData.InvoiceHTLC h: htlcs) {
            RoomData.InvoiceHTLC rh = new RoomData.InvoiceHTLC();
            rh.setData(h);
            rhtlcs.add(rh);
        }

        List<RoomData.Payment> rpayments = new ArrayList<>();
        for (WalletData.Payment p: payments) {
            RoomData.Payment rp = new RoomData.Payment();
            rp.setData(p);
            rpayments.add(rp);
        }

        dao_.updateInvoiceState(r, rhtlcs, rpayments);
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {
        @Query("SELECT * FROM Invoice WHERE preimageHashHex = :hashHex")
        abstract RoomData.Invoice getInvoiceByHash(String hashHex);

        @Query("SELECT * FROM InvoiceHTLC WHERE invoiceId = :invoiceId")
        abstract List<RoomData.InvoiceHTLC> getInvoiceHTLCs(long invoiceId);

        @Query("SELECT * FROM Payment WHERE type = :type AND sourceId = :invoiceId")
        abstract List<RoomData.Payment> getInvoicePayments(int type, long invoiceId);

        // FIXME these two won't work if we recover our wallet from backup
        //  and thus would have an empty lnd instance?
        @Query("SELECT MAX(addIndex) FROM Invoice")
        abstract long getMaxAddIndex();

        @Query("SELECT MAX(settleIndex) FROM Invoice")
        abstract long getMaxSettleIndex();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void upsertInvoice(RoomData.Invoice i);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void upsertInvoiceHTLCs(List<RoomData.InvoiceHTLC> htlcs);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void upsertPayments(List<RoomData.Payment> ps);

        @Transaction
        void updateInvoiceState(RoomData.Invoice i,
                                List<RoomData.InvoiceHTLC> htlcs,
                                List<RoomData.Payment> payments) {
            upsertInvoice(i);
            upsertInvoiceHTLCs(htlcs);
            upsertPayments(payments);
        }
    }

}
