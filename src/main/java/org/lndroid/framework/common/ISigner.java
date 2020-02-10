package org.lndroid.framework.common;

public interface ISigner {
    String getPublicKey();
    String sign(byte[] data);
}
