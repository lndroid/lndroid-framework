package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Job;
import org.lndroid.framework.plugins.SendCoinsWorker;
import org.lndroid.framework.plugins.Transaction;

import java.util.ArrayList;
import java.util.List;

public class SendCoinsWorkerDao implements SendCoinsWorker.IDao, IPluginDao {
    private DaoRoom dao_;

    SendCoinsWorkerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        dao_ = dao;
        dao_.txDao = txDao;
    }

    private List<WalletData.Transaction> fromRoom(List<RoomData.Transaction> list) {
        List<WalletData.Transaction> r = new ArrayList<>();
        for (RoomData.Transaction c: list)
            r.add(c.getData());
        return r;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public List<Job> getNewJobs(long now) {
        return dao_.getJobs(Transaction.JOB_STATE_NEW, now);
    }

    @Override
    public List<Job> getSendingJobs() {
        return dao_.getJobs(Transaction.JOB_STATE_EXECUTING, 0);
    }

    @Override
    public List<Job> getRetryJobs() {
        return dao_.getJobs(Transaction.JOB_STATE_RETRY, 0);
    }

    @Override
    public void updateJob(Job j) {
        dao_.updateJob(j);
    }

    @Override
    public void updateTransaction(Job j, WalletData.Transaction t) {
        RoomData.Transaction d = new RoomData.Transaction();
        d.setData(t);
        dao_.updateTransaction(j, d);
    }

    @Dao
    abstract static class DaoRoom {
        RoomTransactions.TransactionDao txDao;

        private List<Object> fromRoom(List<RoomData.Transaction> rs) {
            List<Object> s = new ArrayList<>();
            for (RoomData.Transaction r: rs) {
                s.add (r.getData());
            }
            return s;
        }

        @androidx.room.Transaction
        public void updateJob(Job job) {
            txDao.updateJob(job.pluginId, job.userId, job.txId, job.job);
        }

        @Query("SELECT * FROM 'Transaction' WHERE id = :id")
        abstract List<RoomData.Transaction> getTransaction(long id);

        @androidx.room.Transaction
        public List<Job> getJobs(int state, long now) {
            List<RoomTransactions.RoomTransaction> txs = txDao.getJobTransactions(
                    DefaultPlugins.SEND_COINS, state, now);

            List<Job> jobs = new ArrayList<>();
            for(RoomTransactions.RoomTransaction tx: txs) {
                if (tx.txData.responseId == 0)
                    continue;
                Job job = new Job(tx.txData.pluginId, tx.txData.userId, tx.txData.txId);
                job.job = tx.jobData;
                job.objects = fromRoom(getTransaction(tx.txData.responseId));
                jobs.add(job);
            }

            return jobs;
        }


        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updateTransaction(RoomData.Transaction c);

        @androidx.room.Transaction
        public void updateTransaction(Job job, RoomData.Transaction t) {
            updateJob(job);
            updateTransaction(t);
        }
    }
}
