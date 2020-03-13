package org.lndroid.framework.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.IOException;
import java.lang.reflect.Type;

import org.lndroid.framework.WalletData;

public class PluginData {

    public static final String BROADCAST_ACTION = "org.lndroid.framework.broadcast.ACTION_PLUGIN_REPLY_READY";
    public static final String BROADCAST_PLUGIN = "org.lndroid.framework.broadcast.EXTRA_PLUGIN";
    public static final String BROADCAST_TX = "org.lndroid.framework.broadcast.EXTRA_TX";

    // message 'what's: remote, local tx, local auth?
    public static final int MESSAGE_WHAT_IPC_TX = 0;
    public static final int MESSAGE_WHAT_LOCAL_TX = 1;
    public static final int MESSAGE_WHAT_AUTH = 2;

    public static final String API_VERSION = "0.1.0";

    // ipc Bundle keys
    public static final String IPC_VERSION = "ver";
    public static final String IPC_MESSAGE = "msg";
    public static final String IPC_SIGNATURE = "sign";

    // current IPC Bundle format version
    public static final String IPC_CURRENT_VERSION = "org.lndroid.framework.IPC_VERSION:0.1.0";

    // tx messages used by PluginClient
    public static final String MESSAGE_TYPE_START = "start";
    public static final String MESSAGE_TYPE_REQUEST = "req";
    public static final String MESSAGE_TYPE_REPLY = "rep";
    public static final String MESSAGE_TYPE_STOP = "stop";
    public static final String MESSAGE_TYPE_ERROR = "error";
    public static final String MESSAGE_TYPE_AUTH = "auth";
    public static final String MESSAGE_TYPE_AUTHED = "authed";
    public static final String MESSAGE_TYPE_DONE = "done";

    @AutoValue @AutoValueClass(className = AutoValue_PluginData_PluginMessage.class)
    public static abstract class PluginMessage implements IPluginData {

//        public static transient String VERSION = PluginMessage.class.getName()+":0.1.0";

        // common fields
        public abstract String type();

        public abstract long timestamp();

        public abstract String apiVersion();

        // the caller identity, null for in-process server replies,
        // IPC replies will have the identity w/ server appPubkey.
        @Nullable public abstract WalletData.UserIdentity userIdentity();

        // some auth messages don't require pluginId
        @Nullable public abstract String pluginId();

        // tx fields
        @Nullable public abstract String txId();
        @Nullable public abstract Long timeout();

        // errors sent back from server
        @Nullable public abstract String code();
        @Nullable public abstract String error();

        // auth related
        @Nullable public abstract Long authId();
        @Nullable public abstract Boolean isPrivileged();

        // we want to freely re-assign new tokens w/o rebuilding the message
        @Nullable public String sessionToken() { return sessionToken_; }
        public void assignSessionToken(String token) { sessionToken_ = token; }
        private transient String sessionToken_;

        // attach data as payload for this message,
        // along with it's type that will be used to encode the message
        // for IPC transport.
        // NOTE: it is assumed that all passed objects are immutable,
        // and both server and client threads can use them freely.
        public void assignData(Object data, @NonNull Type dataType) {
            if (dataType_ != null)
                throw new RuntimeException("Data type already assigned");

            data_ = data;
            if (data != null)
                dataType_ = dataType;
        }
        private transient Object data_;

        // attach a data type to be used by codec to
        // decode the data received from IPC transport
        @Override
        public void assignDataType(Type type) {
            if (data_ == null && dataType_ != null)
                throw new RuntimeException("Data type already assigned");

            dataType_ = type;
        }
        private transient Type dataType_;

        // attach codec provider to let the message
        // (de)serialize data from/to ipcData,
        // must be set before getRequestData or encodeData are called
        @Override
        public void assignCodecProvider(ICodecProvider cp) {
            codecProvider_ = cp;
        }
        private transient ICodecProvider codecProvider_;

        // ipcData is decoded and memoized for future access
        @Nullable @Override
        public <T> T getData() throws IOException {
            if (data_ == null && ipcData_ != null) {
                if (codecProvider_ == null)
                    throw new RuntimeException("Message data codec provider not assigned");

                // get codec
                ICodec<T> c = codecProvider_.get(dataType_);
                if (c == null)
                    throw new IOException("Message data codec not found");

                // decode
                data_ = c.decode(ipcData_);

                // ipcData is sent by remote clients and our server
                // must catch this exception and handle properly i.e. by
                // returning PROTOCOL_MESSAGE error
                if (data_ == null)
                    throw new IOException("Unexpected message data");
            }

            return (T)data_;
        }

        public void encodeData() {
            if (data_ == null)
                return;

            if (dataType_ == null)
                throw new RuntimeException("Message data type not provided");

            if (codecProvider_ == null)
                throw new RuntimeException("Message data codec provider not assigned");

            ICodec c = codecProvider_.get(dataType_);
            if (c == null)
                throw new RuntimeException("No codec for message data");

            ipcData_ = c.encode(data_);
            if (ipcData_ == null)
                throw new RuntimeException("Failed to encode message data");
        }

        // this is not an autovalue property, and is only
        // set if encodeData is called by the plugin server,
        // public to let codec serialize it.
        @Nullable public byte[] ipcData() {
            return ipcData_;
        }
        private byte[] ipcData_;

        public static Builder builder() {
            return new AutoValue_PluginData_PluginMessage.Builder()
                    .setTimestamp(System.currentTimeMillis())
                    .setApiVersion(API_VERSION);
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder setType(String type);
            public abstract Builder setTimestamp(long timestamp);
            public abstract Builder setApiVersion(String apiVersion);
            public abstract Builder setUserIdentity(WalletData.UserIdentity ui);
            public abstract Builder setPluginId(String pluginId);
            public abstract Builder setTxId(String txId);
            public abstract Builder setTimeout(Long timeout);
            public abstract Builder setCode(String code);
            public abstract Builder setError(String error);
            public abstract Builder setAuthId(Long authId);
            public abstract Builder setIsPrivileged(Boolean isPrivileged);

            public abstract PluginMessage build();
        }
    }
}
