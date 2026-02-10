package com.crystalrealm.ecotalequests.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds BuilderCodec instances entirely through reflection to avoid
 * NoSuchMethodError / NoSuchFieldError caused by stub descriptor mismatches.
 *
 * <p>Pattern (what we replicate):</p>
 * <pre>
 * BuilderCodec.&lt;Data&gt;builder(Data.class, Data::new)
 *     .addField(new KeyedCodec&lt;&gt;("Button", Codec.STRING), setter, getter)
 *     .addField(...)
 *     .build();
 * </pre>
 */
@SuppressWarnings("unchecked")
final class ReflectiveCodecBuilder<T> {

    private static final Class<?> CODEC_CLASS;
    private static final Class<?> KEYED_CODEC_CLASS;
    private static final Class<?> BUILDER_CODEC_CLASS;
    private static final Object   CODEC_STRING;

    static {
        try {
            CODEC_CLASS = Class.forName("com.hypixel.hytale.codec.Codec");
            KEYED_CODEC_CLASS = Class.forName("com.hypixel.hytale.codec.KeyedCodec");
            BUILDER_CODEC_CLASS = Class.forName("com.hypixel.hytale.codec.builder.BuilderCodec");

            // Get Codec.STRING via reflection to avoid field descriptor mismatch
            Field stringField = CODEC_CLASS.getField("STRING");
            CODEC_STRING = stringField.get(null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Object builder; // BuilderCodec$Builder instance

    private ReflectiveCodecBuilder(Object builder) {
        this.builder = builder;
    }

    /**
     * Start building a BuilderCodec for the given data class.
     */
    static <T> ReflectiveCodecBuilder<T> create(Class<T> dataClass, Supplier<T> constructor) {
        try {
            Method builderMethod = BUILDER_CODEC_CLASS.getMethod("builder", Class.class, Supplier.class);
            Object b = builderMethod.invoke(null, dataClass, constructor);
            return new ReflectiveCodecBuilder<>(b);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create BuilderCodec.builder()", e);
        }
    }

    /**
     * Create a KeyedCodec("key", Codec.STRING) via reflection.
     */
    private static Object newKeyedCodec(String key) {
        try {
            var ctor = KEYED_CODEC_CLASS.getConstructor(String.class, CODEC_CLASS);
            return ctor.newInstance(key, CODEC_STRING);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create KeyedCodec(\"" + key + "\", Codec.STRING)", e);
        }
    }

    /**
     * Add a String field with given key, setter, and getter.
     * Calls builder.addField(keyedCodec, setter, getter) via reflection.
     */
    ReflectiveCodecBuilder<T> addStringField(String key,
                                              BiConsumer<T, String> setter,
                                              Function<T, String> getter) {
        try {
            Object keyedCodec = newKeyedCodec(key);
            // Find addField method — try all methods since we don't know exact param types
            Method addFieldMethod = findMethod(builder.getClass(), "addField", 3);
            builder = addFieldMethod.invoke(builder, keyedCodec, setter, getter);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Cannot call addField(\"" + key + "\")", e);
        }
    }

    /**
     * Build the final BuilderCodec.
     */
    <R> R build() {
        try {
            Method buildMethod = builder.getClass().getMethod("build");
            return (R) buildMethod.invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException("Cannot call build()", e);
        }
    }

    /**
     * Find a method by name and parameter count (handles unknown descriptor types).
     */
    private static Method findMethod(Class<?> clazz, String name, int paramCount) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                return m;
            }
        }
        throw new RuntimeException("Method " + name + " with " + paramCount
                + " params not found on " + clazz.getName());
    }
}
