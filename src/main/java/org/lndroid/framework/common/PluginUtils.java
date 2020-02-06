package org.lndroid.framework.common;

import android.os.Bundle;

public class PluginUtils {

    public static PluginData.PluginMessage decodePluginMessageIpc(Bundle b, ICodec<PluginData.PluginMessage> codec) {
        if (b == null)
            return null;

        // FIXME check version, timestamp, signature
        byte[] payload = b.getByteArray(PluginData.IPC_MESSAGE);
        if (payload == null)
            return null;

        return codec.decode(payload);
    }

    public static Bundle encodePluginMessageIpc(
            PluginData.PluginMessage msg,
            ICodecProvider codecProvider,
            ICodec<PluginData.PluginMessage> pluginMessageCodec
    ) {
        // encode message data
        msg.assignCodecProvider(codecProvider);
        msg.encodeData();

        // encode message itself
        byte[] payload = pluginMessageCodec.encode(msg);

        // FIXME sign using signer

        // set to bundle
        Bundle b = new Bundle();
        b.putString(PluginData.IPC_VERSION, PluginData.IPC_CURRENT_VERSION);
        b.putByteArray(PluginData.IPC_MESSAGE, payload);
        b.putLong(PluginData.IPC_TIMESTAMP, System.currentTimeMillis());
        //FIXME signature and appPubkey

        return b;
    }
}
