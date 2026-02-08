package com.hypixel.hytale.server.core.ui;

/**
 * Stub — Builder for UI manipulation commands.
 * Real class: com.hypixel.hytale.server.core.ui.UICommandBuilder
 *
 * <p>Used inside InteractiveCustomUIPage.build() to construct the UI layout.</p>
 */
public class UICommandBuilder {

    /** Append a .ui file to the page. */
    public UICommandBuilder append(String uiFilePath) { return this; }

    /** Append a .ui file inline into a specific element. */
    public UICommandBuilder appendInline(String selector, String uiFilePath) { return this; }

    /** Set a property value on a UI element by selector. */
    public UICommandBuilder set(String selectorDotProperty, String value) { return this; }

    /** Set a boolean property on a UI element. */
    public UICommandBuilder set(String selectorDotProperty, boolean value) { return this; }

    /** Set integer property on a UI element. */
    public UICommandBuilder set(String selectorDotProperty, int value) { return this; }

    /** Set float property on a UI element. */
    public UICommandBuilder set(String selectorDotProperty, float value) { return this; }

    /** Set an object value (complex property). */
    public UICommandBuilder setObject(String selectorDotProperty, Object value) { return this; }

    /** Clear contents of an element (remove children). */
    public UICommandBuilder clear(String selector) { return this; }
}
