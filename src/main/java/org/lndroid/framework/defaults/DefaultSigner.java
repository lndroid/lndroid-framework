package org.lndroid.framework.defaults;

import org.lndroid.framework.common.HEX;
import org.lndroid.framework.common.ISigner;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class DefaultSigner implements ISigner {
    private PrivateKey privateKey_;
    private String publicKey_;
    private String alias_;

    private DefaultSigner(PrivateKey priv, String pub, String alias) {
        privateKey_ = priv;
        publicKey_ = pub;
        alias_ = alias;
    }

    public static DefaultSigner create(PrivateKey priv, String pub) {
        return new DefaultSigner(priv, pub, null);
    }

    public static DefaultSigner createForPasswordKey(PrivateKey priv, String pub, String alias) {

        return new DefaultSigner(priv, pub, alias);
    }

    @Override
    public String getPublicKey() {
        return publicKey_;
    }

    @Override
    public String sign(byte[] data) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(privateKey_);
            s.update(data);
            byte[] signature = s.sign();

            if (alias_ != null) {
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                ks.deleteEntry(alias_);
                alias_ = null;
            }

            return HEX.fromBytes(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
