package org.lndroid.framework.defaults;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.FieldInfo;
import org.lndroid.framework.common.IFieldMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DefaultFieldMapper<T> implements IFieldMapper<T> {

    public DefaultFieldMapper() {
    }

    @Override
    public List<WalletData.Field> mapToFields(T t) {
        List<WalletData.Field> fields = new ArrayList<>();
        for(Method m: t.getClass().getMethods()) {
            FieldInfo fi = m.getAnnotation(FieldInfo.class);
            if (fi == null)
                continue;

            Object v = null;
            try {
                v = m.invoke(t);
            } catch (IllegalAccessException e) {
                // ignore?
            } catch (InvocationTargetException e) {
                // ignore?
            }

            Class<?> r = m.getReturnType();

            if (fi.convertors().length != 0) {
                Class<?> c = fi.convertors()[0];
                Method convert = null;
                Method toString = null;
                try {
                    convert = c.getMethod("convert", r, FieldInfo.class);
                } catch(NoSuchMethodException e) {
                    try {
                        toString = c.getMethod("toString", r);
                    } catch(NoSuchMethodException e1) {
                    }
                }

                try {
                    Object convertor = fi.convertors()[0].newInstance();
                    if (convert != null) {
                        List<WalletData.Field> subFields = (List<WalletData.Field>)convert.invoke(convertor, v, fi);
                        fields.addAll(subFields);
                    } else if (toString != null) {
                        String value = (String)toString.invoke(convertor, v);
                        fields.add(WalletData.Field.builder()
                                .setName(fi.name())
                                .setValue(value)
                                .setHelp(fi.help())
                                .setId(m.getName())
                                .build());
                    } else {
                        throw new RuntimeException("Field convertor has no proper method");
                    }
                } catch (IllegalAccessException e) {
                    // ignore?
                } catch (InvocationTargetException e) {
                    // ignore?
                } catch (InstantiationException e) {
                    // ignore?
                }

            } else {

                String value = "";
                if (v != null) {
                    if (Long.class.equals(r) || long.class.equals(r))
                        value = Long.toString((long) v);
                    else if (Integer.class.equals(r) || int.class.equals(r))
                        value = Integer.toString((int) v);
                    else if (Boolean.class.equals(r) || boolean.class.equals(r))
                        value = Boolean.toString((boolean) v);
                    else
                        value = v.toString();
                }

                fields.add(WalletData.Field.builder()
                        .setName(fi.name())
                        .setValue(value)
                        .setHelp(fi.help())
                        .setId(m.getName())
                        .build());
            }
        }

        return fields;
    }

    public static class ImmutableIntListConverter {

        public String toString(ImmutableList<Integer> list) {
            StringBuilder b = new StringBuilder();
            for(int i: list) {
                if (b.length() > 0)
                    b.append(", ");
                b.append(i);
            }

            return b.toString();
        }
    }

    public static class PeerSyncTypeConverter {

        public String toString(int s) {
            if (s == WalletData.PEER_SYNC_TYPE_ACTIVE)
                return "active";
            else if (s == WalletData.PEER_SYNC_TYPE_PASSIVE)
                return "passive";
            else
                return "unknown";
        }
    }

    public static class DateTimeMsConverter {
        private DateFormat dateFormat_;

        public DateTimeMsConverter() {
            dateFormat_ = DateFormat.getDateTimeInstance(
                    DateFormat.LONG, DateFormat.FULL, new Locale("en", "US"));
        }

        public String toString(long ms) {
            return ms != 0 ? dateFormat_.format(new Date(ms)) : "";
        }
    }

    public static class TransactionConvertor {
        public List<WalletData.Field> convert(ImmutableMap<String, Long> addrToAmount, FieldInfo fi) {
            List<WalletData.Field> list = new ArrayList<>();
            int i = 1;
            for(Map.Entry<String, Long> e: addrToAmount.entrySet()) {
                list.add(WalletData.Field.builder()
                        .setId("addrToAmount_Addr_"+i)
                        .setName("Address #"+i)
                        .setValue(e.getKey())
                        .setHelp("On-chain address")
                        .build()
                );
                list.add(WalletData.Field.builder()
                        .setId("addrToAmount_Amount_"+i)
                        .setName("Amount #"+i+", sat")
                        .setValue(e.getValue().toString())
                        .setHelp("Value send to the address in sats")
                        .build()
                );
                i++;
            }

            return list;
        }

        public List<WalletData.Field> convert(ImmutableList<String> addresses, FieldInfo fi) {
            List<WalletData.Field> list = new ArrayList<>();
            int i = 1;
            for(String a: addresses) {
                list.add(WalletData.Field.builder()
                        .setId("destAddresses_"+i)
                        .setName("Dest address #"+i)
                        .setValue(a)
                        .setHelp("Addresses that received funds for this transaction")
                        .build()
                );
                i++;
            }

            return list;
        }

    }
}
