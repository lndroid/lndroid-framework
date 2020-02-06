package org.lndroid.framework.common;

import java.lang.reflect.Type;

public interface ICodecProvider {
    <T> ICodec<T> get(Type type);
}
