package com.hypixel.hytale.codec;

/**
 * Hytale codec type constants.
 * Uses generic typing to match real server field descriptors.
 * Without this, direct field access compiles to wrong JVM descriptor
 * and causes NoSuchFieldError at runtime.
 */
public class Codec<T> {
    private Codec() {}

    public static final Codec<String>  STRING  = new Codec<>();
    public static final Codec<Integer> INTEGER = new Codec<>();
    public static final Codec<Boolean> BOOL    = new Codec<>();
    public static final Codec<Double>  DOUBLE  = new Codec<>();
    public static final Codec<Float>   FLOAT   = new Codec<>();
}
