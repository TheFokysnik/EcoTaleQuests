package com.hypixel.hytale.server.core.ui;

/**
 * Stub — Event data payload for UI event bindings.
 * Real class: com.hypixel.hytale.server.core.ui.EventData
 *
 * <p>Use EventData.of(key, value) for static values,
 * or EventData.of("@Key", "#Element.Property") to read from UI element.</p>
 */
public class EventData {

    private final String key;
    private final String value;

    private EventData(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Create an event data entry.
     *
     * @param key   the data key (prefix with @ for codec field binding)
     * @param value literal value or #ElementId.Property selector
     * @return event data instance
     */
    public static EventData of(String key, String value) {
        return new EventData(key, value);
    }

    /**
     * Combine multiple EventData into an array.
     */
    public static EventData[] all(EventData... entries) {
        return entries;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
}
