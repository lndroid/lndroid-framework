package org.lndroid.framework;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class WalletDataDecl {

    // NOTE: not using IXXX names bcs these are not meant to be
    // used as interfaces, but are simply a way to declare AutoValue
    // structs w/o specifying all implementation details like builders etc
    // You need to use corresponding WalletData.XXX struct to instanciate
    // these.

    public interface EntityBase {
        // all entities have an id
        long id();
    }

    public interface Error {
        @Nullable
        String code();

        @Nullable
        String message();
    }

    public interface WalletState {
        int state();

        @Nullable
        String code();

        @Nullable
        String message();
    }

    public interface UserIdentity {

        // set for local clients who know their user id
        // and can generate proper token
        long userId();

        // signed token: tm+duration+plugins+signature
        @Nullable
        String sessionToken ();

        // set for apps over IPC
        @Nullable
        String appPackageName();

        @Nullable
        String appPubkey();
    }

    public interface User {
        long authUserId();

        long createTime();

        @Nullable
        String role();

        // for local users
        @Nullable
        String authType();

        // for some auth types nonce is required
        @Nullable
        String nonce();

        @Nullable
        String pubkey();

        // fields below are required for App roles only
        @Nullable
        String appPackageName();

        @Nullable
        String appPubkey();

        @Nullable
        String appLabel();
    }

    // NOTE: this struct is not stored in db or
    // send over IPC, so no need to worry about converting
    // 'data' field.
    public interface AuthResponse {
        long authId();

        long authUserId();

        boolean authorized();

        @Nullable
        Object data();
    }

    public interface AuthRequest {
        long userId();

        long createTime();

        boolean background();

        @Nullable
        String pluginId();

        // for foreground plugins only
        @Nullable
        String txId();

        // for background only, optional
        @Nullable
        String type();

        // className of the auth activity
        @Nullable
        String componentClassName();

        // packageName of the auth activity
        @Nullable
        String componentPackageName();
    }

    public interface AddUserRequest {
        // required fields
        @Nullable
        String role();

        @Nullable
        String authType();

        // provide if authType==password
        @Nullable
        String password();

        // fields below are required for App roles only
        @Nullable
        String appPubkey();

        @Nullable
        String appPackageName();

        @Nullable
        String appLabel(); // to present in UI
    }

    public interface NewAddressRequest {
        int type();
    }

    public interface NewAddress {
        @Nullable
        String address();
    }

    // base class to allow for generic plugin implementations
    public interface GetRequestTmpl<IdType> {
        @Nullable
        IdType id();

        boolean noAuth();

        boolean subscribe();
    }

    public interface Contact<RouteHint> {
        long userId();

        long authUserId();

        long createTime();

        @Nullable
        String pubkey();

        @Nullable
        String name();

        @Nullable
        String description();

        @Nullable
        String url();

        @Nullable
        ImmutableList<RouteHint> routeHints();

        @Nullable
        ImmutableList<Integer> features();
    }

    public interface AddContactRequest {
        @Nullable
        String pubkey();

        @Nullable
        String name();

        @Nullable
        String description();

        @Nullable
        String url();
    }

    public interface AddAppContactRequest {
        // empty for now
    }

    public interface ListPage {
        // required field
        // number of items to return
        int count();

        // ItemKeyed-cursor: returned items will
        // be 'count' Items going after 'afterId' item
        // in the sample formed by WHERE and ORDER clauses.
        long afterId();

        // same as afterId, but returns items that go
        // before this one in the sorted set
        long beforeId();

        // returns count before aroundId, then aroundId,
        // and then count items after aroundId, up to 2*count+1
        // in total, if aroundId not found - returns
        // count items from the start of the sample
        long aroundId();
    }

    // base ListRequest for a generic implementation
    public interface ListRequestBase {

        // required
        @Nullable
        ListPage page();

        // if provided, makes sure no 'auth' will be triggered
        // as caller is always allowed to read his own invoices.
        boolean onlyOwn();

        // don't trigger auth if reading is forbidden
        boolean noAuth();

        // if 'true', tx is long-lived:
        // - 'start' sends this object as request, and
        // returned is the first page of data, tx is not terminated
        // by the server
        // - 'send' might be called w/ ListPage as argument,
        // and server will return requested page of items
        // using the same WHERE/ORDER that was supplied in 'start'
        // - 'stop' can be called to teminate the tx
        // - TX_INVALIDATE or TX_TIMEOUT errors should result
        // in retry of the request (data changed or tx expired)
        // If 'false', results are returned and tx is terminated
        // by server.
        boolean enablePaging();
    }

    public interface ListInvoicesRequest {

        // might trigger auth if userId != callerUserId and !onlyOwn
        long userId();

        // all other filters might trigger auth if !onlyOwn
        long invoiceId();

        @Nullable
        String txId();

        @Nullable
        String preimageHex();

        @Nullable
        String preimageHashHex();

        long authUserId();

        long createFrom();

        long createTill();

        long settleFrom();

        long settleTill();

        @Nullable
        ImmutableList<Integer> states();

        @Nullable
        String description();

        @Nullable
        String purpose();

        // sort order: id, settleTime, valueSat
        @Nullable
        String sort();

        boolean sortDesc();
    }

    public interface ListPaymentsRequest {
        // might trigger auth if userId != callerUserId and !onlyOwn
        long userId();

        // all filters might trigger auth if !onlyOwn
        int type();

        long contactId();

        long sourceId();

        long timeFrom();

        long timeTill();

        boolean onlyMessages();

        // sort order: id, time
        @Nullable
        String sort();

        boolean sortDesc();
    }

    public interface ListContactsRequest {
        // might trigger auth if userId != callerUserId and !onlyOwn
        long userId();

        // sort order: id, name, createTime
        @Nullable
        String sort();

        boolean sortDesc();
    }

    public interface ListResultTmpl<Item> {
        @Nullable
        ImmutableList<Item> items();

        int count();

        int position();
    }

    public interface ListContactsPrivilege {
        // who is being granted
        long userId();

        // who authorized this grant
        long authUserId();

        // at what time
        long createTime();
    }

    public interface ContactPaymentsPrivilege {
        // who is being granted
        long userId();

        // who authorized this grant
        long authUserId();

        // at what time
        long createTime();

        // which contact's payments are granted
        long contactId();
    }

    public interface SubscribeRequest {
        boolean noAuth();

        boolean onlyOwn();
    }

    public interface WalletBalance {
        // no primary key as this is a single global object

        long totalBalance();

        /// The confirmed balance of a wallet(with >= 1 confirmations)
        long confirmedBalance();

        /// The unconfirmed balance of a wallet(with 0 confirmations)
        long unconfirmedBalance();
    }

    public interface ChannelBalance {
        long balance();

        long pendingOpenBalance();
    }

    public interface WalletInfo {
        /// The identity appPubkey of the current node.
        @Nullable
        String identityPubkey();

        /// If applicable, the alias of the current node, e.g. "bob"
        @Nullable
        String alias();

        /// Number of pending channels
        int numPendingChannels();

        /// Number of active channels
        int numActiveChannels();

        /// Number of peers
        int numPeers();

        /// The node's current view of the height of the best block
        int blockHeight();

        /// The node's current view of the hash of the best block
        @Nullable
        String blockHash();

        /// Whether the wallet's view is synced to the main chain
        boolean syncedToChain();

        /// The URIs of the current node.
        @Nullable
        ImmutableList<String> uris();

        /// Timestamp of the block best known to the wallet
        long bestHeaderTimestamp();

        /// The version of the LND software that the node is running.
        @Nullable
        String lndVersion();

        /// Number of inactive channels
        int numInactiveChannels();

        /// A list of active chains the node is connected to,
        //public List<String> chains;

        /// The color of the current node in hex code format
        @Nullable
        String color();

        // Whether we consider ourselves synced with the public channel graph.
        boolean syncedToGraph();
    }

    public interface AddInvoiceRequest {
        @Nullable
        String preimageHex();

        // if not provided, payer will choose the value
        long valueSat();

        @Nullable
        String description();

        @Nullable
        String descriptionHashHex();

        @Nullable
        String fallbackAddr();

        /// Payment request expiry time in seconds. Default is 3600 (1 hour).
        long expiry();

        // internal field for user, not included in the payment request
        @Nullable
        String purpose();
    }

    public interface Invoice {

        // lndroid fields
        @Nullable
        String txId(); // which tx created this invoice

        // user that created the invoice
        long userId();

        // who authed this invoice
        long authUserId();

        // internal description to be presented to the user,
        // not included in the paymentRequest
        @Nullable
        String purpose();

        // ================

        /**
         * An optional memo to attach along with the invoice. Used for record keeping
         * purposes for the invoice's creator, and will also be set in the description
         * field of the encoded payment request if the description_hash field is not
         * being used.
         */
        @Nullable
        String description();

        /**
         * The hex-encoded preimage (32 byte) which will allow settling an incoming
         * HTLC payable to this preimage
         */
        @Nullable
        String preimageHex();

        /// The hash of the preimage
        @Nullable
        String preimageHashHex();

        /// The value of this invoice in satoshis
        long valueSat();

        /// When this invoice was created in ms
        long createTime();

        /// When this invoice was settled, in ms
        long settleTime();

        /**
         * A bare-bones invoice for a payment within the Lightning Network.  With the
         * details of the invoice, the sender has all the request necessary to authorized a
         * payment to the recipient.
         */
        @Nullable
        String paymentRequest();

        /**
         * Hash (SHA-256) of a description of the payment. Used if the description of
         * payment (memo) is too long to naturally fit within the description field
         * of an encoded payment request.
         */
        @Nullable
        String descriptionHashHex();

        /// Payment request expiry time in seconds. Default is 3600 (1 hour).
        long expiry();

        /// Fallback on-chain address.
        @Nullable
        String fallbackAddr();

        /// Delta to use for the time-lock of the CLTV extended to the final hop.
        long cltvExpiry();

        /**
         * Route hints that can each be individually used to assist in reaching the
         * invoice's destination.
         */
        // not stored or exposed atm
        // public List<RouteHint> routeHints;

        /// Whether this invoice should include routing hints for private channels.
        boolean isPrivate();

        /**
         * The "add" index of this invoice. Each newly created invoice will increment
         * this index making it monotonically increasing. Callers to the
         * SubscribeInvoices call can use this to instantly get notified of all added
         * invoices with an add_index greater than this one.
         */
        long addIndex();

        /**
         * The "settle" index of this invoice. Each newly settled invoice will
         * increment this index making it monotonically increasing. Callers to the
         * SubscribeInvoices call can use this to instantly get notified of all
         * settled invoices with an settle_index greater than this one.
         */
        long settleIndex();

        /**
         * The amount that was accepted for this invoice, in millisatoshis. This will
         * ONLY be set if this invoice has been settled. We provide this field as if
         * the invoice was created with a zero value, then we need to record what
         * amount was ultimately accepted. Additionally, it's possible that the sender
         * paid MORE that was specified in the original invoice. So we'll record that
         * here as well.
         */
        long amountPaidMsat();

        /**
         * The state the invoice is in.
         */
        int state();

        /// Number of HTLCs paying to this invoice [EXPERIMENTAL].
        int htlcsCount();

        boolean isKeysend();

        // list of feature codes
        @Nullable
        ImmutableList<Integer> features();
    }

    public interface InvoiceHTLC {

        // link to the invoice
        long invoiceId();

        /// Short channel id over which the htlc was received.
        long chanId();

        /// Index identifying the htlc on the channel.
        long htlcIndex();

        /// The amount of the htlc in msat.
        long amountMsat();

        /// Block height at which this htlc was accepted.
        int acceptHeight();

        /// Time at which this htlc was accepted.
        long acceptTime();

        /// Time at which this htlc was settled or canceled.
        long resolveTime();

        /// Block height at which this htlc expires.
        int expiryHeight();

        /// Current state the htlc is in.
        int state();

        // filled if present in custom records
        @Nullable
        String message();

        // filled if present in custom records
        @Nullable
        String senderPubkey();

        // filled if present in custom records, ms
        long senderTime();

        @Nullable
        ImmutableMap<Long,byte[]> customRecords();
    }

    public interface HTLCAttempt {

        // link to sendpayment
        long sendPaymentId();

        // in-flight, ok, failed
        int state();

        long attemptTime(); // ms
        long resolveTime(); // ms

        long totalAmountMsat();
        long totalFeeMsat();

        int totalTimeLock();

        @Nullable
        ImmutableMap<Long,byte[]> destCustomRecords();

        // FIXME later add Route here?
    }

    public interface Channel {

        // userId who called openChannel, might be 0 if
        // created by auto-pilot
        long userId();

        // if created by user
        @Nullable
        String txId();

        // user id who authed creation of this channel
        long authUserId();

        // user-provided description
        @Nullable
        String description();

        // NOTE: copied from OpenChannelRequest
        /// The target number of blocks that the funding transaction should be confirmed by.
        int targetConf();

        /// A manual fee rate set in sat/byte that should be used when crafting the funding transaction.
        long satPerByte();

        /// The minimum value in millisatoshi we will require for incoming HTLCs on the channel.
        long minHtlcMsat();

        /// The minimum number of confirmations each one of your outputs used for the funding transaction must satisfy.
        int minConfs();

        /// Whether unconfirmed outputs should be used as inputs for the funding transaction.
        boolean spendUnconfirmed();
        // ============================

        // NOTE: copied from ChannelCloseSummary
        /// The hash of the genesis block that this channel resides within.
        @Nullable
        String chainHashHex();

        /// The txid of the transaction which ultimately closed this channel.
        @Nullable
        String closingTxHashHex();

        /// Height at which the funding transaction was spent.
        int closeHeight();

        /// Settled balance at the time of channel closure
        long settledBalance();

        /// The sum of all the time-locked outputs at the time of channel closure
        long timeLockedBalance();

        /// Details on how the channel was closed.
        int closeType();
        // =======================

        // lndroid fields
        // current number of tries
        int tries();

        // max number of tries
        int maxTries();

        // deadline for retries, in ms
        long maxTryTime();

        // last time we retried, in ms
        long lastTryTime();

        // next time we'll retry, in ms
        // FIXME what if user changes the device time?
        long nextTryTime();

        // see above
        int state();

        // error code if state=failed
        @Nullable
        String errorCode();

        // error message by lndroid
        @Nullable
        String errorMessage();

        long createTime();

        long openTime();

        long closeTime();

        // ====================================


        /// Whether this channel is active or not
        boolean active();

        /// The identity appPubkey of the remote node
        @Nullable
        String remotePubkey();

        /**
         * The outpoint (txid:index) of the funding transaction. With this value, Bob
         * will be able to generate a signature for Alice's version of the commitment
         * transaction.
         */
        @Nullable
        String channelPoint();

        /**
         * The unique channel ID for the channel. The first 3 bytes are the block
         * height, the next 3 the index within the block, and the last 2 bytes are the
         * output index for the channel.
         */
        long chanId();

        /// The total amount of funds held in this channel
        long capacity();

        /// This node's current balance in this channel
        long localBalance();

        /// The counterparty's current balance in this channel
        long remoteBalance();

        /**
         * The amount calculated to be paid in fees for the current set of commitment
         * transactions. The fee amount is persisted with the channel in order to
         * allow the fee amount to be removed and recalculated with each channel state
         * update, including updates that happen after a system restart.
         */
        long commitFee();

        /// The weight of the commitment transaction
        long commitWeight();

        /**
         * The required number of satoshis per kilo-weight that the requester will pay
         * at all times, for both the funding transaction and commitment transaction.
         * This value can later be updated once the channel is open.
         */
        long feePerKw();

        /// The unsettled balance in this channel
        long unsettledBalance();

        /**
         * The total number of satoshis we've sent within this channel.
         */
        long totalSatoshisSent();

        /**
         * The total number of satoshis we've received within this channel.
         */
        long totalSatoshisReceived();

        /**
         * The total number of updates conducted within this channel.
         */
        long numUpdates();

        /**
         The list of active, uncleared HTLCs currently pending within the channel.
         */
        // public List<HTLC> pendingHtlcs;

        /**
         * The CSV delay expressed in relative blocks. If the channel is force closed,
         * we will need to wait for this many blocks before we can regain our funds.
         */
        int csvDelay();

        /// Whether this channel is advertised to the network or not.
        boolean isPrivate();

        /// True if we were the ones that created the channel.
        boolean initiator();

        /// A set of flags showing the current state of the channel.
        @Nullable
        String chanStatusFlags();

        /// The minimum satoshis this node is required to reserve in its balance.
        long localChanReserveSat();

        /**
         * The minimum satoshis the other node is required to reserve in its balance.
         */
        long remoteChanReserveSat();

        /**
         * If true, then this channel uses the modern commitment format where the key
         * in the output of the remote party does not change each state. This makes
         * back up and recovery easier as when the channel is closed, the funds go
         * directly to that key.
         */
        boolean staticRemoteKey();

        /**
         * The number of seconds that the channel has been monitored by the channel
         * scoring system. Scores are currently not persisted, so this value may be
         * less than the lifetime of the channel [EXPERIMENTAL].
         */
        long lifetime();

        /**
         * The number of seconds that the remote peer has been observed as being online
         * by the channel scoring system over the lifetime of the channel [EXPERIMENTAL].
         */
        long uptime();
    }

    public interface OpenChannelRequest {

        // user-supplied internal description
        @Nullable
        String description();

        /// The appPubkey of the node to open a channel with
        @Nullable
        String nodePubkey();

        /// The number of satoshis the wallet should commit to the channel
        long localFundingAmount();

        /// The number of satoshis to push to the remote side as part of the initial commitment state
        long pushSat();

        /// The target number of blocks that the funding transaction should be confirmed by.
        int targetConf();

        /// A manual fee rate set in sat/byte that should be used when crafting the funding transaction.
        long satPerByte();

        /// Whether this channel should be private, not announced to the greater network.
        boolean isPrivate();

        /// The minimum value in millisatoshi we will require for incoming HTLCs on the channel.
        long minHtlcMsat();

        /// The delay we require on the remote's commitment transaction. If this is not set, it will be scaled automatically with the channel size.
        int remoteCsvDelay();

        /// The minimum number of confirmations each one of your outputs used for the funding transaction must satisfy.
        int minConfs();

        /// Whether unconfirmed outputs should be used as inputs for the funding transaction.
        boolean spendUnconfirmed();
    }

    public interface SendPaymentRequest<RouteHint> {

        // internal description to be presented to the user
        @Nullable
        String purpose();

        // usually come with payreq
        @Nullable
        String invoiceDescription();

        @Nullable
        String invoiceDescriptionHashHex();

        @Nullable
        String invoiceFallbackAddr();

        long invoiceTimestamp();

        // deadline in ms, no retries after this time,
        // by default will use current time + expiry*1000 from paymentRequest
        long expiry();

        // max number of payment attempts
        int maxTries();

        /// The hex-encoded identity pubkey of the payment recipient
        @Nullable
        String destPubkey(); // destString

        /// Number of satoshis to send.
        long valueSat(); // amt

        /// The hex-encoded hash to use within the payment's HTLC
        @Nullable
        String paymentHashHex(); // paymentHashString

        /**
         * A bare-bones invoice for a payment within the Lightning Network.  With the
         * details of the invoice, the sender has all the data necessary to send a
         * payment to the recipient.
         */
        @Nullable
        String paymentRequest();

        /**
         * The CLTV delta from the current height that should be used to set the
         * timelock for the final hop.
         */
        int finalCltvDelta();

        /**
         * The maximum number of satoshis that will be paid as a fee of the payment.
         * This value can be represented either as a percentage of the amount being
         * sent, or as a fixed amount of the maximum fee the user is willing the pay to
         * send the payment.
         */
        /// The fee limit expressed as a fixed amount of satoshis.
        long feeLimitFixedMsat();

        /// The fee limit expressed as a percentage of the payment amount.
        long feeLimitPercent();

        /**
         * The channel id of the channel that must be taken to the first hop. If zero,
         * any channel may be used.
         */
        long outgoingChanId();

        /**
         * An optional maximum total time lock for the route. This should not exceed
         * lnd's `--max-cltv-expiry` setting. If zero, then the value of
         * `--max-cltv-expiry` is enforced.
         */
        int cltvLimit();

        @Nullable
        ImmutableList<RouteHint> routeHints();

        // feature bits assumed to be supportd by destination
        @Nullable
        ImmutableList<Integer> features();

        /**
         * An optional field that can be used to pass an arbitrary set of TLV records
         * to a peer which understands the new records. This can be used to pass
         * application specific data during the payment attempt.
         */
//        @Nullable
//        ImmutableMap<Long, byte[]> destTlv();

        long contactId();

        // send message as custom field
        @Nullable
        String message();

        // send our pubkey as custom field
        boolean includeSenderPubkey();

        boolean isKeysend();

        // don't trigger auth, useful if privileges are expected
        boolean noAuth();
    }

    // Stored Payment is roughtly the last Hop of an HTLC.
    // For incoming payments it's mapped to InvoiceHTLC,
    // For outgoing payments it's last Hop in the Route BUT w/ full amount/fee values.
    // Each invoiceHTLC produces one Payment object.
    // Each SendPayment produces one 'Payment' per HTLC and several Hop objects
    // per HTLC.

    // When SendPayment job is scheduled, a single Payment is created, when
    // SendPayment returns and produces a Route, we update 1 payment w/ full
    // amount, when full support of MPP comes and SendPayment start returning the
    // HTLCs then we will update our pre-made Payment and produce one for every HTLC.

    // Later we might start producing grouped Payment objects from raw stored Payment=HTLC
    // db records. We might even start storing them...

    public interface Payment<SendPayment, HTLCAttempt, Invoice, InvoiceHTLC> {

        int type();

        // invoice/sendpayment id,
        // several Payments might reference same sourceId
        long sourceId();

        // 1-1 link to source InvoiceHTLC/HTLCAttempt,
        // filled when HTLC is actually created
        long sourceHTLCId();

        // Lists of source objects that were grouped
        // by this Payment object.
        // These are transient fields to be populated
        // when reading from db but not stored
        // in the Payment entity
        @Nullable
        ImmutableMap<Long,SendPayment> sendPayments();
        @Nullable
        ImmutableMap<Long,HTLCAttempt> HTLCAttempts();
        @Nullable
        ImmutableMap<Long,Invoice> invoices();
        @Nullable
        ImmutableMap<Long,InvoiceHTLC> invoiceHTLCs();


        // Following fields are for filtering-sorting only!

        // user that created the payment
        long userId();

        // last update time (sent/received?)
        // or maybe create time is better? see later.
        // for sendpayment it's createtime,
        // for incoming payment:
        // - for message-payment it's sendtime from TLV
        // - for other payments it's receive time
        long time();

        // peer pubkey, only filled for raw Payment:
        // for out - dest pubkey,
        // for in - if known from TLV records
        @Nullable
        String peerPubkey();

        // message from TLV records, if any,
        // only filled for raw Payment
        @Nullable
        String message();
    }

    public interface SendPayment<RouteHint> {
        // lndroid fields

        // which tx created this payment
        @Nullable
        String txId();

        // user that created the payment
        long userId();

        // who authed this payment
        long authUserId();

        // internal description to be presented to the user
        @Nullable
        String purpose();

        // current number of tries
        int tries();

        // max number of tries
        int maxTries();

        // deadline for retries, in ms
        long maxTryTime();

        // last time we retried, in ms
        long lastTryTime();

        // next time we'll retry, in ms
        long nextTryTime();

        // 0 - pending, 1 - sent, 2 - failed, 3 - sending, 4 - rejected by user
        int state();

        // error code if state=failed
        @Nullable
        String errorCode();

        // error message by lndroid
        @Nullable
        String errorMessage();

        // usually come from payreq
        @Nullable
        String invoiceDescription();

        @Nullable
        String invoiceDescriptionHashHex();

        @Nullable
        String invoiceFallbackAddr();

        // MPP payment addr from the invoice
        @Nullable
        String paymentAddrHex();

        long invoiceTimestamp();

        // expiry from payreq in relative sec, if any
        long invoiceExpiry();

        // lnd fields

        /// The identity pubkey of the payment recipient
        @Nullable
        String destPubkey();

        /// The value of the payment in milli-satoshis
        long valueMsat();

        // valueMsat + feeMsat
        long totalValueMsat();

        /// The hash to use within the payment's HTLC
        @Nullable
        String paymentHashHex();

        /**
         * A bare-bones invoice for a payment within the Lightning Network.  With the
         * details of the invoice, the sender has all the request necessary to authorized a
         * payment to the recipient.
         */
        @Nullable
        String paymentRequest();

        /**
         * The CLTV delta from the current height that should be used to set the
         * timelock for the final hop.
         */
        int finalCltvDelta();

        /**
         * The maximum number of satoshis that will be paid as a fee of the payment.
         * This value can be represented either as a percentage of the amount being
         * sent, or as a fixed amount of the maximum fee the user is willing the pay to
         * authorized the payment.
         */
        /// The fee limit expressed as a fixed amount of satoshis.
        long feeLimitFixedMsat();

        /// The fee limit expressed as a percentage of the payment amount.
        long feeLimitPercent();

        /**
         * The channel id of the channel that must be taken to the first hop. If zero,
         * any channel may be used.
         */
        long outgoingChanId();

        /**
         * An optional maximum total time lock for the route. This should not exceed
         * lnd's `--max-cltv-expiry` setting. If zero, then the value of
         * `--max-cltv-expiry` is enforced.
         */
        int cltvLimit();

        // SendResponse fields

        /**
         * An optional field that can be used to pass an arbitrary set of TLV records
         * to a peer which understands the new records. This can be used to pass
         * application specific request during the payment attempt.
         */
        @Nullable
        ImmutableMap<Long, byte[]> destCustomRecords();

        @Nullable
        String paymentError();

        @Nullable
        String paymentPreimageHex();

        @Nullable
        ImmutableList<RouteHint> routeHints();

        // feature bits assumed to be supportd by destination
        @Nullable
        ImmutableList<Integer> features();

        // public Route paymentRoute;

        // Payment fields

        /// The time of this payment in ms
        long createTime();

        long sendTime();

        /// The path this payment took
        // instead, store payment routes w/ hops etc
        // public List<String> path;

        ///  The fee paid for this payment in milli-satoshis
        long feeMsat();

        @Nullable
        String contactPubkey();

        @Nullable
        String message();

        @Nullable
        String senderPubkey();

        // send preimage as TLV to make invoiceless payment
        boolean isKeysend();
    }

    public interface ConnectPeerRequest {
        @Nullable
        String pubkey();

        @Nullable
        String host();

        boolean perm();
    }

    public interface AddContactInvoiceResponse {
        @Nullable
        String paymentRequest();
    }

    public interface RouteHint<HopHint> {

        // contact:id or sendpaymentrequest:id etc
        @Nullable
        String parentId();

        /**
         * A list of hop hints that when chained together can assist in reaching a
         * specific destination.
         */
        @Nullable
        ImmutableList<HopHint> hopHints();
    }

    public interface HopHint {

        long routeHintId();

        // index in the route, last index hop must point to the destination
        int index();

        /// The public key of the node at the start of the channel.
        @Nullable
        String nodeId();

        /// The unique identifier of the channel.
        long chanId();

        /// The base fee of the channel denominated in millisatoshis.
        int feeBaseMsat();

        /**
         * The fee rate of the channel for sending one satoshi across it denominated in
         * millionths of a satoshi.
         */
        int feeProportionalMillionths();

        /// The time-lock delta of the channel.
        int cltvExpiryDelta();
    }

    public interface RoutingPolicy {
        long channelId();

        boolean reverse();

        int timeLockDelta();

        long minHtlc();

        long feeBaseMsat();

        long feeRateMilliMsat();

        boolean disabled();

        long maxHtlcMsat();

        int lastUpdate();
    }

    /**
     A fully authenticated channel along with all its unique attributes.
     Once an authenticated channel announcement has been processed on the network,
     then an instance of ChannelEdgeInfo encapsulating the channels attributes is
     stored. The other portions relevant to routing policy of a channel are stored
     within a ChannelEdgePolicy for each direction of the channel.
     */
    public interface ChannelEdge<RoutingPolicy> {

        /**
         * The unique channel ID for the channel. The first 3 bytes are the block
         * height, the next 3 the index within the block, and the last 2 bytes are the
         * output index for the channel.
         */
        long channelId();

        @Nullable
        String chanPoint();

        @Nullable
        String node1Pubkey();

        @Nullable
        String node2Pubkey();

        long capacity();

        @Nullable
        RoutingPolicy node1Policy();

        @Nullable
        RoutingPolicy node2Policy();
    }

    public interface LightningNode {
        int lastUpdate();

        @Nullable
        String pubkey();

        @Nullable
        String alias();

        // add later
        // repeated NodeAddress addresses =4[json_name ="addresses"];

        @Nullable
        String color();

        @Nullable
        ImmutableList<Integer> features();
    }

}
