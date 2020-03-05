package org.lndroid.framework;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.lndroid.framework.common.FieldInfo;
import org.lndroid.framework.defaults.DefaultFieldMapper;

public class WalletDataDecl {

    // NOTE: not using IXXX names bcs these are not meant to be
    // used as interfaces, but are simply a way to declare AutoValue
    // structs w/o specifying all implementation details like builders etc
    // You need to use corresponding WalletData.XXX struct to instanciate
    // these.

    public interface EntityBase {
        @FieldInfo(
                name = "ID",
                help = "Internal identifier"
        )
        // all entities have an id
        long id();
    }

    public interface Field {
        @Nullable
        String id();
        @Nullable
        String name();
        @Nullable
        String value();
        @Nullable
        String help();
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

        // set for apps over IPC
        @Nullable
        String appPackageName();

        @Nullable
        String appPubkey();
    }

    public interface User {
        @FieldInfo(
                name = "Authorizing user id",
                help = "User id that authorized this user/application"
        )
        long authUserId();

        @FieldInfo(
                name = "Create time",
                help = "Time when this user/application was added",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long createTime();

        @FieldInfo(
                name = "Role",
                help = "Role of this user record"
        )
        @Nullable
        String role();

        // for local users
        @Nullable
        String authType();

        @Nullable
        @FieldInfo(
                name = "Local public key",
                help = "Public key of this user's local key pair, used to authenticate user requests"
        )
        String pubkey();

        // fields below are required for App roles only
        @Nullable
        @FieldInfo(
                name = "Application package name",
                help = "Application package name, only applicable for users with 'app' role"
        )
        String appPackageName();

        @Nullable
        @FieldInfo(
                name = "Application public key",
                help = "Remote public key of application's key pair, used to authenticate application requests"
        )
        String appPubkey();

        @Nullable
        @FieldInfo(
                name = "Application name",
                help = "Name of the application as provided by Android package manager service"
        )
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
        @FieldInfo(
                name = "User id",
                help = "User that create the contact"
        )
        long userId();

        @FieldInfo(
                name = "Auth user id",
                help = "User that authorized the contact"
        )
        long authUserId();

        @FieldInfo(
                name = "Create time",
                help = "Time when contact was created",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long createTime();

        @FieldInfo(
                name = "Public key",
                help = "Contact identity public key"
        )
        @Nullable
        String pubkey();

        @FieldInfo(
                name = "Name",
                help = "User-assigned name of the contact"
        )
        @Nullable
        String name();

        @FieldInfo(
                name = "Description",
                help = "User-assigned description"
        )
        @Nullable
        String description();

        @FieldInfo(
                name = "URL",
                help = "User-assigned URL address"
        )
        @Nullable
        String url();

        // FIXME add FieldInfo w/ convertor
        @Nullable
        ImmutableList<RouteHint> routeHints();

        @FieldInfo(
                name = "Features",
                help = "Contact node features",
                convertors = {DefaultFieldMapper.ImmutableIntListConverter.class}
        )
        @Nullable
        ImmutableList<Integer> features();
    }

    public interface AddContactRequest<RouteHint> {
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

    public interface AddAppContactRequest {
        @Nullable
        String name();

        @Nullable
        String description();

        @Nullable
        String url();
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

        long notifyFrom();

        long notifyTill();

        boolean noKeysend();

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
        @FieldInfo(
                name = "Public key",
                help = "The identity pubkey of the current node."
        )
        @Nullable
        String identityPubkey();

        @FieldInfo(
                name = "Alias",
                help = "If applicable, the alias of the current node, e.g. \"bob\""
        )
        @Nullable
        String alias();

        @FieldInfo(
                name = "Pending channels",
                help = "Number of pending channels"
        )
        int numPendingChannels();

        @FieldInfo(
                name = "Active channels",
                help = "Number of active channels"
        )
        int numActiveChannels();

        @FieldInfo(
                name = "Peers",
                help = "Number of peers"
        )
        int numPeers();

        @FieldInfo(
                name = "Block height",
                help = "The node's current view of the height of the best block"
        )
        int blockHeight();

        @FieldInfo(
                name = "Block hash",
                help = "The node's current view of the hash of the best block"
        )
        @Nullable
        String blockHash();

        @FieldInfo(
                name = "Synched to blockchain",
                help = "Whether the wallet's view is synced to the main chain"
        )
        boolean syncedToChain();

        @FieldInfo(
                name = "URIs",
                help = "The URIs of the current node."
        )
        @Nullable
        ImmutableList<String> uris();

        @FieldInfo(
                name = "Best header time",
                help = "Timestamp of the block best known to the wallet"
        )
        long bestHeaderTimestamp();

        @FieldInfo(
                name = "LND version",
                help = "The version of the LND software that the node is running."
        )
        @Nullable
        String lndVersion();

        @FieldInfo(
                name = "Inactive channels",
                help = "Number of inactive channels"
        )
        int numInactiveChannels();

        /// A list of active chains the node is connected to,
        //public List<String> chains;

        /// The color of the current node in hex code format
        @FieldInfo(
                name = "Color",
                help = "Color of the node"
        )
        @Nullable
        String color();

        @FieldInfo(
                name = "Synched to graph",
                help = "Whether we consider ourselves synced with the public channel graph."
        )
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

        @FieldInfo(
                name = "User id",
                help = "User that created the invoice"
        )
        long userId();

        @FieldInfo(
                name = "Auth user id",
                help = "User that authorized the invoice"
        )
        long authUserId();

        @FieldInfo(
                name = "Purpose",
                help = "User-assigned internal description of the invoice, not included into the payment request"
        )
        @Nullable
        String purpose();

        // ================

        @FieldInfo(
                name = "Description",
                help = "An optional memo to attach along with the invoice. Used for record keeping "+
                        "purposes for the invoice's creator, and will also be set in the description "+
                        "field of the encoded payment request if the description_hash field is not" +
                        "being used."
        )
        @Nullable
        String description();

        @FieldInfo(
                name = "Pre-image",
                help = "The hex-encoded preimage (32 byte) which will allow settling an incoming HTLC "+
                        "payable to this preimage"
        )
        @Nullable
        String preimageHex();

        @FieldInfo(
                name = "Pre-image hash",
                help = "The hash of the preimage"
        )
        @Nullable
        String preimageHashHex();

        @FieldInfo(
                name = "Amount, sats",
                help = "The value of this invoice in satoshis"
        )
        long valueSat();

        @FieldInfo(
                name = "Create time",
                help = "When this invoice was created",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long createTime();

        @FieldInfo(
                name = "Settle time",
                help = "When this invoice was settled",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long settleTime();

        // when user was notified about this paid invoice, notifiedPaidInvoice method
        // must be called to set this field
        long notifyTime();

        @FieldInfo(
                name = "Payment request",
                help = "A bare-bones invoice for a payment within the Lightning Network.  With the " +
                        "details of the invoice, the sender has all the request necessary to authorized a " +
                        "payment to the recipient."
        )
        @Nullable
        String paymentRequest();

        @FieldInfo(
                name = "Description hash",
                help = "Hash (SHA-256) of a description of the payment. Used if the description of " +
                        "payment (memo) is too long to naturally fit within the description field " +
                        "of an encoded payment request."
        )
        @Nullable
        String descriptionHashHex();

        @FieldInfo(
                name = "Expiry",
                help = "Time when the invoice expires."
                // FIXME sec to datetime
        )
        long expiry();

        @FieldInfo(
                name = "Fallback address",
                help = "Fallback on-chain address."
        )
        @Nullable
        String fallbackAddr();

        @FieldInfo(
                name = "CLTV expiry",
                help = "Delta to use for the time-lock of the CLTV extended to the final hop."
        )
        long cltvExpiry();

        /**
         * Route hints that can each be individually used to assist in reaching the
         * invoice's destination.
         */
        // not stored or exposed atm
        // public List<RouteHint> routeHints;

        @FieldInfo(
                name = "Private",
                help = "Whether this invoice should include routing hints for private channels."
        )
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

        @FieldInfo(
                name = "Amount paid, msat",
                help = "The amount that was accepted for this invoice, in millisatoshis. This will " +
                        "ONLY be set if this invoice has been settled. We provide this field as if " +
                        "the invoice was created with a zero value, then we need to record what " +
                        "amount was ultimately accepted. Additionally, it's possible that the sender " +
                        "paid MORE that was specified in the original invoice. So we'll record that " +
                        "here as well."
        )
        long amountPaidMsat();

        @FieldInfo(
                name = "State",
                help = "The state the invoice is in."
                // FIXME add convertor
        )
        int state();

        @FieldInfo(
                name = "HTLC count",
                help = "Number of HTLCs paying to this invoice."
        )
        int htlcsCount();

        @FieldInfo(
                name = "Keysend",
                help = "Whether this invoice was generated as a result of incoming key-send payment"
        )
        boolean isKeysend();

        // list of feature codes
        @Nullable
        ImmutableList<Integer> features();

        @FieldInfo(
                name = "Message",
                help = "Message attached to the key-send payment."
        )
        @Nullable
        String message();

        @FieldInfo(
                name = "Sender public key",
                help = "Public key of the sender, if provided."
        )
        @Nullable
        String senderPubkey();
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

        @FieldInfo(
                name = "User id",
                help = "User that opened this channel, might be 0 if " +
                        "created by auto-pilot or is inbound"
        )
        long userId();

        // if created by user
        @Nullable
        String txId();

        @FieldInfo(
                name = "User id",
                help = "User that authorized the opening of this channel, might be 0 if " +
                        "created by auto-pilot or is inbound"
        )
        long authUserId();

        @FieldInfo(
                name = "Description",
                help = "User-assigned interal description of this channel"
        )
        @Nullable
        String description();

        @FieldInfo(
                name = "Funding target confs",
                help = "The target number of blocks that the funding transaction should be confirmed by."
        )
        int targetConf();

        @FieldInfo(
                name = "Funding sats per byte",
                help = "A manual fee rate set in sat/byte that should be used when crafting the funding transaction."
        )
        long satPerByte();

        @FieldInfo(
                name = "Min HTLC msat",
                help = "The minimum value in millisatoshi we will require for incoming HTLCs on the channel."
        )
        long minHtlcMsat();

        @FieldInfo(
                name = "Min confs",
                help = "The minimum number of confirmations each one of your outputs used for the funding transaction must satisfy."
        )
        int minConfs();

        @FieldInfo(
                name = "Spend unconfirmed",
                help = "Whether unconfirmed outputs should be used as inputs for the funding transaction."
        )
        boolean spendUnconfirmed();
        // ============================

        // NOTE: copied from ChannelCloseSummary
        /// The hash of the genesis block that this channel resides within.
        @Nullable
        String chainHashHex();

        @FieldInfo(
                name = "Closing tx hash",
                help = "The txid of the transaction which ultimately closed this channel."
        )
        @Nullable
        String closingTxHashHex();

        @FieldInfo(
                name = "Closing block height",
                help = "Height at which the funding transaction was spent."
        )
        int closeHeight();

        @FieldInfo(
                name = "Settled balance",
                help = "Settled balance at the time of channel closure."
        )
        long settledBalance();

        @FieldInfo(
                name = "Time-locked balance",
                help = "The sum of all the time-locked outputs at the time of channel closure."
        )
        long timeLockedBalance();

        @FieldInfo(
                name = "Close type",
                help = "Details on how the channel was closed."
                // FIXME add convertor
        )
        int closeType();
        // =======================

        // see above
        @FieldInfo(
                name = "State",
                help = "State of the channel"
                // FIXME add convertor
        )
        int state();

        @FieldInfo(
                name = "Error code",
                help = "Error code if channel opening failed"
        )
        @Nullable
        String errorCode();

        @FieldInfo(
                name = "Error message",
                help = "Error message if channel opening failed"
        )
        @Nullable
        String errorMessage();

        @FieldInfo(
                name = "Create time",
                help = "Time when channel was created",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long createTime();

        @FieldInfo(
                name = "Open time",
                help = "Time when channel was opened",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long openTime();

        @FieldInfo(
                name = "Close time",
                help = "Time when channel was closed",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long closeTime();

        // ====================================


        @FieldInfo(
                name = "Active",
                help = "Whether this channel is ative or not"
        )
        boolean active();

        @FieldInfo(
                name = "Remote pubkey",
                help = "The identity appPubkey of the remote node"
        )
        @Nullable
        String remotePubkey();

        @FieldInfo(
                name = "Channel point",
                help = "The outpoint (txid:index) of the funding transaction. With this value, Bob " +
                        "will be able to generate a signature for Alice's version of the commitment " +
                        "transaction."
        )
        @Nullable
        String channelPoint();

        @FieldInfo(
                name = "Channel id",
                help = "The unique channel ID for the channel. The first 3 bytes are the block " +
                        "height, the next 3 the index within the block, and the last 2 bytes are the " +
                        "output index for the channel."
        )
        long chanId();

        @FieldInfo(
                name = "Capacity",
                help = "The total amount of funds held in this channel."
        )
        long capacity();

        @FieldInfo(
                name = "Local balance",
                help = "This node's current balance in this channel"
        )
        long localBalance();

        @FieldInfo(
                name = "Remote balance",
                help = "The counterparty's current balance in this channel"
        )
        long remoteBalance();

        @FieldInfo(
                name = "Confirmation height",
                help = "The height at which this channel will be confirmed"
        )
        int confirmationHeight();

        @FieldInfo(
                name = "Limbo balance",
                help = "The balance in satoshis encumbered in this channel"
        )
        long limboBalance();

        @FieldInfo(
                name = "Maturity height",
                help = "The height at which funds can be swept into the wallet"
        )
        int maturityHeight();

        /*
          Remaining # of blocks until the commitment output can be swept.
          Negative values indicate how many blocks have passed since becoming
          mature.
        */
        // calculate from current height
        // int blocksTilMaturity = 5 [ json_name = "blocks_til_maturity" ];

        @FieldInfo(
                name = "Recovered balance",
                help = "The total value of funds successfully recovered from this channel."
        )
        long recoveredBalance();

        // repeated PendingHTLC pending_htlcs = 8 [ json_name = "pending_htlcs" ];

        @FieldInfo(
                name = "Commit fee",
                help = "The amount calculated to be paid in fees for the current set of commitment " +
                        "transactions. The fee amount is persisted with the channel in order to " +
                        "allow the fee amount to be removed and recalculated with each channel state " +
                        "update, including updates that happen after a system restart."
        )
        long commitFee();

        @FieldInfo(
                name = "Commit weight",
                help = "The weight of the commitment transaction."
        )
        long commitWeight();

        @FieldInfo(
                name = "Fee per kw",
                help = "The required number of satoshis per kilo-weight that the requester will pay " +
                        "at all times, for both the funding transaction and commitment transaction. " +
                        "This value can later be updated once the channel is open."
        )
        long feePerKw();

        @FieldInfo(
                name = "Unsettled balance",
                help = "The unsettled balance in this channel."
        )
        long unsettledBalance();

        @FieldInfo(
                name = "Total sats sent",
                help = "The total number of satoshis we've sent within this channel."
        )
        long totalSatoshisSent();

        @FieldInfo(
                name = "Total sats received",
                help = "The total number of satoshis we've received within this channel."
        )
        long totalSatoshisReceived();

        @FieldInfo(
                name = "Number of updates",
                help = "The total number of updates conducted within this channel."
        )
        long numUpdates();

        /**
         The list of active, uncleared HTLCs currently pending within the channel.
         */
        // public List<HTLC> pendingHtlcs;

        @FieldInfo(
                name = "CSV delay",
                help = "The CSV delay expressed in relative blocks. If the channel is force closed, " +
                        "we will need to wait for this many blocks before we can regain our funds."
        )
        int csvDelay();

        @FieldInfo(
                name = "Private",
                help = "Whether this channel is advertised to the network or not."
        )
        boolean isPrivate();

        @FieldInfo(
                name = "Initiator",
                help = "True if we were the ones that created the channel."
        )
        boolean initiator();

        @FieldInfo(
                name = "Status flags",
                help = "A set of flags showing the current state of the channel."
        )
        @Nullable
        String chanStatusFlags();

        @FieldInfo(
                name = "Local reserve sats",
                help = "The minimum satoshis this node is required to reserve in its balance."
        )
        long localChanReserveSat();

        @FieldInfo(
                name = "Remote reserve sats",
                help = "The minimum satoshis the other node is required to reserve in its balance."
        )
        long remoteChanReserveSat();

        @FieldInfo(
                name = "Remote reserve sats",
                help = "If true, then this channel uses the modern commitment format where the key " +
                        "in the output of the remote party does not change each state. This makes " +
                        "back up and recovery easier as when the channel is closed, the funds go " +
                        "directly to that key."
        )
        boolean staticRemoteKey();

        @FieldInfo(
                name = "Lifetime, sec",
                help = "The number of seconds that the channel has been monitored by the channel " +
                        "scoring system. Scores are currently not persisted, so this value may be " +
                        "less than the lifetime of the channel."
        )
        long lifetime();

        @FieldInfo(
                name = "Lifetime, sec",
                help = "The number of seconds that the remote peer has been observed as being online " +
                        "by the channel scoring system over the lifetime of the channel."
        )
        long uptime();

    }

    public interface ListChannelsRequest {
        long userId();

        // all, open, pending, closed
        @Nullable
        String stateFilter();

        // sort order: id
        @Nullable
        String sort();

        boolean sortDesc();
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

    public interface CloseChannelRequest {

        // either id or channelPoint must be provided
        long channelId();

        /// If true, then the channel will be closed forcibly. This means the current commitment transaction will be signed and broadcast.
        boolean force();

        /// The target number of blocks that the closure transaction should be confirmed by.
        int targetConf();

        /// A manual fee rate set in sat/byte that should be used when crafting the closure transaction.
        long satPerByte();
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

        @FieldInfo(
                name = "User id",
                help = "User who created this payment."
        )
        long userId();

        @FieldInfo(
                name = "Auth user id",
                help = "User who authorized this payment."
        )
        long authUserId();

        @FieldInfo(
                name = "Purpose",
                help = "User-assigned internal description, not sent to the payee."
        )
        @Nullable
        String purpose();

        @FieldInfo(
                name = "State",
                help = "State this payment is in"
                //FIXME add convertor
        )
        int state();

        @FieldInfo(
                name = "Error code",
                help = "Error code if payment failed"
        )
        @Nullable
        String errorCode();

        @FieldInfo(
                name = "Error message",
                help = "Error message if payment failed"
        )
        @Nullable
        String errorMessage();

        @FieldInfo(
                name = "Invoice description",
                help = "Description that was provided within the payment request"
        )
        @Nullable
        String invoiceDescription();

        @FieldInfo(
                name = "Invoice description hash",
                help = "Hash of the description that was provided within the payment request"
        )
        @Nullable
        String invoiceDescriptionHashHex();

        @FieldInfo(
                name = "Invoice fallback address",
                help = "Fallback address that was provided within the payment request"
        )
        @Nullable
        String invoiceFallbackAddr();

        @FieldInfo(
                name = "Payment address",
                help = "MPP payment addr that was provided within the payment request"
        )
        @Nullable
        String paymentAddrHex();

        @FieldInfo(
                name = "Invoice timestamp",
                help = "Time when invoice was created"
                // FIXME convertor
        )
        long invoiceTimestamp();

        @FieldInfo(
                name = "Invoice expiry, sec",
                help = "Invoice expiry interval"
        )
        long invoiceExpiry();

        // lnd fields

        @FieldInfo(
                name = "Destination public key",
                help = "The identity pubkey of the payment recipient"
        )
        @Nullable
        String destPubkey();

        @FieldInfo(
                name = "Value, msat",
                help = "The value of the payment in milli-satoshis"
        )
        long valueMsat();

        @FieldInfo(
                name = "Total value, msat",
                help = "The total value (value+fee) of the payment in milli-satoshis"
        )
        long totalValueMsat();

        @FieldInfo(
                name = "Payment hash",
                help = "The hash to use within the payment's HTLC"
        )
        @Nullable
        String paymentHashHex();

        @FieldInfo(
                name = "Payment request",
                help = "A bare-bones invoice for a payment within the Lightning Network. With the " +
                        "details of the invoice, the sender has all the request necessary to authorized a " +
                        "payment to the recipient."
        )
        @Nullable
        String paymentRequest();

        @FieldInfo(
                name = "Final CLTV delta",
                help = "The CLTV delta from the current height that should be used to set the " +
                        "timelock for the final hop."
        )
        int finalCltvDelta();

        @FieldInfo(
                name = "Fee limit fixed, msat",
                help = "The fee limit expressed as a fixed amount of satoshis. "+
                        "The maximum number of satoshis that will be paid as a fee of the payment. " +
                        "This value can be represented either as a percentage of the amount being " +
                        "sent, or as a fixed amount of the maximum fee the user is willing the pay to " +
                        "authorized the payment."
        )
        long feeLimitFixedMsat();

        @FieldInfo(
                name = "Fee limit, %",
                help = "The fee limit expressed as a percentage of the payment amount. "+
                        "The maximum number of satoshis that will be paid as a fee of the payment. " +
                        "This value can be represented either as a percentage of the amount being " +
                        "sent, or as a fixed amount of the maximum fee the user is willing the pay to " +
                        "authorized the payment."
        )
        long feeLimitPercent();

        @FieldInfo(
                name = "Outgoing channel id",
                help = "The channel id of the channel that must be taken to the first hop. If zero, " +
                        "any channel may be used."
        )
        long outgoingChanId();

        @FieldInfo(
                name = "CLTV limit",
                help = "An optional maximum total time lock for the route. This should not exceed " +
                        "lnd's `--max-cltv-expiry` setting. If zero, then the value of\n" +
                        "`--max-cltv-expiry` is enforced."
        )
        int cltvLimit();

        /**
         * An optional field that can be used to pass an arbitrary set of TLV records
         * to a peer which understands the new records. This can be used to pass
         * application specific request during the payment attempt.
         */
        @Nullable
        ImmutableMap<Long, byte[]> destCustomRecords();

        @FieldInfo(
                name = "Payment error",
                help = "Error returned by lnd on the last payment attempt."
        )
        @Nullable
        String paymentError();

        @FieldInfo(
                name = "Payment preimage",
                help = "Payment preimage retrieved in exchange for the payment."
        )
        @Nullable
        String paymentPreimageHex();

        @Nullable
        ImmutableList<RouteHint> routeHints();

        // feature bits assumed to be supportd by destination
        @Nullable
        ImmutableList<Integer> features();

        // public Route paymentRoute;

        // Payment fields

        @FieldInfo(
                name = "Create time",
                help = "The time when this payment was created",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long createTime();

        @FieldInfo(
                name = "Send time",
                help = "The time when this payment was sent",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long sendTime();

        /// The path this payment took
        // instead, store payment routes w/ hops etc
        // public List<String> path;

        @FieldInfo(
                name = "Fee, msat",
                help = "The fee paid for this payment in milli-satoshis"
        )
        long feeMsat();

        @FieldInfo(
                name = "Contact public key",
                help = "Public key of the contact if key-send to the contact was requested"
        )
        @Nullable
        String contactPubkey();

        @FieldInfo(
                name = "Message",
                help = "Message attached to the key-send payment"
        )
        @Nullable
        String message();

        @Nullable
        String senderPubkey();

        @FieldInfo(
                name = "Key-send",
                help = "Whether this is key-send payment: send preimage as TLV to make invoiceless payment"
        )
        boolean isKeysend();
    }

    public interface Peer {
        @FieldInfo(
                name = "Public key",
                help = "The identity pubkey of the peer"
        )
        @Nullable
        String pubkey();

        @FieldInfo(
                name = "Address",
                help = "Network address of the peer"
        )
        @Nullable
        String address();

        @FieldInfo(
                name = "Bytes sent",
                help = "Bytes of data transmitted to this peer"
        )
        long bytesSent();

        @FieldInfo(
                name = "Bytes received",
                help = "Bytes of data transmitted from this peer"
        )
        long bytesRecv();

        @FieldInfo(
                name = "Sats sent",
                help = "Satoshis sent to this peer"
        )
        long satsSent();

        @FieldInfo(
                name = "Sats received",
                help = "Satoshis received from this peer"
        )
        long satsRecv();

        @FieldInfo(
                name = "Inbound",
                help = "If connection was initiated by the peer"
        )
        boolean inbound();

        @FieldInfo(
                name = "Ping time",
                help = "Ping time to this peer"
        )
        long pingTime();

        @FieldInfo(
                name = "Sync type",
                help = "The type of sync we are currently performing with this peer.",
                convertors = {DefaultFieldMapper.PeerSyncTypeConverter.class}
        )
        int syncType();

        @FieldInfo(
                name = "Features",
                help = "Features advertised by the remote peer in their init message.",
                convertors = {DefaultFieldMapper.ImmutableIntListConverter.class}
        )
        @Nullable
        ImmutableList<Integer> features();

        @FieldInfo(
                name = "Permanent",
                help = "If connection was requested to be permanent"
        )
        boolean perm();

        @FieldInfo(
                name = "Online",
                help = "If connection is active"
        )
        boolean online();

        @FieldInfo(
                name = "Disabled",
                help = "If disconnection was requested"
        )
        boolean disabled();

        @FieldInfo(
                name = "Last connect time",
                help = "Last time the Connect was called, ms"
        )
        long lastConnectTime();

        @FieldInfo(
                name = "Last disconnect time",
                help = "Last time the Disconnect was called, ms"
        )
        long lastDisconnectTime();
    }

    public interface ConnectPeerRequest {
        @Nullable
        String pubkey();

        @Nullable
        String address();

        boolean perm();
    }

    public interface DisconnectPeerRequest {
        // use id or contactId or pubkey
        long id();

        // contact id
        long contactId();

        // peer pubkey
        @Nullable
        String pubkey();
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

    interface SendCoinsRequest {
        // lndroid fields

        @Nullable
        String purpose();

        // max number of tries
        int maxTries();

        // deadline for retries, in ms
        long maxTryTime();

        /// The map from addresses to amounts
        @Nullable
        ImmutableMap<String, Long> addrToAmount();

        /// The target number of blocks that this transaction should be confirmed by.
        int targetConf();

        /// A manual fee rate set in sat/byte that should be used when crafting the transaction.
        long satPerByte();

        /**
         If set, then the amount field will be ignored, and lnd will attempt to
         send all the coins under control of the internal wallet to the specified
         address. addrToAmount must contain a single address.
         */
        boolean sendAll();
    }

    interface Transaction {
        // lndroid fields

        // which tx created this payment
        @Nullable
        String txId();

        @FieldInfo(
                name = "User id",
                help = "User that created this transaction, or 0 if not created by this wallet."
        )
        long userId();

        @FieldInfo(
                name = "User id",
                help = "User that authorized this transaction."
        )
        long authUserId();

        @FieldInfo(
                name = "Create time",
                help = "Time when transaction was created.",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long createTime();

        @FieldInfo(
                name = "Send time",
                help = "Time when transaction was broadcasted.",
                convertors = {DefaultFieldMapper.DateTimeMsConverter.class}
        )
        long sendTime();

        // when user was notified about this tx, auto-filled for
        // txs created by this wallet, for others notifiedTransaction method
        // must be called to mark txs as notified
        long notifyTime();

        @FieldInfo(
                name = "Purpose",
                help = "User-assigned internal description of transaction."
        )
        @Nullable
        String purpose();

        @FieldInfo(
                name = "State",
                help = "State of the transaction."
                // FIXME convertor
        )
        int state();

        @FieldInfo(
                name = "Error code",
                help = "Error code if transaction creation failed."
        )
        @Nullable
        String errorCode();

        @FieldInfo(
                name = "Error message",
                help = "Error message if transaction creation failed."
        )
        @Nullable
        String errorMessage();

        // request fields

        @FieldInfo(
                convertors = {DefaultFieldMapper.TransactionConvertor.class}
        )
        @Nullable
        ImmutableMap<String, Long> addrToAmount();

        @FieldInfo(
                name = "Target conf blocks",
                help = "The target number of blocks that this transaction should be confirmed by."
        )
        int targetConf();

        @FieldInfo(
                name = "Sat per byte",
                help = "A manual fee rate set in sat/byte that should be used when crafting the transaction."
        )
        long satPerByte();

        @FieldInfo(
                name = "Send all",
                help = "If set, then the amount field will be ignored, and lnd will attempt to " +
                        "send all the coins under control of the internal wallet to the specified " +
                        "address. addrToAmount must contain a single address."
        )
        boolean sendAll();


        // lnd fields

        @FieldInfo(
                name = "Tx hash",
                help = "The on-chain transaction hash."
        )
        @Nullable
        String txHash();

        @FieldInfo(
                name = "Amount, sat",
                help = "The total transaction amount in sats."
        )
        long amount();

        @FieldInfo(
                name = "Confirmations",
                help = "The number of confirmations."
        )
        int numConfirmations();

        @FieldInfo(
                name = "Block hash",
                help = "The hash of the block this transaction was included in."
        )
        @Nullable
        String blockHash();

        @FieldInfo(
                name = "Block height",
                help = "The height of the block this transaction was included in."
        )
        int blockHeight();

        @FieldInfo(
                name = "Timestamp",
                help = "Timestamp of this transaction."
        )
        long timestamp();

        @FieldInfo(
                name = "Fee, sat",
                help = "Fees paid for this transaction in sats."
        )
        long totalFees();

        @FieldInfo(
                convertors = {DefaultFieldMapper.TransactionConvertor.class}
        )
        @Nullable
        ImmutableList<String> destAddresses();

        @FieldInfo(
                name = "Tx hex",
                help = "The raw transaction hex."
        )
        @Nullable
        String rawTxHex();
    }

    public interface ListTransactionsRequest {
        long userId();

        long timeFrom();

        long timeTill();

        // sort order: time, amount
        @Nullable
        String sort();

        boolean sortDesc();
    }

    interface EstimateFeeRequest {
        /// The map from addresses to amounts for the transaction.
        ImmutableMap<String, Long> addrToAmount();

        /// The target number of blocks that this transaction should be confirmed by.
        int targetConf();
    }

    interface EstimateFeeResponse {
        /// The total fee in satoshis.
        long feeSat();

        /// The fee rate in satoshi/byte.
        long feerateSatPerByte();
    }

    interface Utxo {
        @FieldInfo(
                name = "Type",
                help = "The type of address"
                // FIXME convertor
        )
        int type();

        @FieldInfo(
                name = "Address",
                help = "The address"
        )
        String address();

        @FieldInfo(
                name = "Value, sat",
                help = "The value of the unspent coin in satoshis"
        )
        long amountSat();

        @FieldInfo(
                name = "Pkscript",
                help = "The pkscript in hex"
        )
        String pkScript();

        @FieldInfo(
                name = "Tx hash",
                help = "Reversed, hex-encoded string representing the transaction id."
        )
        String txidHex();

        @FieldInfo(
                name = "Output index",
                help = "The index of the output on the transaction."
        )
        int outputIndex();

        @FieldInfo(
                name = "Confirmations",
                help = "The number of confirmations for the Utxo."
        )
        long confirmations();
    }

    public interface ListUtxoRequest {
        long minConfirmations();

        long maxConfirmations();

        // sort order: amount, confirmations
        @Nullable
        String sort();

        boolean sortDesc();
    }

    public interface ListPeersRequest {

        long authUserId();

        // all, online, enabled(!disabled), offline(!online && !disabled), disabled
        @Nullable
        String stateFilter();

        // sort order: id, pubkey, address
        @Nullable
        String sort();

        boolean sortDesc();
    }

    public interface ListUsersRequest {
        @Nullable
        String role();

        // sort order: id, name
        @Nullable
        String sort();

        boolean sortDesc();
    }

    public interface NotifiedInvoicesRequest {
        ImmutableList<Long> invoiceIds();
    }

    public interface NotifiedInvoicesResponse {
    }

    public interface SubscribeNewPaidInvoices {

        boolean noAuth();

        String protocolExtension();

        // for broadcast-based notifications
        String componentPackageName();
        String componentClassName();
    }

    public interface BackgroundInfo {
        boolean isActive();
        long activeSendPaymentCount();
        long activeOpenChannelCount();
        long activeCloseChannelCount();
        long activeSendCoinCount();
        long pendingChannelCount();
    }

    public interface PaidInvoicesEvent {
        @Nullable
        ImmutableList<Long> invoiceIds();
        long satsReceived();
        long invoicesCount();
    }
}
