package org.lndroid.framework.common;

public interface ICodec<T> {
    byte[] encode(T value);
    T decode(byte[] d);
}