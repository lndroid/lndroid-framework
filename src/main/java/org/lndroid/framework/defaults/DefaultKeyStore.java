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
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.common.IVerifier;
import org.lndroid.framework.engine.IKeyStore;
import org.lndroid.framework.common.HEX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class DefaultKeyStore implements IKeyStore {
    private static final String TAG = "DefaultKeyStore";
    private static final String WP_KEY_ALIAS = "WALLET_PASSWORD_KEY";
    private static final String WP_CIPHER = "AES/GCM/NoPadding";
    private static final String PASSWORD_ALIAS_SUFFIX = "_pwd";
    private static int DEFAULT_WP_AUTH_VALIDITY_DURATION = 6 * 60 * 60; // 6h
    private static int PASSWORD_KEY_SIZE = 32; // bytes
    private static int PASSWORD_NONCE_SIZE = 32; // sha256

    private static final Object lock_ = new Object();
    private static DefaultKeyStore instance_;
    private String passwordDir_;

    private Context ctx_;
    private boolean isAvailable_;
    private int wpAuthValidityDuration_;

    DefaultKeyStore(Context appCtx, String passwordDir) {
        ctx_ = appCtx;
        passwordDir_ = passwordDir;
    }

    public static DefaultKeyStore getInstance(Context appCtx, String passwordDir) {
        synchronized (lock_) {
            if (instance_ == null)
                instance_ = new DefaultKeyStore(appCtx, passwordDir);
            return instance_;
        }
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
            if (!ks.containsAlias(WP_KEY_ALIAS)) {
                SecretKey sk = generateWalletPasswordKey();
                Log.i(TAG, "wallet key generated "+(sk != null));
            }

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

    private SecretKey generateWalletPasswordKey() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        try {
            final int avd = wpAuthValidityDuration_ != 0
                    ? wpAuthValidityDuration_
                    : DEFAULT_WP_AUTH_VALIDITY_DURATION;

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

            // FIXME only set if SB is available
            /*
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // these methods require API min 28
                builder
                        // NO! this won't let our wallet run as daemon!
                        // .setUnlockedDeviceRequired(true)

                        // prefer StrongBox
                        .setIsStrongBoxBacked(true)
                ;
            }

             */

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

            final Cipher cipher = Cipher.getInstance(WP_CIPHER);
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
                final Cipher cipher = Cipher.getInstance(WP_CIPHER);
                final GCMParameterSpec spec = new GCMParameterSpec(128, data.iv);
                cipher.init(Cipher.DECRYPT_MODE, key, spec);
                return cipher.doFinal(data.data);
            }
        } catch (Exception e) {
            Log.e(TAG, "wp decrypt error " + e);
        }

        return null;
    }

    private String getPasswordFile(String alias) {
        return passwordDir_+"/."+alias;
    }

    private byte[] generatePasswordKey(byte[] nonce, String password) {
        try {
            // first do PWKDF2 to get secret key from password
            final long start = System.currentTimeMillis();
            final int iterationCount = 10000;
            KeySpec keySpec = new PBEKeySpec(
                    password.toCharArray(),
                    nonce,
                    iterationCount,
                    PASSWORD_KEY_SIZE * 8);
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA1");

            byte[] key = keyFactory.generateSecret(keySpec).getEncoded();
            Log.i(TAG, "pbkdf for nonce "+nonce+" in "+(System.currentTimeMillis() - start)+" ms");
            return key;
        } catch (Exception e) {
            Log.e(TAG, "generatePasswordKey error "+e);
            return null;
        }
    }

    static class PasswordKey {
        byte[] nonce;
        byte[] key;
    }

    private void writePasswordKey(String alias, PasswordKey pk) {
        try {
            File file = new File(getPasswordFile(alias));

            FileOutputStream f = new FileOutputStream(file);
            f.write(pk.key);
            f.write(pk.nonce);
            f.close();

        } catch (Exception e) {
            Log.e(TAG, "writeKeyPassword error "+e);
            throw new RuntimeException(e);
        }
    }

    private PasswordKey readPasswordKey(String alias) {
        try {
            FileInputStream s = new FileInputStream(getPasswordFile(alias));
            byte[] buffer = new byte[512];
            final int r = s.read(buffer);
            if (r == (PASSWORD_KEY_SIZE + PASSWORD_NONCE_SIZE)) {
                PasswordKey pk = new PasswordKey();
                pk.key = Arrays.copyOfRange(buffer, 0, PASSWORD_KEY_SIZE);
                pk.nonce = Arrays.copyOfRange(buffer, PASSWORD_KEY_SIZE, PASSWORD_KEY_SIZE + PASSWORD_NONCE_SIZE);
                return pk;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private KeyPair generateKeyPairImpl(String alias, String authType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        if (authType == null)
            authType = WalletData.AUTH_TYPE_NONE;

        try {

            KeyGenParameterSpec.Builder params = new KeyGenParameterSpec.Builder(
                    WalletData.AUTH_TYPE_PASSWORD.equals(authType)
                    ? alias + PASSWORD_ALIAS_SUFFIX
                    : alias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256);

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
                    break;

                default:
                    throw new RuntimeException("Unknown auth type");
            }

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(params.build());

            KeyPair kp = kpg.generateKeyPair();
            return kp;
        } catch (Exception e) {
            Log.e(TAG, "generate key pair error "+e);
        }

        return null;
    }

    private PublicKey getUnrestrictedPubkey(PublicKey pk) {
        try {
            // A known bug in Android 6.0 (API Level 23) causes user authentication-related
            // authorizations to be enforced even for public keys. To work around this issue extract
            // the public key material to use outside of Android Keystore. For example:
            return KeyFactory.getInstance(pk.getAlgorithm()).generatePublic(
                    new X509EncodedKeySpec(pk.getEncoded()));
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] pubkeyToNonce(PublicKey pk) {
        try {
            MessageDigest digest = null;
            digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            return digest.digest(pk.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* authType=password:
    generate:
    - keypair is generated w/ special alias suffix
    - pubkey converted to nonce using sha-256
    - pbkdf turns password+nonce into a hash
    - hash written to a file
    get_signer;
    - read alias password file from disk, if any
    - if alias password exists, get key from suffixed alias, otherwise use plain alias
    - after key is read from keystore, when password is available
    - do pbkdf using pubkey as nonce and password to get the hash
    - compare hashes, if ok - proceed
    security:
    - if pubkey is invalidated due to keystore policy, password is useless
    - if password file deleted, no key will be retrieved from keystore using plain alias
    - if key is regenerated in keystore, password key won't match bcs pubkey is nonce
    - if key is regenerated and password file deleted then pubkey won't match the one in user db
    * */
    @Override
    public String generateKeyPair(String alias, String authType, String password) {
        try {
            KeyPair kp = generateKeyPairImpl(alias, authType);

            PublicKey unrestrictedPublicKey = getUnrestrictedPubkey (kp.getPublic());
            Log.i(TAG, "new keypair alias "+alias+" pubkey "+HEX.fromBytes(unrestrictedPublicKey.getEncoded()));

            if (WalletData.AUTH_TYPE_PASSWORD.equals(authType)) {
                PasswordKey pk = new PasswordKey();
                pk.nonce = pubkeyToNonce(unrestrictedPublicKey);
                pk.key = generatePasswordKey(pk.nonce, password);
                writePasswordKey(alias, pk);
            } else {
                File file = new File(getPasswordFile(alias));
                file.delete();
            }

            // FIXME maybe use this instead?
            // https://stackoverflow.com/questions/40155888/how-can-i-generate-a-valid-ecdsa-ec-key-pair
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

        final PasswordKey pk = readPasswordKey(alias);

        KeyStore.PrivateKeyEntry key = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            final String keyAlias = pk != null ? alias + PASSWORD_ALIAS_SUFFIX : alias;

            if (!ks.containsAlias(keyAlias))
                return null;

            key = ((KeyStore.PrivateKeyEntry) ks.getEntry(keyAlias, null));

        } catch (Exception e) {
            Log.e(TAG, "getKeySigner error "+e);
            return null;
        }

        if (key == null)
            return null;

        PublicKey unrestrictedPubkey = getUnrestrictedPubkey (key.getCertificate().getPublicKey());
        String pubkey = HEX.fromBytes(unrestrictedPubkey.getEncoded());

        if (pk != null) {
            final byte[] nonce = pubkeyToNonce(unrestrictedPubkey);

            // key regenerated, password file not updated!
            if (!Arrays.equals(nonce, pk.nonce))
                return null;

            return DefaultSigner.createAuthPassword(key.getPrivateKey(), pubkey, new DefaultSigner.IPasswordVerifier() {
                @Override
                public boolean verify(String password) {
                    byte[] reqKey = generatePasswordKey(nonce, password);
                    return Arrays.equals(reqKey, pk.key);
                }
            });
        } else {
            return DefaultSigner.create(key.getPrivateKey(), pubkey);
        }
    }

}
