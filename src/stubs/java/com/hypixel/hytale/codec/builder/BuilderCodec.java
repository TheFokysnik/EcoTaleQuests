package com.hypixel.hytale.codec.builder;

import com.hypixel.hytale.codec.KeyedCodec;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Stub matching real Hytale API. Pattern from TravelAnchors:
 * <pre>
 * BuilderCodec.builder(MyData.class, MyData::new)
 *     .append(new KeyedCodec&lt;String&gt;("Key", Codec.STRING), setter, getter).add()
 *     .build()
 * </pre>
 */
public class BuilderCodec<T> {

    private BuilderCodec() {}

    public static <T> Builder<T> builder(Class<T> clazz, Supplier<T> constructor) {
        return new Builder<>();
    }

    public static class Builder<T> {

        public <V> AppendStep<T> append(KeyedCodec<V> keyedCodec,
                                        TriConsumer<T, V, Object> setter,
                                        BiFunction<T, Object, V> getter) {
            return new AppendStep<>(this);
        }

        public BuilderCodec<T> build() {
            return new BuilderCodec<>();
        }
    }

    public static class AppendStep<T> {
        private final Builder<T> builder;

        AppendStep(Builder<T> builder) {
            this.builder = builder;
        }

        public Builder<T> add() {
            return builder;
        }
    }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}
