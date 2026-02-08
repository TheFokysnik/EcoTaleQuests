package com.hypixel.hytale.server.core.ui.builder;

import javax.annotation.Nonnull;

/**
 * UI command builder for interactive custom UI pages.
 * Correct package per TravelAnchors reference.
 */
public class UICommandBuilder {

    public UICommandBuilder() {}

    @Nonnull
    public UICommandBuilder append(@Nonnull String uiFilePath) { return this; }

    /** Append a child UI template into a container element. */
    @Nonnull
    public UICommandBuilder append(@Nonnull String containerSelector, @Nonnull String uiFilePath) { return this; }

    @Nonnull
    public UICommandBuilder set(@Nonnull String selectorDotProperty, @Nonnull String value) { return this; }

    @Nonnull
    public UICommandBuilder set(@Nonnull String selectorDotProperty, boolean value) { return this; }

    @Nonnull
    public UICommandBuilder set(@Nonnull String selectorDotProperty, int value) { return this; }

    @Nonnull
    public UICommandBuilder set(@Nonnull String selectorDotProperty, float value) { return this; }

    @Nonnull
    public UICommandBuilder clear(@Nonnull String selector) { return this; }
}
