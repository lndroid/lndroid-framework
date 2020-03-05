package org.lndroid.framework.room;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IPluginDao;
import org.lndroid.framework.plugins.Job;
import org.lndroid.framework.plugins.SubscribeBackgroundInfo;
import org.lndroid.framework.plugins.Transaction;

public class SubscribeBackgroundInfoDao implements SubscribeBackgroundInfo.IDao, IPluginDao {

    private DaoRoom dao_;

    SubscribeBackgroundInfoDao(DaoRoom dao, RoomTransactions.TransactionDao txDao) {
        dao_ = dao;
        dao_.txDao = txDao;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public long getPendingChannelCount() {
        int[] states = {
                WalletData.CHANNEL_STATE_PENDING_CLOSE,
                WalletData.CHANNEL_STATE_PENDING_OPEN,
                WalletData.CHANNEL_STATE_PENDING_FORCE_CLOSE,
                WalletData.CHANNEL_STATE_WAITING_CLOSE
        };
        return dao_.getChannelCount(states);
    }

    @Override
    public List<Job> getActiveJobs() {
        return dao_.getActiveJobs();
    }

    @Dao
    abstract static class DaoRoom {
        RoomTransactions.TransactionDao txDao;

        @Query("SELECT COUNT(id) FROM Channel WHERE state IN (:states)")
        abstract long getChannelCount(int[] states);


        @androidx.room.Transaction
        public List<Job> getActiveJobs() {
            int[] states = {
                    Transaction.JOB_STATE_NEW,
                    Transaction.JOB_STATE_EXECUTING,
                    Transaction.JOB_STATE_LOST,
                    Transaction.JOB_STATE_RETRY
            };
            List<RoomTransactions.RoomTransaction> txs = txDao.getJobTransactions(states);

            List<Job> jobs = new ArrayList<>();
            for(RoomTransactions.RoomTransaction tx: txs) {
                if (tx.txData.responseId == 0)
                    continue;
                Job job = new Job(tx.txData.pluginId, tx.txData.userId, tx.txData.txId);
                job.job = tx.jobData;
                jobs.add(job);
            }

            return jobs;
        }
    }
}
