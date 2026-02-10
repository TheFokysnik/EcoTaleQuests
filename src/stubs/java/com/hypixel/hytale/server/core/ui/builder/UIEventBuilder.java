package com.hypixel.hytale.server.core.ui.builder;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;

import javax.annotation.Nonnull;

/**
 * UI event builder for interactive custom UI pages.
 * Correct package per TravelAnchors reference.
 */
public class UIEventBuilder {

    public UIEventBuilder() {}

    @Nonnull
    public UIEventBuilder addEventBinding(@Nonnull CustomUIEventBindingType eventType,
                                          @Nonnull String selector,
                                          @Nonnull EventData data) {
        return this;
    }

    @Nonnull
    public UIEventBuilder addEventBinding(@Nonnull CustomUIEventBindingType eventType,
                                          @Nonnull String selector,
                                          @Nonnull EventData data,
                                          boolean propagate) {
        return this;
    }
}
