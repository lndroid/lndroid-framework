package org.lndroid.framework.engine;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.common.IVerifier;

// NOTE: since android key store implementation
// is not thread-safe, plugin server will only
// use it in his own thread, and wallet code
// MUST NOT access keystore anywhere else.
// To check isAvailable flag wallet should ask
// plugin server through auth/plugin client
public interface IKeyStore {
    class EncryptedData {
        public byte[] data;
        public byte[] iv;
    }

    // all these calls will happen in PluginServer thread only

    // init might be called twice, as with DaoProvider
    void init();

    boolean isAvailable();
    boolean isDeviceSecure();
    boolean isBiometricsAvailable();
    boolean isDeviceLocked();

    String generatePasswordKeyNonce();
    String generateKeyPair(String alias, String authType, String nonce, String password);
    ISigner getKeySigner(String alias);
    IVerifier getVerifier();

    byte[] decryptWalletPassword(EncryptedData data);
    EncryptedData encryptWalletPassword(byte[] data);

}
