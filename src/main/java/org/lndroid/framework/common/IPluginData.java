package org.lndroid.framework.common;

import java.io.IOException;
import java.lang.reflect.Type;

public interface IPluginData {
    void assignCodecProvider(ICodecProvider cp);
    void assignDataType(Type type);
    <T> T getData() throws IOException;
}
