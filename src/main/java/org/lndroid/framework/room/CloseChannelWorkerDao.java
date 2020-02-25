package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.CloseChannelWorker;
import org.lndroid.framework.plugins.Job;
import org.lndroid.framework.plugins.Transaction;

import java.util.ArrayList;
import java.util.List;

public class CloseChannelWorkerDao implements CloseChannelWorker.IDao, IPluginDao {

    private DaoRoom dao_;

    CloseChannelWorkerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
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

    @Dao
    abstract static class DaoRoom {
        RoomTransactions.TransactionDao txDao;

        @Query("SELECT * FROM Channel WHERE id = :id")
        abstract List<RoomData.Channel> getChannel(long id);

        @Query("SELECT * FROM txCloseChannelRequest WHERE id_ = :id")
        abstract RoomTransactions.CloseChannelRequest getRequest(long id);

        private List<Object> fromRoom(List<RoomData.Channel> rsps) {
            List<Object> sps = new ArrayList<>();
            for (RoomData.Channel r: rsps) {
                WalletData.Channel sp = r.getData();
                sps.add (sp);
            }
            return sps;
        }

        private Object fromRoom(RoomTransactions.CloseChannelRequest r) {
            return r != null ? r.data : null;
        }

        @androidx.room.Transaction
        public List<Job> getJobs(int state, long now) {
            List<RoomTransactions.RoomTransaction> txs = txDao.getJobTransactions(
                    DefaultPlugins.CLOSE_CHANNEL, state, now);

            List<Job> jobs = new ArrayList<>();
            for(RoomTransactions.RoomTransaction tx: txs) {
                if (tx.txData.responseId == 0 || tx.txData.requestId == 0)
                    continue;
                Job job = new Job(tx.txData.pluginId, tx.txData.userId, tx.txData.txId);
                job.job = tx.jobData;
                job.request = fromRoom(getRequest(tx.txData.requestId));
                job.objects = fromRoom(getChannel(tx.txData.responseId));
                jobs.add(job);
            }

            return jobs;
        }

        @androidx.room.Transaction
        void updateJob(Job job) {
            txDao.updateJob(job.pluginId, job.userId, job.txId, job.job);
        }
    }

}

