package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Job;
import org.lndroid.framework.plugins.SendPaymentWorker;
import org.lndroid.framework.plugins.Transaction;

public class SendPaymentWorkerDao implements SendPaymentWorker.IDao, IPluginDao {
    private DaoRoom dao_;

    SendPaymentWorkerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao, RouteHintsDaoRoom routeDao) {
        dao_ = dao;
        dao_.routeDao = routeDao;
        dao_.txDao = txDao;
    }

    @Override
    public List<Job> getSendingJobs() {
        return dao_.getJobs(Transaction.JOB_STATE_EXECUTING, 0);
    }

    @Override
    public List<Job> getPendingJobs(long now) {
        return dao_.getJobs(Transaction.JOB_STATE_NEW, now);
    }

    @Override
    public WalletData.Contact getContact(String contactPubkey) {
        return dao_.getContact(contactPubkey);
    }

    @Override
    public void updateJob(Job j) {
        dao_.updateJob(j);
    }

    @Override
    public void updatePayment(Job job, WalletData.SendPayment p) {
        RoomData.SendPayment rp = new RoomData.SendPayment();
        rp.setData(p);
        dao_.updatePayment(job, rp);
    }

    @Override
    public void settlePayment(Job job, WalletData.SendPayment sp, WalletData.HTLCAttempt htlc) {
        RoomData.SendPayment rsp = new RoomData.SendPayment();
        rsp.setData(sp);
        RoomData.HTLCAttempt rhtlc = new RoomData.HTLCAttempt();
        rhtlc.setData(htlc);
        dao_.settlePayment(job, rsp, rhtlc);
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {

        RouteHintsDaoRoom routeDao;
        RoomTransactions.TransactionDao txDao;

        @Query("SELECT * FROM Contact WHERE pubkey = :pubkey")
        public abstract RoomData.Contact getContactRoom(String pubkey);

        public WalletData.Contact getContact(String contactPubkey) {
            RoomData.Contact rc = getContactRoom(contactPubkey);
            if (rc == null)
                return null;

            return rc.getData().toBuilder()
                    .setRouteHints(routeDao.getRouteHints(RoomData.routeHintsParentId(rc.getData())))
                    .build();
        }

        @Query("SELECT * FROM SendPayment WHERE id = :id")
        abstract List<RoomData.SendPayment> getSendPayment(long id);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updateSendPayment(RoomData.SendPayment p);

        @Query("UPDATE Payment SET peerPubkey = :peerPubkey, sourceHTLCId = :htlcId WHERE type = :type AND sourceId = :sourceId")
        abstract void setPaymentData(int type, long sourceId, String peerPubkey, long htlcId);

        @Insert
        abstract void insertHTLC(RoomData.HTLCAttempt htlc);

        private List<Object> fromRoom(List<RoomData.SendPayment> rsps) {
            List<Object> sps = new ArrayList<>();
            for (RoomData.SendPayment r: rsps) {
                WalletData.SendPayment sp = r.getData();
                sps.add (sp.toBuilder()
                        .setRouteHints(routeDao.getRouteHints(RoomData.routeHintsParentId(sp)))
                        .build());
            }
            return sps;
        }

        @androidx.room.Transaction
        public void updateJob(Job job) {
            txDao.updateJob(job.pluginId, job.userId, job.txId, job.job);
        }

        @androidx.room.Transaction
        public List<Job> getJobs(int state, long now) {
            List<RoomTransactions.RoomTransaction> txs = txDao.getReadyJobTransactions(
                    DefaultPlugins.SEND_PAYMENT, state, now);

            List<Job> jobs = new ArrayList<>();
            for(RoomTransactions.RoomTransaction tx: txs) {
                if (tx.txData.responseId == 0)
                    continue;
                Job job = new Job(tx.txData.pluginId, tx.txData.userId, tx.txData.txId);
                job.job = tx.jobData;
                job.objects = fromRoom(getSendPayment(tx.txData.responseId));
                jobs.add(job);
            }

            return jobs;
        }

        @androidx.room.Transaction
        public void updatePayment(Job job, RoomData.SendPayment sp) {
            updateJob(job);
            updateSendPayment(sp);
            setPaymentData(WalletData.PAYMENT_TYPE_SENDPAYMENT, sp.getData().id(), sp.getData().destPubkey(), 0L);
        }

        @androidx.room.Transaction
        public void settlePayment(Job job, RoomData.SendPayment sp, RoomData.HTLCAttempt htlc) {
            updateJob(job);
            updateSendPayment(sp);
            insertHTLC(htlc);
            setPaymentData(WalletData.PAYMENT_TYPE_SENDPAYMENT, sp.getData().id(),
                    sp.getData().destPubkey(), htlc.getData().id());
        }
    }


}
