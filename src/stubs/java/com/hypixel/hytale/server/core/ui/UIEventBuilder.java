package com.hypixel.hytale.server.core.ui;

/**
 * Stub — Builder for binding UI events.
 * Real class: com.hypixel.hytale.server.core.ui.UIEventBuilder
 *
 * <p>Used inside InteractiveCustomUIPage.build() to bind events to UI elements.</p>
 */
public class UIEventBuilder {

    /**
     * Bind an event to a UI element.
     *
     * @param eventType the type of UI event (Activating, ValueChanged, etc.)
     * @param selector  CSS-like selector for the target element (e.g., "#ButtonId")
     * @param data      event data payload to send when event triggers
     * @return this builder
     */
    public UIEventBuilder addEventBinding(CustomUIEventBindingType eventType, String selector, EventData data) {
        return this;
    }
}
