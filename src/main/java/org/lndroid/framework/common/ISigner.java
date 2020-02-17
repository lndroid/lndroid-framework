package org.lndroid.framework.common;

public interface ISigner {
    String getPublicKey();

    // returns object that must be used for auth flow
    Object getAuthObject();

    // accepts authorized object after auth flow
    void setAuthObject(Object o);

    // try to sign, returns null if auth needed
    String sign(byte[] data);
}
