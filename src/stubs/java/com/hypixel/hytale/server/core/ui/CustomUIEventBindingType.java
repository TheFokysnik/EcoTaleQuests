package com.hypixel.hytale.server.core.ui;

/**
 * Stub — Types of UI event bindings.
 * Real class: com.hypixel.hytale.server.core.ui.CustomUIEventBindingType
 */
public enum CustomUIEventBindingType {
    /** Triggered when a button or interactive element is activated (clicked). */
    Activating,
    /** Triggered when a value changes (e.g. text input, slider). */
    ValueChanged,
    /** Triggered when the page is about to be dismissed. */
    Dismissing,
    /** Triggered when mouse enters an element. */
    MouseEntered,
    /** Triggered when mouse exits an element. */
    MouseExited,
    /** Triggered when selected tab changes in TabNavigation. */
    SelectedTabChanged
}
