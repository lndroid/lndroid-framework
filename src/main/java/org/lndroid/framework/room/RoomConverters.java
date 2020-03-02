package org.lndroid.framework.room;

import androidx.room.TypeConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.plugins.Transaction;

final class RoomConverters {

    static class ImmutableStringListConverter {

        @TypeConverter
        public static String fromStringList(List<String> list) {
            if (list == null)
                return null;

            StringBuffer b = new StringBuffer();
            for (String line : list) {
                if (line.contains("\n"))
                    throw new RuntimeException("String list with multi-line entries not supported");
                b.append(line);
                b.append("\n");
            }
            return b.toString();
        }

        @TypeConverter
        public static ImmutableList<String> toImmutableStringList(String lines) {
            if (lines == null)
                return null;

            ImmutableList.Builder<String> b = ImmutableList.builder();
            for (String line : lines.split("\\r?\\n")) {
                if (!line.isEmpty())
                    b.add(line);
            }
            return b.build();
        }
    }

    static class ImmutableIntListConverter {

        @TypeConverter
        public static String fromIntList(List<Integer> list) {
            if (list == null)
                return null;

            StringBuffer b = new StringBuffer();
            for (Integer v: list) {
                b.append(v.toString());
                b.append("\n");
            }
            return b.toString();
        }

        @TypeConverter
        public static ImmutableList<Integer> toImmutableIntList(String lines) {
            if (lines == null)
                return null;

            ImmutableList.Builder<Integer> b = ImmutableList.builder();
            for (String line : lines.split("\\r?\\n")) {
                if (!line.isEmpty())
                    b.add(Integer.valueOf(line));
            }
            return b.build();
        }
    }

    static class ImmutableLongListConverter {

        @TypeConverter
        public static String fromIntList(List<Long> list) {
            if (list == null)
                return null;

            StringBuffer b = new StringBuffer();
            for (Long v: list) {
                b.append(v.toString());
                b.append("\n");
            }
            return b.toString();
        }

        @TypeConverter
        public static ImmutableList<Long> toImmutableLongList(String lines) {
            if (lines == null)
                return null;

            ImmutableList.Builder<Long> b = ImmutableList.builder();
            for (String line : lines.split("\\r?\\n")) {
                if (!line.isEmpty())
                    b.add(Long.valueOf(line));
            }
            return b.build();
        }
    }

    static class ImmutableStringLongMapConverter {

        @TypeConverter
        public static String fromStringList(Map<String, Long> map) {
            if (map == null)
                return null;

            StringBuffer b = new StringBuffer();
            for (Map.Entry<String, Long> e: map.entrySet()) {
                if (e.getKey().contains("\n"))
                    throw new RuntimeException("String list with multi-line entries not supported");
                b.append(e.getKey());
                b.append("\n");
                b.append(e.getValue().toString());
                b.append("\n");
            }
            return b.toString();
        }

        @TypeConverter
        public static ImmutableMap<String,Long> toImmutableStringLongMap(String lines) {
            if (lines == null)
                return null;

            ImmutableMap.Builder<String, Long> b = ImmutableMap.builder();
            String[] list = lines.split("\\r?\\n");
            for (int i = 0; i < list.length - 1; i += 2) {
                String key = list[i];
                Long value = Long.parseLong(list[i+1]);
                b.put(key, value);
            }
            return b.build();
        }
    }

    static class DestTLVConverter {

        private static void writeLong(long l, ByteArrayOutputStream bytes) {
            bytes.write((byte)((l >> 56) & 0xFF));
            bytes.write((byte)((l >> 48) & 0xFF));
            bytes.write((byte)((l >> 40) & 0xFF));
            bytes.write((byte)((l >> 32) & 0xFF));
            bytes.write((byte)((l >> 24) & 0xFF));
            bytes.write((byte)((l >> 16) & 0xFF));
            bytes.write((byte)((l >>  8) & 0xFF));
            bytes.write((byte)((l      ) & 0xFF));
        }

