package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.dao.IInvoiceStateWorkerDao;
import org.lndroid.framework.engine.IPluginDao;

class InvoiceStateWorkerDao implements IInvoiceStateWorkerDao, IPluginDao {
    private InvoiceStateWorkerDaoRoom dao_;

    InvoiceStateWorkerDao(InvoiceStateWorkerDaoRoom dao) {
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
    public long getMaxAddIndex() {
        return dao_.getMaxAddIndex();
    }

    @Override
    public long getMaxSettleIndex() {
        return dao_.getMaxSettleIndex();
    }

    private List<RoomData.InvoiceHTLC> prepareHTLCs(List<WalletData.InvoiceHTLC> htlcs) {
        List<RoomData.InvoiceHTLC> rhtlcs = new ArrayList<>();
        for (WalletData.InvoiceHTLC h: htlcs) {
            RoomData.InvoiceHTLC rh = new RoomData.InvoiceHTLC();
            rh.setData(h);
            rhtlcs.add(rh);
        }
        return rhtlcs;
    }

    @Override
    public long insertInvoice(WalletData.Invoice i) {
        RoomData.Invoice r = new RoomData.Invoice();
        r.setData(i);
        return dao_.insertInvoice(r);
    }

    @Override
    public void updateInvoiceState(WalletData.Invoice invoice, List<WalletData.InvoiceHTLC> htlcs) {
        RoomData.Invoice r = new RoomData.Invoice();
        r.setData(invoice);

        List<RoomData.InvoiceHTLC> rhtlcs = prepareHTLCs(htlcs);

        dao_.updateInvoiceState(r, rhtlcs);
    }

    @Override
    public void settleInvoice(WalletData.Invoice invoice, List<WalletData.InvoiceHTLC> htlcs, WalletData.Payment p) {
        RoomData.Invoice r = new RoomData.Invoice();
        r.setData(invoice);

        List<RoomData.InvoiceHTLC> rhtlcs = prepareHTLCs(htlcs);

        dao_.settleInvoice(r, rhtlcs, p);
    }

    @Override
    public void init() {
        // noop
    }
}

@Dao
abstract class InvoiceStateWorkerDaoRoom {
    @Query("SELECT * FROM Invoice WHERE preimageHashHex = :hashHex")
    abstract RoomData.Invoice getInvoiceByHash(String hashHex);

    @Query("SELECT * FROM InvoiceHTLC WHERE invoiceId = :invoiceId")
    abstract List<RoomData.InvoiceHTLC> getInvoiceHTLCs(long invoiceId);

    // FIXME these two won't work if we recover our wallet from backup
    //  and thus would have an empty lnd instance?
    @Query("SELECT MAX(addIndex) FROM Invoice")
    abstract long getMaxAddIndex();

    @Query("SELECT MAX(settleIndex) FROM Invoice")
    abstract long getMaxSettleIndex();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long updateInvoice(RoomData.Invoice i);
    @Query("UPDATE Invoice SET id = id_ WHERE id_ = :id")
    abstract void setInvoiceId(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long[] updateInvoiceHTLCs(List<RoomData.InvoiceHTLC> htlcs);
    @Query("UPDATE InvoiceHTLC SET id = id_ WHERE id_ IN (:ids)")
    abstract void setInvoiceHTLCIds(long[] ids);

    @Query("DELETE FROM Payment WHERE type = :type AND sourceId = :sourceId")
    abstract void deleteInvoicePayments(int type, long sourceId);
    @Insert
    abstract long[] insertPayments(List<RoomData.Payment> ps);
    @Query("UPDATE Payment SET id = id_ WHERE id_ IN (:ids)")
    abstract void setPaymentIds(long[] ids);

    @Transaction
    long insertInvoice(RoomData.Invoice i) {
        final long invoiceId = updateInvoice(i);
        setInvoiceId(invoiceId);
        return invoiceId;
    }

    @Transaction
    void updateInvoiceState(RoomData.Invoice i, List<RoomData.InvoiceHTLC> htlcs) {
        final long invoiceId = updateInvoice(i);
        setInvoiceId(invoiceId);
        final long[] htlcIds = updateInvoiceHTLCs(htlcs);
        setInvoiceHTLCIds(htlcIds);
    }

    @Transaction
    void settleInvoice(RoomData.Invoice invoice, List<RoomData.InvoiceHTLC> htlcs, WalletData.Payment p) {
        final long invoiceId = updateInvoice(invoice);
        setInvoiceId(invoiceId);
        final long[] htlcIds = updateInvoiceHTLCs(htlcs);
        setInvoiceHTLCIds(htlcIds);

        // FIXME this is ugly bullshit! auto-incremented ids generated at db level
        // are a huge pain source. This code was placed here until we move to
        // using custom id generator:
        // - id generator available to each plugin
        // - all ids are generated at plugin level
        // - db layer just writes provided ids and does not use auto-increments
        // - this code is moved to the plugin level where we can link htlcs to payments
        // as all ids are known
        List<RoomData.Payment> rps = new ArrayList<>();
        for(RoomData.InvoiceHTLC htlc: htlcs) {
            RoomData.Payment rp = new RoomData.Payment();
            rp.setData(p.toBuilder()
                    .setMessage(htlc.getData().message())
                    .setPeerPubkey(htlc.getData().senderPubkey())
                    .setTime(htlc.getData().senderTime() != 0 ? htlc.getData().senderTime() : invoice.getData().settleTime())
                    .build()
            );
            rps.add(rp);
        }

        deleteInvoicePayments(WalletData.PAYMENT_TYPE_INVOICE, invoiceId);
        final long[] paymentIds = insertPayments(rps);
        setPaymentIds(paymentIds);

//        final long paymentId = updatePayment(p);
  //      setPaymentId(paymentId);
    }
}
