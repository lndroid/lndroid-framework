package org.lndroid.framework.engine;

import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IVerifier;
import org.lndroid.framework.common.PluginData;

public class PluginUtilsLocal {

    // FIXME move this stuff to PluginData, make SessionToken a better 'class' not half-POJO
    public static class SessionToken {
        long timestamp;
        long duration;
        String plugin;

        public byte[] payload;
        public String signature;

        static SessionToken parse(String str) {
            SessionToken st = new SessionToken();
            String[] sig_payload = str.split(":");
            if (sig_payload.length != 2)
                return null;

            st.signature = sig_payload[0];
            try {
                st.payload = sig_payload[1].getBytes("UTF-8");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String[] fields = sig_payload[1].split("_");
            if (fields.length != 3)
                return null;

            st.timestamp = Long.parseLong(fields[0]);
            st.duration = Long.parseLong(fields[1]);
            st.plugin = fields[2];
            if (st.plugin.isEmpty() || st.plugin.equals("*"))
                st.plugin = null;

            return st;
        }

        private String formatPayload() {
            return "" + timestamp + "_" + duration + "_" +
                    (plugin != null ? plugin : "*");
        }

        public byte[] preparePayload() {
            String s = formatPayload();
            try {
                return s.getBytes("UTF-8");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String formatToken() {
            return signature+":"+formatPayload();
        }
    };

    public static SessionToken prepareSessionToken(long duration, String plugin) {
        SessionToken st = new SessionToken();
        st.timestamp = System.currentTimeMillis();
        st.duration = duration;
        st.plugin = plugin;
        st.payload = st.preparePayload();
        return st;
    }

    public static String checkPluginMessageLocal(
            PluginData.PluginMessage pm, String pubkey, IVerifier verifier) {
        if (pm.sessionToken() == null)
            return Errors.MESSAGE_FORMAT;

        // parse token
        SessionToken st = SessionToken.parse(pm.sessionToken());
        if (st == null)
            return Errors.MESSAGE_FORMAT;

        final long now = System.currentTimeMillis();
        if (st.timestamp > now || st.timestamp < (now - st.duration))
            return Errors.MESSAGE_AUTH;

        if (st.plugin != null && !st.plugin.equals(pm.pluginId()))
            return Errors.MESSAGE_AUTH;

        if (!verifier.verify(st.payload, pubkey, st.signature))
            return Errors.MESSAGE_AUTH;

        return null;
    }


}
