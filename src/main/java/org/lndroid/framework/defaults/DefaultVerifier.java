package org.lndroid.framework.defaults;

import org.lndroid.framework.common.HEX;
import org.lndroid.framework.common.IVerifier;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class DefaultVerifier implements IVerifier {
    @Override
    public boolean verify(byte[] payload, String pubkey, String signature) {
        try {
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(HEX.toBytes(pubkey));
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey pub = kf.generatePublic(publicKeySpec);
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(pub);
            s.update(payload);
            return s.verify(HEX.toBytes(signature));
        } catch (NoSuchAlgorithmException e) {
            // EC etc must be supported
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            // invalid key spec
            return false;
        } catch (InvalidKeyException e) {
            // invalid key
            return false;
        } catch (SignatureException e) {
            // invalid signature
            return false;
        }
    }
}