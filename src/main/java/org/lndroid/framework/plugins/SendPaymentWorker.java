package org.lndroid.framework.plugins;

import android.util.Log;

import org.lndroid.framework.defaults.DefaultTopics;
import org.lndroid.framework.engine.IPluginServer;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.data.Data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class SendPaymentWorker implements IPluginBackground {

    public interface IDao {
        List<Job> getSendingJobs();
        List<Job> getPendingJobs(long now);

        WalletData.Contact getContact(String contactPubkey);

        void updateJob(Job j);
        void updatePayment(Job j, WalletData.SendPayment p);
        void settlePayment(Job j, WalletData.SendPayment sp, WalletData.HTLCAttempt htlc);
    }

    private static final String TAG = "SendPaymentWorker";
    private static final long DEFAULT_EXPIRY = 3600000; // 1h
    private static final long TRY_INTERVAL = 60000; // 1m
    private static final long WORK_INTERVAL = 10000; // 10sec

    private IPluginServer server_;
    private IPluginBackgroundCallback engine_;
    private IDao dao_;
    private ILightningDao lnd_;
    private boolean starting_;
    private boolean started_;
    private boolean notified_;
    private long nextWorkTime_;

    @Override
    public String id() {
        return DefaultPlugins.SEND_PAYMENT_WORKER;
    }

    @Override
    public void init(IPluginServer server, IPluginBackgroundCallback callback) {
        server_ = server;
        engine_ = callback;
        dao_ = (IDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
    }

    private void onUpdate(Job job, WalletData.SendPayment sp) {
        Log.i(TAG, "onUpdate payment "+sp);

        if (sp.state() == WalletData.SEND_PAYMENT_STATE_FAILED) {
            job.job.jobState = Transaction.JOB_STATE_FAILED;
            job.job.jobErrorCode = sp.errorCode();
            job.job.jobErrorMessage = sp.errorMessage();
        }

        if (sp.state() == WalletData.SEND_PAYMENT_STATE_OK) {
            // NOTE: now we should write (at least one) HTLCAttempt.
            // Since lnd doesn't yet return them here,
            // we generate one for the whole payment.

            WalletData.HTLCAttempt htlc = WalletData.HTLCAttempt.builder()
                    .setId(server_.getIdGenerator().generateId(WalletData.HTLCAttempt.class))
                    .setSendPaymentId(sp.id())
                    .setAttemptTime(System.currentTimeMillis())
                    .setResolveTime(System.currentTimeMillis())
                    .setState(WalletData.HTLC_ATTEMPT_STATE_SUCCEEDED)
                    .setTotalAmountMsat(sp.totalValueMsat())
                    .setTotalFeeMsat(sp.feeMsat())
//                    .setTotalTimeLock(sp.finalCltvDelta()) // FIXME
                    .setDestCustomRecords(sp.destCustomRecords())
                    .build();

            dao_.settlePayment(job, sp, htlc);

        } else {
            dao_.updatePayment(job, sp);
        }

        PluginData.PluginNotification n = new PluginData.PluginNotification();
        n.pluginId = id();
        n.entityId = sp.id();
        engine_.onSignal(id(), DefaultTopics.SEND_PAYMENT_STATE, n);
    }

    private void deletePayments() {
        Data.DeleteAllPaymentsRequest r = new Data.DeleteAllPaymentsRequest();
        lnd_.client().deleteAllPayments(r, new ILightningCallback<Data.DeleteAllPaymentsResponse>() {
            @Override
            public void onResponse(Data.DeleteAllPaymentsResponse r) {
                Log.i(TAG, "delete payments response " + r);
                started_ = true;
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "delete payments error "+i+" err "+s);
                throw new RuntimeException(s);
            }
        });
    }

    private void checkPayments(List<Job> list) {

        final Map<String, Job> map = new HashMap<>();
        for(Job job: list) {
            WalletData.SendPayment p = (WalletData.SendPayment)job.objects.get(0);
            if (p.paymentHashHex() == null || p.paymentHashHex().equals("")) {
                p = p.toBuilder()
                        .setErrorCode(Errors.PLUGIN_INPUT)
                        .setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT))
                        .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                        .build();

                onUpdate(job, p);
            } else {
                map.put(p.paymentHashHex(), job);
            }
        }

        // all were errors?
        if (map.isEmpty()) {
            // no sending payments to recover?
            // delete old payments from lnd
            deletePayments();
            return;
        }

        Data.ListPaymentsRequest r = new Data.ListPaymentsRequest();
        lnd_.client().listPayments(r, new ILightningCallback<Data.ListPaymentsResponse>() {
            @Override
            public void onResponse(Data.ListPaymentsResponse r) {
                Log.i(TAG, "list payments response "+r);

                // scan all payments until
                for(Data.Payment p: r.payments) {
                    if (map.isEmpty())
                        break;

                    Job job = map.get(p.paymentHash);
                    if (job == null)
                        continue;

                    WalletData.SendPayment.Builder b = ((WalletData.SendPayment)job.objects.get(0)).toBuilder();
                    LightningCodec.PaymentConverter.decode(p, b);
                    b.setState(WalletData.SEND_PAYMENT_STATE_OK);

                    job.job.jobState = Transaction.JOB_STATE_DONE;
                    onUpdate(job, b.build());

                    // drop from map
                    map.remove(p.paymentHash);
                }

                // payments that weren't found need to be retried
                for(Job job: map.values()) {
                    // reset state to pending to try again
                    job.job.jobState = Transaction.JOB_STATE_NEW;
                    WalletData.SendPayment p = ((WalletData.SendPayment)job.objects.get(0))
                            .toBuilder()
                            .setState(WalletData.SEND_PAYMENT_STATE_PENDING)
                            .build();
                    onUpdate(job, p);
                }

                // now delete payments after we've recovered
                deletePayments();
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "list payments error "+i+" err "+s);
                throw new RuntimeException(s);
            }
        });
    }

    private void decodePayment(final Job job, final WalletData.SendPayment p) {

        Data.PayReqString r = new Data.PayReqString();
        r.payReq = p.paymentRequest();
        lnd_.client().decodePayReq(r, new ILightningCallback<Data.PayReq>() {
            @Override
            public void onResponse(Data.PayReq r) {
                Log.i(TAG, "pay req response dest "+r);
                WalletData.SendPayment.Builder b = p.toBuilder();
                LightningCodec.PayReqConverter.decode(r, b);
                WalletData.SendPayment decodedPayment = b.build();
                onUpdate(job, decodedPayment);
                queryRoutes(job, decodedPayment);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "decode payreq error "+i+" err "+s);

                onUpdate(job, p.toBuilder()
                    .setErrorCode(Errors.LND_ERROR)
                    .setErrorMessage(s)
                    .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                    .build());
            }
        });
    }

    private void queryRoutes(final Job job, WalletData.SendPayment p_) {

        WalletData.SendPayment.Builder b = p_.toBuilder();

        // new preimage on every send attempt
        if (b.isKeysend()) {

            SecureRandom random = new SecureRandom();
            byte[] preimage = new byte[32];
            random.nextBytes(preimage);

            b.setPaymentPreimageHex(LightningCodec.bytesToHex(preimage));

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(preimage);
                b.setPaymentHashHex(LightningCodec.bytesToHex(hash));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        if (b.contactPubkey() != null) {
            WalletData.Contact c = dao_.getContact(b.contactPubkey());
            if (c != null) {
                b.setFeatures(c.features());
                b.setRouteHints(c.routeHints());
            }
        }

        final WalletData.SendPayment p = b.build();

        Data.QueryRoutesRequest r = new Data.QueryRoutesRequest();
        LightningCodec.QueryRoutesConvertor.encode(p, r);

        lnd_.client().queryRoutes(r, new ILightningCallback<Data.QueryRoutesResponse>() {
            @Override
            public void onResponse(Data.QueryRoutesResponse r) {
                Log.i(TAG, "queryRoutes response routes "+r.routes.size());
                if (!r.routes.isEmpty()) {
                    sendPayment(job, p, r);
                } else {
                    Log.e(TAG, "queryRoutes no route");

                    WalletData.SendPayment.Builder b = p.toBuilder();
                    b.setPaymentError("no route found");
                    onFailure(job, b);
                    onUpdate(job, b.build());
                }
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "queryRoutes error "+i+" err "+s);

                onUpdate(job, p.toBuilder()
                        .setErrorCode(Errors.LND_ERROR)
                        .setErrorMessage(s)
                        .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                        .build());
            }
        });
    }


    private void onFailure(Job job, WalletData.SendPayment.Builder b) {
        boolean permanent = false;
        boolean notReady = false;
        final boolean expired = System.currentTimeMillis() > job.job.maxTryTime;

        if (b.paymentError() != null) {
            permanent |= b.paymentError().contains("invoice is already paid");
            permanent |= b.paymentError().contains("invoice expired");
            permanent |= b.paymentError().contains("IncorrectOrUnknownPaymentDetails");
        }

        if (b.errorMessage() != null) {
            notReady |= b.errorMessage().contains("in the process of starting");
        }

        if (!notReady)
            job.job.tries++;

        Log.e(TAG, "payment attempt failed id "+b.id()+" tries "+job.job.tries+
                " max "+job.job.maxTries+" expired "+expired+" permanent "+permanent+
                " not ready "+notReady+" paymentError "+b.paymentError()+" error "+b.errorMessage());

        if (permanent || job.job.tries >= job.job.maxTries || expired) {
            b.setState(WalletData.SEND_PAYMENT_STATE_FAILED);
        } else {
            job.job.nextTryTime = System.currentTimeMillis() + TRY_INTERVAL;
            job.job.jobState = Transaction.JOB_STATE_NEW;

            b.setState(WalletData.SEND_PAYMENT_STATE_PENDING);

            // reset to avoid re-using them at next attempt
            b.setErrorMessage(null);
            b.setErrorCode(null);
            b.setPaymentError(null);
        }
    }

    private void sendPayment(final Job job, final WalletData.SendPayment p, Data.QueryRoutesResponse qr) {

        // mark as sending
        job.job.jobState = Transaction.JOB_STATE_EXECUTING;
        job.job.lastTryTime = System.currentTimeMillis();
//        b.setState(WalletData.SEND_PAYMENT_STATE_SENDING);
//        b.setLastTryTime(System.currentTimeMillis());

        // ensure deadline exists and is not higher than invoice expiry
        final long now = job.job.lastTryTime > 0 ? job.job.lastTryTime : System.currentTimeMillis();
        if (p.invoiceExpiry() > 0 && p.invoiceTimestamp() > 0) {

            long tm = p.invoiceTimestamp();

            // crop future timestamps to current device time
            if ((tm*1000) > now)
                tm = now / 1000;

            final long invoiceExpiry = (tm + p.invoiceExpiry()) * 1000;

            if (job.job.maxTryTime == 0 || job.job.maxTryTime > tm)
                job.job.maxTryTime = invoiceExpiry;
        }

        // ensure some deadline anyhow
        if (job.job.maxTryTime == 0) {
            job.job.maxTryTime = now + DEFAULT_EXPIRY;
        }

        // prepare sendtoroute request
        Data.SendToRouteRequest r = new Data.SendToRouteRequest();

        // take route from queryroutes response
        r.route = qr.routes.get(0);

        // convert, stop if error
        if (!LightningCodec.SendToRouteCodec.encode(p, r)) {
            Log.e(TAG, "send payment error bad request");
            onUpdate(job, p
                    .toBuilder()
                    .setErrorCode(Errors.PLUGIN_INPUT)
                    .setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT))
                    .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                    .build());
            return;
        }

        // write sending state
        dao_.updateJob(job);

        // send
        lnd_.client().sendToRoute(r, new ILightningCallback<Data.SendResponse>() {
            @Override
            public void onResponse(Data.SendResponse r) {
                WalletData.SendPayment.Builder b = p.toBuilder();
                LightningCodec.SendToRouteCodec.decode(r, b);
                if (r.paymentError != null && !r.paymentError.isEmpty()) {
                    Log.i(TAG, "send payment target error " + r);

                    // check if we'd want to retry
                    onFailure(job, b);
                } else {
                    Log.i(TAG, "send payment response " + r);
                    job.job.jobState = Transaction.JOB_STATE_DONE;
                    b.setState(WalletData.SEND_PAYMENT_STATE_OK);
                    b.setSendTime(System.currentTimeMillis());
                }
                onUpdate(job, b.build());
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "send payment error "+i+" err "+s);
                WalletData.SendPayment.Builder b = p.toBuilder();
                b.setErrorCode(Errors.LND_ERROR);
                b.setErrorMessage(s);

                // check if we need to retry
                onFailure(job, b);
                // write
                onUpdate(job, b.build());
            }
        });
    }

    @Override
    public void work() {

        // lnd must be ready
        if (!lnd_.isRpcReady())
            return;

        if (!started_) {
            // NOTE: start protocol to recover in-flight payments' state:
            // - get all payments w/ 'sending' state from db
            // - get all payments from lnd
            // - find 'sending' payments within lnd payments
            // - update found payments, reset state for not-found
            // - delete all payments from lnd to clear it
            // - started_ = true, can process new payments now
            if (!starting_) {
                starting_ = true;
                List<Job> sendingJobs = dao_.getSendingJobs();
                checkPayments(sendingJobs);
            }
            return;
        }

        if (!notified_ && nextWorkTime_ > System.currentTimeMillis())
            return;

        // reset
        notified_ = false;

        List<Job> pending = dao_.getPendingJobs(System.currentTimeMillis());
        for (Job job: pending) {
            WalletData.SendPayment p = (WalletData.SendPayment)job.objects.get(0);
            if (p.paymentHashHex() != null || p.isKeysend()) {
                queryRoutes(job, p);
            } else if (p.paymentRequest() != null) {
                decodePayment(job, p);
            }
        }

        nextWorkTime_ = System.currentTimeMillis() + WORK_INTERVAL;
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
        topics.add(DefaultTopics.NEW_SEND_PAYMENT);
    }

    @Override
    public void notify(String topic, Object data) {
        notified_ = true;
    }
}
