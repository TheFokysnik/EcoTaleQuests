package com.hypixel.hytale.server.core.ui.page;

/**
 * Stub — Lifetime policy for custom UI pages.
 * Real class: com.hypixel.hytale.server.core.ui.page.CustomPageLifetime
 */
public enum CustomPageLifetime {
    /** Player can dismiss by pressing ESC. */
    CanDismiss,
    /** Player can dismiss via ESC or through an in-page interaction. */
    CanDismissOrCloseThroughInteraction,
    /** Page must be closed through an in-page interaction only. */
    MustCloseThroughInteraction
}
