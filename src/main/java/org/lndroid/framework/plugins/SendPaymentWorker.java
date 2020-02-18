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
import org.lndroid.framework.dao.ISendPaymentWorkerDao;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.engine.IPluginBackground;
import org.lndroid.framework.engine.IPluginBackgroundCallback;
import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.lnd.ILightningDao;
import org.lndroid.framework.lnd.LightningCodec;

public class SendPaymentWorker implements IPluginBackground {

    private static final String TAG = "SendPaymentWorker";
    private static final long DEFAULT_EXPIRY = 3600000; // 1h
    private static final long TRY_INTERVAL = 60000; // 1m
    private static final long WORK_INTERVAL = 10000; // 10sec

    private IPluginServer server_;
    private IPluginBackgroundCallback engine_;
    private ISendPaymentWorkerDao dao_;
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
        dao_ = (ISendPaymentWorkerDao) server.getDaoProvider().getPluginDao(id());
        lnd_ = server.getDaoProvider().getLightningDao();
    }

    private void onUpdate(WalletData.SendPayment sp) {
        Log.i(TAG, "onUpdate payment "+sp);

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

            dao_.settlePayment(sp, htlc);

        } else {
            dao_.updatePayment(sp);
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

    private void checkPayments(final List<WalletData.SendPayment> list) {

        final Map<String, WalletData.SendPayment> map = new HashMap<>();
        for(WalletData.SendPayment p: list) {
            if (p.paymentHashHex() == null || p.paymentHashHex().equals("")) {
                p = p.toBuilder()
                        .setErrorCode(Errors.PLUGIN_INPUT)
                        .setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT))
                        .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                        .build();
                onUpdate(p);
            } else {
                map.put(p.paymentHashHex(), p);
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

                    WalletData.SendPayment mp = map.get(p.paymentHash);
                    if (mp == null)
                        continue;

                    WalletData.SendPayment.Builder b = mp.toBuilder();
                    LightningCodec.PaymentConverter.decode(p, b);
                    b.setState(WalletData.SEND_PAYMENT_STATE_OK);
                    onUpdate(b.build());

                    // drop from map
                    map.remove(p.paymentHash);
                }

                // payments that weren't found need to be retried
                for(WalletData.SendPayment p: map.values()) {
                    // reset state to pending to try again
                    p = p.toBuilder().setState(WalletData.SEND_PAYMENT_STATE_PENDING).build();
                    onUpdate(p);
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

    private void decodePayment(final WalletData.SendPayment p) {

        // FIXME later decode payment requests w/o asking the lnd as their format is open

        Data.PayReqString r = new Data.PayReqString();
        r.payReq = p.paymentRequest();
        lnd_.client().decodePayReq(r, new ILightningCallback<Data.PayReq>() {
            @Override
            public void onResponse(Data.PayReq r) {
                Log.i(TAG, "pay req response dest "+r);
                WalletData.SendPayment.Builder b = p.toBuilder();
                LightningCodec.PayReqConverter.decode(r, b);
                WalletData.SendPayment decodedPayment = b.build();
                onUpdate(decodedPayment);
                queryRoutes(decodedPayment);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "decode payreq error "+i+" err "+s);

                onUpdate(p.toBuilder()
                    .setErrorCode(Errors.LND_ERROR)
                    .setErrorMessage(s)
                    .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                    .build());
            }
        });
    }

    private void queryRoutes(WalletData.SendPayment p_) {

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
                    sendPayment(p, r);
                } else {
                    Log.e(TAG, "queryRoutes no route");

                    WalletData.SendPayment.Builder b = p.toBuilder();
                    b.setPaymentError("no route found");
                    onFailure(b);
                    onUpdate(b.build());
                }
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "queryRoutes error "+i+" err "+s);

                onUpdate(p.toBuilder()
                        .setErrorCode(Errors.LND_ERROR)
                        .setErrorMessage(s)
                        .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                        .build());
            }
        });
    }


    private void onFailure(WalletData.SendPayment.Builder b) {
        boolean permanent = false;
        boolean notReady = false;
        final boolean expired = System.currentTimeMillis() > b.maxTryTime();

        if (b.paymentError() != null) {
            permanent |= b.paymentError().contains("invoice is already paid");
            permanent |= b.paymentError().contains("invoice expired");
            permanent |= b.paymentError().contains("IncorrectOrUnknownPaymentDetails");
        }

        if (b.errorMessage() != null) {
            notReady |= b.errorMessage().contains("in the process of starting");
        }

        int tries = b.tries();
        if (!notReady)
            tries++;
        b.setTries(tries);

        Log.e(TAG, "payment attempt failed id "+b.id()+" tries "+b.tries()+
                " max "+b.maxTries()+" expired "+expired+" permanent "+permanent+
                " not ready "+notReady+" paymentError "+b.paymentError()+" error "+b.errorMessage());

        if (permanent || tries >= b.maxTries() || expired) {
            b.setState(WalletData.SEND_PAYMENT_STATE_FAILED);
        } else {
            b.setNextTryTime(System.currentTimeMillis() + TRY_INTERVAL);
            b.setState(WalletData.SEND_PAYMENT_STATE_PENDING);

            // reset to avoid re-using them at next attempt
            b.setErrorMessage(null);
            b.setErrorCode(null);
            b.setPaymentError(null);
        }
    }

    private void sendPayment(WalletData.SendPayment p_, Data.QueryRoutesResponse qr) {

        WalletData.SendPayment.Builder b = p_.toBuilder();

        // mark as sending
        b.setState(WalletData.SEND_PAYMENT_STATE_SENDING);
        b.setLastTryTime(System.currentTimeMillis());

        // ensure deadline exists and is not higher than invoice expiry
        final long now = b.lastTryTime() > 0 ? b.lastTryTime() : System.currentTimeMillis();
        if (b.invoiceExpiry() > 0 && b.invoiceTimestamp() > 0) {

            long tm = b.invoiceTimestamp();

            // crop future timestamps to current device time
            if ((tm*1000) > now)
                tm = now / 1000;

            final long invoiceExpiry = (tm + b.invoiceExpiry()) * 1000;

            if (b.maxTryTime() == 0 || b.maxTryTime() > tm)
                b.setMaxTryTime(invoiceExpiry);
        }

        // ensure some deadline anyhow
        if (b.maxTryTime() == 0) {
            b.setMaxTryTime(now + DEFAULT_EXPIRY);
        }

        // prepare sendtoroute request
        Data.SendToRouteRequest r = new Data.SendToRouteRequest();

        // take route from queryroutes response
        r.route = qr.routes.get(0);

        // convert, stop if error
        if (!LightningCodec.SendToRouteCodec.encode(b, r)) {
            Log.e(TAG, "send payment error bad request");
            onUpdate(b
                    .setErrorCode(Errors.PLUGIN_INPUT)
                    .setErrorMessage(Errors.errorMessage(Errors.PLUGIN_INPUT))
                    .setState(WalletData.SEND_PAYMENT_STATE_FAILED)
                    .build());
            return;
        }

        final WalletData.SendPayment up = b.build();

        // write sending state
        onUpdate(up);

        // send
        lnd_.client().sendToRoute(r, new ILightningCallback<Data.SendResponse>() {
            @Override
            public void onResponse(Data.SendResponse r) {
                WalletData.SendPayment.Builder b = up.toBuilder();
                LightningCodec.SendToRouteCodec.decode(r, b);
                if (r.paymentError != null && !r.paymentError.isEmpty()) {
                    Log.i(TAG, "send payment target error " + r);

                    // check if we'd want to retry
                    onFailure(b);
                } else {
                    Log.i(TAG, "send payment response " + r);
                    b.setState(WalletData.SEND_PAYMENT_STATE_OK);
                    b.setSendTime(System.currentTimeMillis());
                }
                onUpdate(b.build());
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "send payment error "+i+" err "+s);
                WalletData.SendPayment.Builder b = up.toBuilder();
                b.setErrorCode(Errors.LND_ERROR);
                b.setErrorMessage(s);

                // check if we need to retry
                onFailure(b);
                // write
                onUpdate(b.build());
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
                List<WalletData.SendPayment> sendingPayments = dao_.getSendingPayments();
                checkPayments(sendingPayments);
            }
            return;
        }

        if (!notified_ && nextWorkTime_ > System.currentTimeMillis())
            return;

        // reset
        notified_ = false;

        List<WalletData.SendPayment> pending = dao_.getPendingPayments(System.currentTimeMillis());
        for (WalletData.SendPayment p: pending) {
            if (p.paymentHashHex() != null || p.isKeysend()) {
                queryRoutes(p);
            } else if (p.paymentRequest() != null) {
                decodePayment(p);
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
