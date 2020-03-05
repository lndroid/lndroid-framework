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
import org.lndroid.framework.plugins.OpenChannelWorker;
import org.lndroid.framework.plugins.Transaction;

class OpenChannelWorkerDao implements OpenChannelWorker.IDao, IPluginDao {

    private DaoRoom dao_;

    OpenChannelWorkerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        dao_ = dao;
        dao_.txDao = txDao;
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
    public List<Job> getExecutingJobs() {
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
    public void updateChannel(Job j, WalletData.Channel c) {
        RoomData.Channel d = new RoomData.Channel();
        d.setData(c);
        dao_.updateChannel(j, d);
    }


    @Dao
    abstract static class DaoRoom {
        RoomTransactions.TransactionDao txDao;

        @Query("SELECT * FROM Channel WHERE id = :id")
        abstract List<RoomData.Channel> getChannel(long id);

        private List<Object> fromRoom(List<RoomData.Channel> rsps) {
            List<Object> sps = new ArrayList<>();
            for (RoomData.Channel r: rsps) {
                WalletData.Channel sp = r.getData();
                sps.add (sp);
            }
            return sps;
        }

        @androidx.room.Transaction
        public List<Job> getJobs(int state, long now) {
            List<RoomTransactions.RoomTransaction> txs = txDao.getReadyJobTransactions(
                    DefaultPlugins.OPEN_CHANNEL, state, now);

            List<Job> jobs = new ArrayList<>();
            for(RoomTransactions.RoomTransaction tx: txs) {
                if (tx.txData.responseId == 0)
                    continue;
                Job job = new Job(tx.txData.pluginId, tx.txData.userId, tx.txData.txId);
                job.job = tx.jobData;
                job.objects = fromRoom(getChannel(tx.txData.responseId));
                jobs.add(job);
            }

            return jobs;
        }

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updateChannel(RoomData.Channel c);

        @androidx.room.Transaction
        void updateJob(Job job) {
            txDao.updateJob(job.pluginId, job.userId, job.txId, job.job);
        }

        @androidx.room.Transaction
        void updateChannel(Job job, RoomData.Channel c) {
            updateJob(job);
            updateChannel(c);
        }
    }

}
