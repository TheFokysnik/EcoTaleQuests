package com.hypixel.hytale.codec;

/**
 * Keyed codec mapping a serialization key to a typed value codec.
 * Per TravelAnchors: {@code new KeyedCodec<String>("Key", Codec.STRING)}
 */
public class KeyedCodec<V> {

    private final String key;

    public KeyedCodec(String key, Codec<V> valueCodec) {
        this.key = key;
    }

    public String getKey() { return key; }
}
