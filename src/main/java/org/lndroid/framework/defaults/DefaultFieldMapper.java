package org.lndroid.framework.defaults;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.FieldInfo;
import org.lndroid.framework.common.IFieldConvertor;
import org.lndroid.framework.common.IFieldMapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DefaultFieldMapper<T> implements IFieldMapper<T> {

    public DefaultFieldMapper() {
    }

    @Override
    public List<WalletData.Field> mapToFields(T t) {
        List<WalletData.Field> fields = new ArrayList<>();
        for(Method m: t.getClass().getMethods()){
            FieldInfo fi = m.getAnnotation(FieldInfo.class);
            if (fi == null)
                continue;

            String value = "";
            Class<?> c = m.getReturnType();
            try {
                Object v = m.invoke(t);
                if (v != null) {
                    if (fi.convertors().length != 0)
                        value = fi.convertors()[0].newInstance().toString(v);
                    else if (Long.class.equals(c) || long.class.equals(c))
                        value = Long.toString((long) v);
                    else if (Integer.class.equals(c) || int.class.equals(c))
                        value = Integer.toString((int) v);
                    else if (Boolean.class.equals(c) || boolean.class.equals(c))
                        value = Boolean.toString((boolean) v);
                    else
                        value = v.toString();
                }
            } catch (IllegalAccessException e) {
                // ignore?
            } catch (InvocationTargetException e) {
                // ignore?
            } catch (InstantiationException e) {
                // ignore?
            }

            fields.add(WalletData.Field.builder()
                    .setName(fi.name())
                    .setValue(value)
                    .setHelp(fi.help())
                    .setId(m.getName())
                    .build());
        }

        return fields;
    }

    public static class ImmutableIntListConverter implements IFieldConvertor {

        @Override
        public String toString(Object o) {
            ImmutableList<Integer> list = (ImmutableList<Integer>)o;
            StringBuilder b = new StringBuilder();
            for(int i: list) {
                if (b.length() > 0)
                    b.append(", ");
                b.append(i);
            }

            return b.toString();
        }
    }

    public static class PeerSyncTypeConverter implements IFieldConvertor {

        @Override
        public String toString(Object o) {
            int s = (int)o;
            if (s == WalletData.PEER_SYNC_TYPE_ACTIVE)
                return "active";
            else if (s == WalletData.PEER_SYNC_TYPE_PASSIVE)
                return "passive";
            else
                return "unknown";
        }
    }

    public static class DateTimeMsConverter implements IFieldConvertor {
        private DateFormat dateFormat_;

        public DateTimeMsConverter() {
            dateFormat_ = DateFormat.getDateTimeInstance(
                    DateFormat.LONG, DateFormat.FULL, new Locale("en", "US"));
        }

        @Override
        public String toString(Object v) {
            long ms = (long)v;
            return ms != 0 ? dateFormat_.format(new Date(ms)) : "";
        }
    }
}
