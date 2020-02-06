package org.lndroid.framework;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class DefaultKeyStore implements IKeyStore {
    private static final String TAG = "DefaultKeyStore";
    private static final String WP_KEY_ALIAS = "WALLET_PASSWORD_KEY";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static int DEFAULT_AUTH_VALIDITY_DURATION = 4 * 60 * 60; // 4h

    private Context ctx_;
    private boolean isAvailable_;
    private int authValidityDuration_;

    private SecretKey generateWalletPasswordKey() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        try {
            final int avd = authValidityDuration_ != 0 ? authValidityDuration_ : DEFAULT_AUTH_VALIDITY_DURATION;

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(WP_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    ;

            KeyguardManager km = (KeyguardManager)ctx_.getSystemService(Context.KEYGUARD_SERVICE);
            if (km.isDeviceSecure()) {
                builder
                        // w/ these 2 settings we get a key that is accessible for AVD
                        // seconds after the last user Auth, even when device is locked,
                        // giving our daemon some time to do nightly network sync,
                        // and then when user unlocks again in the morning the
                        // key is available again w/o forcing user to re-auth
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(avd)
                ;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // these methods require API min 28
                builder
                        // NO! this won't let our wallet run as daemon!
                        // .setUnlockedDeviceRequired(true)

                        // prefer StrongBox
                        .setIsStrongBoxBacked(true)
                ;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                // this method requires API min 24
                builder.setInvalidatedByBiometricEnrollment(true);
            }

            keyGenerator.init(builder.build());
            return keyGenerator.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "generate key error "+e);
        }

        return null;
    }

    public DefaultKeyStore(Context ctx) {
        ctx_ = ctx;
    }

    public void setAuthValidityDuration(int avd) {
        authValidityDuration_ = avd;
    }

    public void init() {

        // FIXME look at ways to use sdk 18 where only KeyPair can be created

        // if sdk < 23 symm keys not supported by AndroidKeystore
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            // generate the key immediately to check if
            // device supports it before we declare
            // our key store as 'isAvailable'
            if (!ks.containsAlias(WP_KEY_ALIAS))
                generateWalletPasswordKey();

            isAvailable_ = ks.containsAlias(WP_KEY_ALIAS);
        }
        catch(Exception e){
            Log.e(TAG, "error " + e);
        }
    }

    public boolean isAvailable() {
        return isAvailable_;
    }

    // tries to encrypt the password, if returns null
    // then we don't store password and thus will
    // ask user for his password explicitly on every lnd start
    @Override
    public synchronized EncryptedData encryptWalletPassword(byte[] data) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        if (!isAvailable_)
            throw new RuntimeException("KeyStore not available");

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            // FIXME what are params?
            ks.load(null);

            SecretKey key = null;
            if (!ks.containsAlias(WP_KEY_ALIAS)) {
                // FIXME params?
                key = ((KeyStore.SecretKeyEntry) ks.getEntry(WP_KEY_ALIAS, null)).getSecretKey();
            } else {
                // key could have been invalidated after we've used it since the last time
                key = generateWalletPasswordKey();
            }

            if (key == null)
                return null;

            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            EncryptedData result = new EncryptedData();
            result.iv = cipher.getIV();
            result.data = cipher.doFinal(data);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "wp encrypt error " + e);
        }

        return null;
    }

    // if null is returned then we just drop the current encrypted password
    // and ask user for new one
    @Override
    public synchronized byte[] decryptWalletPassword(EncryptedData data) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        if (!isAvailable_)
            throw new RuntimeException("KeyStore not available");

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            // FIXME what are params?
            ks.load(null);

            // FIXME params?
            SecretKey key = ((KeyStore.SecretKeyEntry) ks.getEntry(WP_KEY_ALIAS, null)).getSecretKey();
            if (key != null) {
                final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                final GCMParameterSpec spec = new GCMParameterSpec(128, data.iv);
                cipher.init(Cipher.DECRYPT_MODE, key, spec);
                return cipher.doFinal(data.data);
            }
        } catch (Exception e) {
            Log.e(TAG, "wp decrypt error " + e);
        }

        return null;
    }
}
