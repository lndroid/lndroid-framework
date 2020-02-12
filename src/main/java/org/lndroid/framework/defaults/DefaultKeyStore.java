package org.lndroid.framework.defaults;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.common.IVerifier;
import org.lndroid.framework.engine.IKeyStore;
import org.lndroid.framework.common.HEX;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class DefaultKeyStore implements IKeyStore {
    private static final String TAG = "DefaultKeyStore";
    private static final String WP_KEY_ALIAS = "WALLET_PASSWORD_KEY";
    private static final String USER_KEY_ALIAS_PREFIX = "USER_KEY_";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static int DEFAULT_AUTH_VALIDITY_DURATION = 6 * 60 * 60; // 6h
    private static int PASSWORD_KEY_SIZE = 128;

    private static final String lock_ = "";
    private static DefaultKeyStore instance_;

    private Context ctx_;
    private boolean isAvailable_;
    private int wpAuthValidityDuration_;

    DefaultKeyStore(Context appCtx) {
        ctx_ = appCtx;
    }

    public static DefaultKeyStore getInstance(Context appCtx) {
        synchronized (lock_) {
            if (instance_ == null)
                instance_ = new DefaultKeyStore(appCtx);
            return instance_;
        }
    }

    public void setWalletPasswordAuthValidityDuration(int avd) {
        wpAuthValidityDuration_ = avd;
    }

    @Override
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

    @Override
    public boolean isAvailable() {
        return isAvailable_;
    }

    @Override
    public boolean isDeviceSecure() {
        KeyguardManager m = (KeyguardManager) ctx_.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;

        return m.isDeviceSecure();
    }

    @Override
    public boolean isDeviceLocked() {
        KeyguardManager m = (KeyguardManager) ctx_.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;

        return m.isKeyguardLocked();
    }

    @Override
    public boolean isBiometricsAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            BiometricManager m = (BiometricManager) ctx_.getSystemService(Context.BIOMETRIC_SERVICE);
            return m != null && m.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FingerprintManager m = (FingerprintManager) ctx_.getSystemService(Context.FINGERPRINT_SERVICE);
            return m != null && m.isHardwareDetected() && m.hasEnrolledFingerprints();
        } else {
            return false;
        }
    }

    @Override
    public String generatePasswordKeyNonce() {
        byte[] nonce = new byte[PASSWORD_KEY_SIZE / 8];
        SecureRandom r = new SecureRandom();
        r.nextBytes(nonce);
        return HEX.fromBytes(nonce);
    }

    private SecretKey generateWalletPasswordKey() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        try {
            final int avd = wpAuthValidityDuration_ != 0
                    ? wpAuthValidityDuration_
                    : DEFAULT_AUTH_VALIDITY_DURATION;

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

    // tries to encrypt the password, if returns null
    // then we don't store password and thus will
    // ask user for his password explicitly on every lnd start
    @Override
    public EncryptedData encryptWalletPassword(byte[] data) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        if (!isAvailable_)
            throw new RuntimeException("KeyStore not available");

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            SecretKey key = null;
            if (ks.containsAlias(WP_KEY_ALIAS)) {
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
    public byte[] decryptWalletPassword(EncryptedData data) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        if (!isAvailable_)
            throw new RuntimeException("KeyStore not available");

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

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

    private SecureRandom generateSeedFromPassword(String nonce, String password) {
        try {
            // first do PWKDF2 to get secret key from password
            final int iterationCount = 10000;
            KeySpec keySpec = new PBEKeySpec(
                    password.toCharArray(),
                    HEX.toBytes(nonce),
                    iterationCount,
                    PASSWORD_KEY_SIZE);
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();

            // next, use secret as random seed for keypair
            return new SecureRandom(keyBytes);

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String generateKeyPair(String alias, String authType, String nonce, String password) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        if (authType == null)
            authType = WalletData.AUTH_TYPE_NONE;

        try {

            KeyGenParameterSpec.Builder params = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256);

            SecureRandom seed = null;

            switch (authType) {
                case WalletData.AUTH_TYPE_NONE:
                    //key is always available
                    break;
                case WalletData.AUTH_TYPE_BIO:
                    // Bio auth
                    params
                            .setUserAuthenticationRequired(true)
                            .setUserAuthenticationValidityDurationSeconds(-1);
                    break;
                case WalletData.AUTH_TYPE_DEVICE_SECURITY:
                    // short screen-lock auth
                    params
                            .setUserAuthenticationRequired(true)
                            .setUserAuthenticationValidityDurationSeconds(2);
                    break;

                case WalletData.AUTH_TYPE_SCREEN_LOCK:
                    // screen-lock auth with high duration
                    params
                            .setUserAuthenticationRequired(true)
                            .setUserAuthenticationValidityDurationSeconds(3600); // 1h
                    break;

                case WalletData.AUTH_TYPE_PASSWORD:
                    seed = generateSeedFromPassword(nonce, password);
                    break;

                default:
                    return null;
            }

            if (seed == null)
                seed = new SecureRandom();

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(params.build(), seed);

            KeyPair kp = kpg.generateKeyPair();

            // A known bug in Android 6.0 (API Level 23) causes user authentication-related
            // authorizations to be enforced even for public keys. To work around this issue extract
            // the public key material to use outside of Android Keystore. For example:
            PublicKey unrestrictedPublicKey =
                    KeyFactory.getInstance(kp.getPublic().getAlgorithm()).generatePublic(
                            new X509EncodedKeySpec(kp.getPublic().getEncoded()));

            // password-based keys are immediately deleted after we've
            // acquired the pubkey
            if (WalletData.AUTH_TYPE_PASSWORD.equals(authType)) {
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                ks.deleteEntry(alias);
            }

            return HEX.fromBytes(unrestrictedPublicKey.getEncoded());

        } catch (Exception e) {
            Log.e(TAG, "generate key pair error "+e);
        }

        return null;
    }

    @Override
    public IVerifier getVerifier() {
        return new DefaultVerifier();
    }

    @Override
    public ISigner getKeySigner(String alias) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        KeyStore.PrivateKeyEntry key = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            if (!ks.containsAlias(alias))
                return null;

            key = ((KeyStore.PrivateKeyEntry) ks.getEntry(alias, null));

        } catch (Exception e) {
            Log.e(TAG, "getKeySigner error "+e);
            return null;
        }

        if (key == null)
            return null;

        return new DefaultSigner(key.getPrivateKey(), key.getCertificate().getPublicKey());
    }
}
