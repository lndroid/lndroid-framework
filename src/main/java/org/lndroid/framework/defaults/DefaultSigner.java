package org.lndroid.framework.defaults;

import org.lndroid.framework.common.HEX;
import org.lndroid.framework.common.ISigner;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class DefaultSigner implements ISigner {
    private PrivateKey privateKey_;
    private PublicKey publicKey_;

    public DefaultSigner(PrivateKey priv, PublicKey pub) {
        privateKey_ = priv;
        publicKey_ = pub;
    }

    @Override
    public String getPublicKey() {
        return HEX.fromBytes(publicKey_.getEncoded());
    }

    @Override
    public String sign(byte[] data) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(privateKey_);
            s.update(data);
            byte[] signature = s.sign();
            return HEX.fromBytes(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
