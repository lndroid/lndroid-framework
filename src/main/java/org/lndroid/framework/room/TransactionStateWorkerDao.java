package org.lndroid.framework.room;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Job;
import org.lndroid.framework.plugins.Transaction;
import org.lndroid.framework.plugins.TransactionStateWorker;

import java.util.ArrayList;
import java.util.List;

public class TransactionStateWorkerDao implements TransactionStateWorker.IDao, IPluginDao {

    private DaoRoom dao_;

    TransactionStateWorkerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        dao_ = dao;
        dao_.txDao = txDao;
    }

    @Override
    public WalletData.Transaction getTransaction(String txHash) {
        RoomData.Transaction t = dao_.getTransaction(txHash);
        return t != null ? t.getData() : null;
    }

    @Override
    public List<Job> getSendingJobs() {
        return dao_.getJobs(Transaction.JOB_STATE_EXECUTING, 0);
    }

    @Override
    public void updateTransaction(@Nullable Job job, WalletData.Transaction t) {
        RoomData.Transaction r = new RoomData.Transaction();
        r.setData(t);
        if (job != null)
            dao_.updateTransaction(job, r);
        else
            dao_.updateTransaction(r);
    }

    public void updateJob(Job job) {
        dao_.updateJob(job);
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {
        RoomTransactions.TransactionDao txDao;

        @Query("SELECT * FROM 'Transaction' WHERE txHash = :txHash")
        abstract RoomData.Transaction getTransaction(String txHash);

        @Query("SELECT * FROM 'Transaction' WHERE id = :id")
        abstract List<RoomData.Transaction> getTransaction(long id);

        private List<Object> fromRoom(List<RoomData.Transaction> rs) {
            List<Object> s = new ArrayList<>();
            for (RoomData.Transaction r: rs) {
                WalletData.Transaction t = r.getData();
                s.add (t);
            }
            return s;
        }

        @androidx.room.Transaction
        public List<Job> getJobs(int state, long now) {
            List<RoomTransactions.RoomTransaction> txs = txDao.getReadyJobTransactions(
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
        void updateJob(Job job) {
            txDao.updateJob(job.pluginId, job.userId, job.txId, job.job);
        }

        @androidx.room.Transaction
        void updateTransaction(Job job, RoomData.Transaction tx) {
            updateJob(job);
            updateTransaction(tx);
        }
    }
}
