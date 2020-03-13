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
    public static final String WALLET_LOCKED = "org.lndroid.errors.WALLET_LOCKED";

    // device locked, txs from user roles are rejected
    // FIXME not used! do we need it?
    public static final String DEVICE_LOCKED = "org.lndroid.errors.DEVICE_LOCKED";

    // transaction timed out, authorization of this tx is no longer possible,
    // receiving data for this tx is no longer possible
    public static final String TX_TIMEOUT = "org.lndroid.errors.TX_TIMEOUT";

    // dataset is invalidated, caller needs to retry his read request
    public static final String TX_INVALIDATE = "org.lndroid.errors.TX_INVALIDATE";

    // server terminated the transaction
    public static final String TX_DONE = "org.lndroid.errors.TX_DONE";

    // wrong message format, version, timestamp, etc
    public static final String MESSAGE_FORMAT = "org.lndroid.errors.MESSAGE_FORMAT";

    // wrong message auth info like signature or session token
    public static final String MESSAGE_AUTH = "org.lndroid.errors.MESSAGE_AUTH";

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

    // IPC server failed, reconnect and retry (returned by client-side to calling code)
    public static final String IPC_ERROR = "org.lndroid.errors.IPC_ERROR";

    // IPC server identity changed, need to run connect-to-wallet flow
    public static final String IPC_IDENTITY_ERROR = "org.lndroid.errors.IPC_IDENTITY_ERROR";

    // IPC API version mismatch btw client/server
    public static final String IPC_API_VERSION = "org.lndroid.errors.IPC_API_VERSION";

    public static String errorMessage(String e) {
        switch (e) {
            case UNKNOWN_CALLER: return "Calling user not found";
            case FORBIDDEN: return "Call forbidden";
            case REJECTED: return "Call rejected";
            case WALLET_ERROR: return "Internal wallet error";
            case NO_WALLET: return "Wallet not found";
            case WALLET_LOCKED: return "Wallet locked";
            case DEVICE_LOCKED: return "Device locked";
            case PLUGIN_INPUT: return "Invalid plugin input";
            case TX_TIMEOUT: return "Transaction time out";
            case TX_INVALIDATE: return "Data set invalidated";
            case TX_DONE: return "Transaction finished";
            case PLUGIN_PROTOCOL: return "Plugin protocol error";
            case PLUGIN_MESSAGE: return "Plugin message error";
            case MESSAGE_FORMAT: return "Bad message format";
            case MESSAGE_AUTH: return "Message auth failed";
            case LND_ERROR: return "Lnd error";
            case IPC_ERROR: return "IPC error";
            case IPC_IDENTITY_ERROR: return "IPC server identity error";
            case AUTH_INPUT: return "Unknown auth request";
            case IPC_API_VERSION: return "API version mismatch, please update your app and wallet";
        }

        return "";
    }
}
