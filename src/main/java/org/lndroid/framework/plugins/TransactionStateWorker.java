package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.util.ArrayList;
import java.util.List;

public class TransactionStateWorker implements IPluginBackground {

    public interface IDao {
        WalletData.Transaction getTransaction(String txHash);
        List<Job> getSendingJobs();
        void updateTransaction(Job job, WalletData.Transaction t);
        void updateJob(Job job);
    }

    private static final String TAG = "TransactionStateWorker";

    private IPluginServer server_;
    private IDao dao_;
    private ILightningDao lnd_;
    private IPluginBackgroundCallback engine_;
    private boolean started_;
    private boolean synched_;
    private List<Data.Transaction> buffer_ = new ArrayList<>();

    @Override
    public String id() {
        return DefaultPlugins.TRANSACTION_STATE_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        server_ = server;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
        engine_ = callback;
    }

    private void onUpdate(Data.Transaction u) {
        Log.i(TAG, "tx update up "+u);

        WalletData.Transaction tx = dao_.getTransaction(u.txHash);
        Job job = null;
        if (tx == null) {
            // if we were interrupted while sending new tx,
            // or if we get notified before sender gets txHash,
            // we now have to figure out if this tx is the one we sent.
            // we have amount, time and addresses
            // - total amount should match, is it?
            // - we might be dest ourselves, can we? we don't know our spent utxo anyway
            // - so timestamp should be around lastTryTime
            // - amount should be same?
            // - dest addresses should be same
            List<Job> jobs = dao_.getSendingJobs();
            for (Job j: jobs) {
                WalletData.Transaction t = (WalletData.Transaction)j.objects.get(0);
                long amount = 0;
                if (t.addrToAmount() != null) {
                    for (long a : t.addrToAmount().values())
                        amount += a;
                }

                boolean addressesMatch = u.destAddresses.size() == t.addrToAmount().size();
                for (String a: u.destAddresses)
                    addressesMatch &= t.addrToAmount().containsKey(a);
                // FIXME check this!
                boolean timestampMatch = u.timeStamp >= (j.job.lastTryTime / 1000);
                // FIXME problem is a)amount will be negative, b) amount will include fees which are
                //  not known in advance!
                boolean amountMatch = t.sendAll() || amount == t.amount();

                Log.i(TAG, "maybe tx "+t+" addressesMatch "+addressesMatch+
                        " timestampMatch "+timestampMatch+" amountMatch "+amountMatch);
                if (addressesMatch && timestampMatch && amountMatch) {
                    tx = t;
                    job = j;
                    break;
                }
            }
        }

        WalletData.Transaction.Builder b;
        if (tx != null) {
            b = tx.toBuilder();
        } else {
            b = WalletData.Transaction.builder();
            b.setId(server_.getIdGenerator().generateId(WalletData.Transaction.class));
        }

        LightningCodec.TransactionConverter.decode(u, b);
        b.setState(WalletData.TRANSACTION_STATE_SENT);
        if (job != null)
            job.job.jobState = Transaction.JOB_STATE_DONE;

        dao_.updateTransaction(job, b.build());

        engine_.onSignal(id(), DefaultTopics.TRANSACTION_STATE, null);
    }

    @Override
    public void work() {
        if (!lnd_.isRpcReady())
            return;

        if (started_)
            return;

        started_ = true;

        Data.GetTransactionsRequest s = new Data.GetTransactionsRequest();
        lnd_.client().subscribeTransactionsStream(s, new ILightningCallback<Data.Transaction>() {

            @Override
            public void onResponse(Data.Transaction d) {
                Log.i(TAG, "subscribe transaction update");
                if (synched_)
                    onUpdate(d);
                else
                    buffer_.add(d);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "subscribe transactions error "+i+" err "+s);
                throw new RuntimeException("SubscribeTransactions failed");
            }
        });

//        List<Job> jobs = dao_.getSendingJobs();
//        if (jobs == null || jobs.isEmpty()) {
//            synched_ = true;
//            return;
//        }

        // initial sync protocol:
        // - subscribe to updates
        // - until synced_ == true, buffer any incoming updates
        // - load pending/lost txs
        // - if not empty - start sync sequence
        //  - call getTxs to get all
        //  - drop known txs by txHash
        //  - call onUpdate
        //  - set PENDING status to all lost txs that were not matched

        Data.GetTransactionsRequest s1 = new Data.GetTransactionsRequest();
        lnd_.client().getTransactions(s1, new ILightningCallback<Data.TransactionDetails>() {

            @Override
            public void onResponse(Data.TransactionDetails d) {
                Log.i(TAG, "get transactions reply "+d.transactions.size());

                // we might have missed some updates, so now we have
                // to update all known txs :(
                for(Data.Transaction t: d.transactions)
                    onUpdate(t);

                // mark all non-updated channels as 'retry'
                List<Job> jobs = dao_.getSendingJobs();
                for(Job job: jobs) {
                    job.job.jobState = Transaction.JOB_STATE_RETRY;
                    dao_.updateJob(job);
                }

                // now we're synched and ready to process new events
                synched_ = true;

                // process buffered new events
                for(Data.Transaction t: buffer_)
                    onUpdate(t);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "get transactions error "+i+" err "+s);
                throw new RuntimeException("GetTransactions failed");
            }
        });

    }

    @Override
    public void auth(WalletData.AuthRequest ar, WalletData.AuthResponse r) {
        throw new RuntimeException("Unexpected auth");
    }

    @Override
    public boolean isUserPrivileged(WalletData.User user, String requestType) {
        throw new RuntimeException("Unexpected priv check");
    }

    @Override
    public void getSubscriptions(List<String> topics) {
        // noop
    }

    @Override
    public void notify(String topic, Object data) {
        // noop
    }
}

