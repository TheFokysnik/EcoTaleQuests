package com.hypixel.hytale.server.core.ui.builder;

import javax.annotation.Nonnull;

/**
 * Event data for UI event bindings. Correct package per TravelAnchors reference.
 * Supports chaining via {@link #append(String, String)}.
 */
public class EventData {

    public EventData() {}

    @Nonnull
    public static EventData of(@Nonnull String key, @Nonnull String value) {
        return new EventData();
    }

    @Nonnull
    public EventData append(@Nonnull String key, @Nonnull String value) {
        return this;
    }
}
