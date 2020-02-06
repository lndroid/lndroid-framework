package org.lndroid.framework;

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
    String generateUserKeyPair(int userId, String role);
    byte[] decryptWalletPassword(EncryptedData data);
    EncryptedData encryptWalletPassword(byte[] data);
}
