package org.lndroid.framework.common;

import android.os.Bundle;

public class PluginUtils {

    public static final String USER_KEY_ALIAS_PREFIX = "uk_";
    public static String userKeyAlias(long userId) {
        return USER_KEY_ALIAS_PREFIX+userId;
    }

    public static String checkPluginMessageIpc(
            Bundle b, String pubkey, IVerifier verifier) {
        if (b == null || pubkey == null)
            return Errors.PLUGIN_INPUT;

        byte[] payload = b.getByteArray(PluginData.IPC_MESSAGE);
        String signature = b.getString(PluginData.IPC_SIGNATURE);
        if (payload == null || signature == null)
            return Errors.MESSAGE_FORMAT;

        if (!verifier.verify(payload, pubkey, signature))
            return Errors.MESSAGE_AUTH;

        return null;
    }

    public static PluginData.PluginMessage decodePluginMessageIpc(Bundle b, ICodec<PluginData.PluginMessage> codec) {
        if (b == null)
            return null;

        String version = b.getString(PluginData.IPC_VERSION);
        if (!PluginData.IPC_CURRENT_VERSION.equals(version))
            return null;

        byte[] payload = b.getByteArray(PluginData.IPC_MESSAGE);
        if (payload == null)
            return null;

        return codec.decode(payload);
    }

    public static Bundle encodePluginMessageIpc(
            PluginData.PluginMessage msg,
            ICodecProvider codecProvider,
            ICodec<PluginData.PluginMessage> pluginMessageCodec,
            ISigner signer
    ) {
        // encode message data
        msg.assignCodecProvider(codecProvider);
        msg.encodeData();

        // encode message itself
        byte[] payload = pluginMessageCodec.encode(msg);

        // set to bundle
        Bundle b = new Bundle();
        b.putString(PluginData.IPC_VERSION, PluginData.IPC_CURRENT_VERSION);
        b.putByteArray(PluginData.IPC_MESSAGE, payload);

        final String signature = signer.sign(payload);
        // FIXME what if sign==null?

        b.putString(PluginData.IPC_SIGNATURE, signature);

        return b;
    }
}
