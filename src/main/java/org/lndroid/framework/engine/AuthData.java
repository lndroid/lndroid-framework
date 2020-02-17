package org.lndroid.framework.engine;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.lndroid.framework.common.AutoValueClass;

public class AuthData {

    // auth messages used by AuthClient
    public static final int MESSAGE_TYPE_PRIV = 1;
    public static final int MESSAGE_TYPE_AUTHED = 2;
    public static final int MESSAGE_TYPE_AUTH_SUB = 3;
    public static final int MESSAGE_TYPE_WALLET_STATE_SUB = 4;
    public static final int MESSAGE_TYPE_GEN_SEED = 5;
    public static final int MESSAGE_TYPE_INIT_WALLET = 6;
    public static final int MESSAGE_TYPE_UNLOCK_WALLET = 7;
    public static final int MESSAGE_TYPE_GET = 8;
    public static final int MESSAGE_TYPE_GET_TX = 9;
    public static final int MESSAGE_TYPE_USER_AUTH_INFO = 10;

    @AutoValue
    @AutoValueClass(className = AutoValue_AuthData_AuthMessage.class)
    public static abstract class AuthMessage {

//        public static transient String VERSION = AuthMessage.class.getName()+":0.1.0";

        // auth message contexts are attached to sequential ids
        public abstract int id();

        // message type
        public abstract int type();

        // auth request id
        public abstract long authId();

        // tx credentials
        public abstract long userId();
        @Nullable public abstract String txId();
        @Nullable public abstract String pluginId();

        // errors sent back from server
        @Nullable public abstract String code();
        @Nullable public abstract String error();

        // auth request, response, tx request, etc
        @Nullable public abstract Object data();

        public static Builder builder() {
            return new AutoValue_AuthData_AuthMessage.Builder()
                    .setId(0)
                    .setType(0)
                    .setAuthId(0)
                    .setUserId(0);
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder {

            public abstract Builder setId(int id);
            public abstract Builder setType(int type);
            public abstract Builder setUserId(long userId);
            public abstract Builder setPluginId(String pluginId);
            public abstract Builder setAuthId(long authId);
            public abstract Builder setTxId(String txId);
            public abstract Builder setCode(String code);
            public abstract Builder setError(String error);
            public abstract Builder setData(Object data);

            public abstract AuthMessage build();
        }
    }

}