        private static boolean readLong(byte[] bytes, int o, Long lo) {
            if (8 > (bytes.length - o))
                return false;

            long l = 0;
            l += (bytes[o + 0] << 56) & 0xFF00000000000000L;
            l += (bytes[o + 1] << 48) & 0x00FF000000000000L;
            l += (bytes[o + 2] << 40) & 0x0000FF0000000000L;
            l += (bytes[o + 3] << 32) & 0x000000FF00000000L;
            l += (bytes[o + 4] << 24) & 0x00000000FF000000L;
            l += (bytes[o + 5] << 16) & 0x0000000000FF0000L;
            l += (bytes[o + 6] << 8 ) & 0x000000000000FF00L;
            l += (bytes[o + 7]      ) & 0x00000000000000FFL;

            lo = l;
            return true;
        }

        @TypeConverter
        public static byte[] fromDestTLV(ImmutableMap<Long,byte[]> map) {
            if (map == null)
                return null;

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            for(Map.Entry<Long,byte[]> e: map.entrySet()) {
                writeLong(e.getKey(), bytes);
                if (e.getValue() != null) {
                    writeLong(e.getValue().length, bytes);
                    try {
                        bytes.write(e.getValue());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    writeLong(0L, bytes);
                }
            }
            return bytes.toByteArray();
        }

        @TypeConverter
        public static ImmutableMap<Long,byte[]> toDestTLV(byte[] d) {
            if (d == null)
                return null;

            ImmutableMap.Builder<Long,byte[]> b = ImmutableMap.builder();
            int o = 0;
            Long l = new Long(0);
            while(o < d.length) {
                if (!readLong(d, o, l))
                    break;
                o += 8;

                final long key = l;
                if (!readLong(d, o, l))
                    break;
                o += 8;

                final long len = l;
                if (len > (d.length - o))
                    break;

                byte[] value = null;
                if (len > 0) {
                    value = new byte[(int)len];
                    System.arraycopy(d, o, value, 0, (int)len);
                }
                o += len;

                b.put(key, value);
            }
            return b.build();
        }
    }

    static class PaymentConverter {

        @TypeConverter
        public static String fromSendPayments(ImmutableMap<Long,WalletData.SendPayment> p) {
            return null;
        }

        @TypeConverter
        public static ImmutableMap<Long,WalletData.SendPayment> toSendPayments(String o) {
            return null;
        }

        @TypeConverter
        public static String fromHTLCAttempts(ImmutableMap<Long,WalletData.HTLCAttempt> p) {
            return null;
        }

        @TypeConverter
        public static ImmutableMap<Long,WalletData.HTLCAttempt> toHTLCAttempts(String o) {
            return null;
        }

        @TypeConverter
        public static String fromInvoices(ImmutableMap<Long,WalletData.Invoice> i) {
            return null;
        }

        @TypeConverter
        public static ImmutableMap<Long,WalletData.Invoice> toInvoices(String o) {
            return null;
        }

        @TypeConverter
        public static String fromInvoiceHTLCs(ImmutableMap<Long,WalletData.InvoiceHTLC> i) {
            return null;
        }

        @TypeConverter
        public static ImmutableMap<Long,WalletData.InvoiceHTLC> toInvoiceHTLCs(String o) {
            return null;
        }
    }

    static class TransientHopHintsConverter {

        @TypeConverter
        public static String fromHopHints(ImmutableList<WalletData.HopHint> hints) {
            return null;
        }

        @TypeConverter
        public static ImmutableList<WalletData.HopHint> toHopHints(String o) {
            return null;
        }
    }

    static class TransientRouteHintsConverter {

        @TypeConverter
        public static String fromRouteHints(ImmutableList<WalletData.RouteHint> hints) {
            return null;
        }

        @TypeConverter
        public static ImmutableList<WalletData.RouteHint> toRouteHints(String o) {
            return null;
        }
    }

    static class TransientRoutingPolicyConverter {

        @TypeConverter
        public static String fromRoutingPolicy(WalletData.RoutingPolicy p) {
            return null;
        }

        @TypeConverter
        public static WalletData.RoutingPolicy toRoutingPolicy(String o) {
            return null;
        }
    }

}
