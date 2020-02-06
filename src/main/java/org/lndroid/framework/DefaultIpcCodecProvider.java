package org.lndroid.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lndroid.framework.common.AutoValueClass;
import org.lndroid.framework.common.Transient;
import org.lndroid.framework.common.ICodec;
import org.lndroid.framework.common.ICodecProvider;

class CustomizedTypeAdapterFactory implements TypeAdapterFactory {

    private static final String CLASS_NAME_PROPERTY = "className_";

    public CustomizedTypeAdapterFactory() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();

        AutoValueClass annotation = rawType.getAnnotation(AutoValueClass.class);

        // return adapter for AutoValue implementation if @AutoValueClass is present
        if (annotation != null)
            return customizeMyClassAdapter(gson, TypeToken.get(annotation.className()));
        else
            return customizeMyClassAdapter(gson, type);
    }

    private <T> void addMetadata(T source, JsonElement json) {
        if (!json.isJsonObject())
            return;

        // Bad idea: letting clients specify a class that
        // would be auto-instanciated on the server is a security hole!
//        JsonObject self = json.getAsJsonObject();
//        self.addProperty(CLASS_NAME_PROPERTY, source.getClass().getName());
    }

/*    private static <T> Class<?> getObjectClass(Class<T> cls, JsonElement json) throws IOException {
        // T is the type expected by Gson, in case it's an Object or some other
        // abstract type then we'd like to read the metadata and
        // choose proper delegate, different than for T

        if (!json.isJsonObject())
            return null;

        JsonObject self = json.getAsJsonObject();
        JsonElement classNameElement = self.get(CLASS_NAME_PROPERTY);
        if (classNameElement == null || !classNameElement.isJsonPrimitive())
            return null;

        String className = classNameElement.getAsJsonPrimitive().getAsString();
        try {
            return cls.getClassLoader().loadClass(className);
        } catch(ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

 */

    private <T> TypeAdapter<T> customizeMyClassAdapter(final Gson gson, final TypeToken<T> type) {

        return new TypeAdapter<T>() {
            @Override public void write(JsonWriter out, T value) throws IOException {
                // when serializing, we use default adapter to serialize first,
                // and then simply add our metadata like class name and version.

                // serialize using default adapter
                TypeAdapter<T> delegate = gson.getDelegateAdapter(CustomizedTypeAdapterFactory.this, type);
                JsonElement tree = delegate.toJsonTree(value);

                // add our metadata
                addMetadata(value, tree);

                // write
                TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
                elementAdapter.write(out, tree);
            }

            @Override public T read(JsonReader in) throws IOException {
                // when reading, we have to read metadata first and then
                // use it to choose proper delegate adapter

                // parse the stream into json elements
                TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
                JsonElement tree = elementAdapter.read(in);

//                Class<?> objectClass = getObjectClass(type.getClass(), tree);

                if (type.getRawType() == ImmutableList.class) {

                    // from https://stackoverflow.com/a/21805758
                    final com.google.common.reflect.TypeToken<ImmutableList<?>> immutableListToken =
                            (com.google.common.reflect.TypeToken<ImmutableList<?>>) com.google.common.reflect.TypeToken.of(type.getType());
                    final com.google.common.reflect.TypeToken<? super ImmutableList<?>> listToken =
                            immutableListToken.getSupertype(List.class);
                    TypeAdapter<?> delegate = gson.getDelegateAdapter(CustomizedTypeAdapterFactory.this, TypeToken.get(listToken.getType()));

                    // hope that objectClass is compatible
                    // with T (which will hopefully be an Object)
                    return (T) ImmutableList.copyOf((List<?>) delegate.fromJsonTree(tree));
                } else if (type.getRawType() == ImmutableMap.class) {

                        final com.google.common.reflect.TypeToken<ImmutableMap<?,?>> immutableMapToken =
                                (com.google.common.reflect.TypeToken<ImmutableMap<?,?>>)com.google.common.reflect.TypeToken.of(type.getType());
                        final com.google.common.reflect.TypeToken<? super ImmutableMap<?,?>> mapToken =
                                immutableMapToken.getSupertype(Map.class);
                        TypeAdapter<?> delegate = gson.getDelegateAdapter(CustomizedTypeAdapterFactory.this, TypeToken.get(mapToken.getType()));

                        // hope that objectClass is compatible
                        // with T (which will hopefully be an Object)
                        return (T)ImmutableMap.copyOf((Map<?,?>)delegate.fromJsonTree(tree));

                } else {
//                Class<?> objectClass = getObjectClass(type.getClass(), tree);

//                if (objectClass != null)
//                    delegate = gson.getDelegateAdapter(CustomizedTypeAdapterFactory.this, TypeToken.get(objectClass));
//                else
                    TypeAdapter<?> delegate = gson.getDelegateAdapter(CustomizedTypeAdapterFactory.this, type);

                    // hope that objectClass is compatible
                    // with T (which will hopefully be an Object)
                    return (T)delegate.fromJsonTree(tree);
                }
            }
        };

    }
}

public class DefaultIpcCodecProvider implements ICodecProvider {

    private static final String TAG = "DefaultIpcCodecFactory";

    private Gson gson_;
    private Map<Type, ICodec> codecs_ = new HashMap<>();

    static class GsonCodec<T> implements ICodec<T> {
        private Type type_;
        private Gson gson_;

        GsonCodec(Type type, Gson gson) {
            type_ = type;
            gson_ = gson;
        }

        @Override
        public byte[] encode(T value) {
            return gson_.toJson(value).getBytes();
        }

        @Override
        public T decode(byte[] d) {
            return gson_.fromJson(new String(d), type_);
        }
    }

    class TransientExclusionStrategy implements ExclusionStrategy {

        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(Transient.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    public DefaultIpcCodecProvider() {
        gson_ = new GsonBuilder()
                .registerTypeAdapterFactory(new CustomizedTypeAdapterFactory())
                .setExclusionStrategies(new TransientExclusionStrategy())
                .create();
    }

    @Override
    public <T> ICodec<T> get(Type type) {
        ICodec<T> c = (ICodec<T>)codecs_.get(type);
        if (c == null) {
            c = new GsonCodec<>(type, gson_);
            codecs_.put(type, c);
        }
        return c;
    }

}
