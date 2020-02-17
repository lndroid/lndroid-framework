package org.lndroid.framework.defaults;

import android.os.Build;
import android.security.keystore.UserNotAuthenticatedException;

import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;

import org.lndroid.framework.common.HEX;
import org.lndroid.framework.common.ISigner;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class DefaultSigner implements ISigner {
    private PrivateKey privateKey_;
    private String publicKey_;
    private IPasswordVerifier passwordVerifier_;
    private String password_;
    private BiometricPrompt.CryptoObject cryptoObject_;

    public interface IPasswordVerifier {
        boolean verify(String password);
    }

    private DefaultSigner(PrivateKey priv, String pub, IPasswordVerifier passwordVerifier) {
        privateKey_ = priv;
        publicKey_ = pub;
        passwordVerifier_ = passwordVerifier;
    }

    public static DefaultSigner create(PrivateKey priv, String pub) {
        return new DefaultSigner(priv, pub, null);
    }

    public static DefaultSigner createAuthPassword(
            PrivateKey priv, String pub, IPasswordVerifier passwordVerifier) {
        return new DefaultSigner(priv, pub, passwordVerifier);
    }

    @Override
    public String getPublicKey() {
        return publicKey_;
    }

    @Override
    public void setAuthObject(Object o) {
        if (o == null)
            throw new RuntimeException("Auth object is null");

        if (passwordVerifier_ != null) {
            if (!(o instanceof String))
                throw new RuntimeException("Wrong auth object type");
            password_ = (String) o;
        } else {
            if (!(o instanceof BiometricPrompt.CryptoObject))
                throw new RuntimeException("Wrong auth object type");
            cryptoObject_ = (BiometricPrompt.CryptoObject) o;
        }
    }

    @Override
    public Object getAuthObject() {
        if (passwordVerifier_ != null)
            return null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(privateKey_);
            return new BiometricPrompt.CryptoObject(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sign(byte[] data, Signature s) throws SignatureException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        s.update(data);
        byte[] signature = s.sign();

        return HEX.fromBytes(signature);
    }

    @Override
    public String sign(byte[] data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        try {
            if (cryptoObject_ != null) {
                // bio authed signature
                return sign(data, cryptoObject_.getSignature());
            }

            if (passwordVerifier_ != null) {
                if (password_ == null)
                    return null;

                if (!passwordVerifier_.verify(password_))
                    return null;

                // reset
                password_ = null;
            }

            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(privateKey_);
            return sign(data, s);
        } catch (SignatureException e) {
            return null;
        } catch (UserNotAuthenticatedException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
