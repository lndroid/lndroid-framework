package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.ChannelStateWorker;
import org.lndroid.framework.plugins.Job;
import org.lndroid.framework.plugins.Transaction;

import java.util.ArrayList;
import java.util.List;

public class ChannelStateWorkerDao implements
        ChannelStateWorker.IDao, IPluginDao
{

    private DaoRoom dao_;

    ChannelStateWorkerDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        dao_ = dao;
        dao_.txDao = txDao;
    }

    @Override
    public WalletData.Channel getChannelByChannelPoint(String channelPoint) {
        RoomData.Channel c = dao_.getChannelByChannelPoint(channelPoint);
        return c != null ? c.getData() : null;
    }

    @Override
    public List<Job> getOpeningJobs() {
        return dao_.getJobs(DefaultPlugins.OPEN_CHANNEL, Transaction.JOB_STATE_EXECUTING, 0);
    }

    @Override
    public List<Job> getClosingJobs() {
        return dao_.getJobs(DefaultPlugins.CLOSE_CHANNEL, Transaction.JOB_STATE_EXECUTING, 0);
    }

    @Override
    public void updateChannel(Job job, WalletData.Channel c) {
        RoomData.Channel r = new RoomData.Channel();
        r.setData(c);
        dao_.updateChannel(job, r);
    }

    @Override
    public void updateJob(Job job) {
        dao_.updateJob(job);
    }

    @Override
    public void setChannelActive(String channelPoint, boolean active) {
        dao_.setChannelActive(channelPoint, active);
    }

    @Override
    public void init() {
        // noop
    }

    @Dao
    abstract static class DaoRoom {
        RoomTransactions.TransactionDao txDao;

        @Query("SELECT * FROM Channel WHERE channelPoint = :channelPoint")
        abstract RoomData.Channel getChannelByChannelPoint(String channelPoint);

        @Query("SELECT * FROM Channel WHERE id = :id")
        abstract List<RoomData.Channel> getChannel(long id);

        @Query("SELECT * FROM Channel WHERE state = :state")
        abstract List<RoomData.Channel> getChannels(int state);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void updateChannel(RoomData.Channel c);

        @Query("UPDATE Channel SET active = :active WHERE channelPoint = :channelPoint")
        abstract void setChannelActive(String channelPoint, boolean active);

        private List<Object> fromRoom(List<RoomData.Channel> rs) {
            List<Object> s = new ArrayList<>();
            for (RoomData.Channel r : rs) {
                WalletData.Channel t = r.getData();
                s.add(t);
            }
            return s;
        }

        @androidx.room.Transaction
        public List<Job> getJobs(String pluginId, int state, long now) {
            List<RoomTransactions.RoomTransaction> txs = txDao.getJobTransactions(
                    pluginId, state, now);

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

        @androidx.room.Transaction
        void updateJob(Job job) {
            txDao.updateJob(job.pluginId, job.userId, job.txId, job.job);
        }

        @androidx.room.Transaction
        void updateChannel(Job job, RoomData.Channel c) {
            if (job != null)
                updateJob(job);
            updateChannel(c);
        }

    }

}

