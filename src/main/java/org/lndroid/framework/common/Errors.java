package org.lndroid.framework.common;

public class Errors {
    // provided user identity not found
    public static final String UNKNOWN_CALLER = "org.lndroid.errors.UNKNOWN_CALLER";

    // caller is not authorized to perform this action
    public static final String FORBIDDEN = "org.lndroid.errors.FORBIDDEN";

    // User has rejected the authorization of caller's request
    public static final String REJECTED = "org.lndroid.errors.REJECTED";

    // internal wallet error
    public static final String WALLET_ERROR = "org.lndroid.errors.WALLET_ERROR";

    // wallet not initialized
    public static final String NO_WALLET = "org.lndroid.errors.NO_WALLET";

    // wallet locked, need UI to unlock it
    public static final String LOCKED = "org.lndroid.errors.LOCKED";

    // transaction timed out, authorization of this tx is no longer possible,
    // receiving data for this tx is no longer possible
    public static final String TX_TIMEOUT = "org.lndroid.errors.TX_TIMEOUT";

    // dataset is invalidated, caller needs to retry his read request
    public static final String TX_INVALIDATE = "org.lndroid.errors.TX_INVALIDATE";

    // wrong message pattern for the called plugin
    public static final String PLUGIN_PROTOCOL = "org.lndroid.errors.PLUGIN_PROTOCOL";

    // wrong message payload type for the called plugin
    public static final String PLUGIN_MESSAGE = "org.lndroid.errors.PLUGIN_MESSAGE";

    // wrong message payload content for the called plugin
    public static final String PLUGIN_INPUT = "org.lndroid.errors.PLUGIN_INPUT";

    // lnd daemon error
    public static final String LND_ERROR = "org.lndroid.errors.LND_ERROR";

    // invalid auth request id, etc
    public static final String AUTH_INPUT = "org.lndroid.errors.AUTH_INPUT";

    // IPC server failed, reconnect and retry
    public static final String IPC_ERROR = "org.lndroid.errors.IPC_ERROR";

    // FIXME wtf?
    public static final String PAYMENT_NO_HASH = "org.lndroid.errors.PAYMENT_NO_HASH";
    public static final String PAYMENT_BAD_INPUT = "org.lndroid.errors.PAYMENT_BAD_INPUT";

    public static String errorMessage(String e) {
        switch (e) {
            case UNKNOWN_CALLER: return "Calling user not found";
            case FORBIDDEN: return "Call forbidden";
            case REJECTED: return "Call rejected";
            case WALLET_ERROR: return "Internal wallet error";
            case NO_WALLET: return "Wallet not found";
            case LOCKED: return "Wallet locked";
            case PLUGIN_INPUT: return "Invalid plugin input";
            case TX_TIMEOUT: return "Transaction time out";
            case PAYMENT_NO_HASH: return "Payment hash not provided";
            case PAYMENT_BAD_INPUT: return "Payment has wrong parameters";
            case TX_INVALIDATE: return "Data set invalidated";
            case PLUGIN_PROTOCOL: return "Plugin protocol error";
            case PLUGIN_MESSAGE: return "Plugin message error";
            case LND_ERROR: return "Lnd error";
            case IPC_ERROR: return "IPC error";
            case AUTH_INPUT: return "Unknown auth request";
        }

        return "";
    }
}
