package org.lndroid.framework.common;

public interface IVerifier {
    boolean verify(byte[] payload, String pubkey, String signature);
}
