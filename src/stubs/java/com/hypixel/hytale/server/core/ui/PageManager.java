package com.hypixel.hytale.server.core.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.page.InteractiveCustomUIPage;

/**
 * Stub — Manages UI pages for a player.
 * Real class: com.hypixel.hytale.server.core.ui.PageManager
 *
 * <p>Access via Player.getPageManager().</p>
 */
public class PageManager {

    /**
     * Open a custom interactive UI page for this player.
     *
     * @param ref   player entity reference
     * @param store entity store
     * @param page  the page instance to open
     */
    public void openCustomPage(Ref<EntityStore> ref, Store<EntityStore> store, InteractiveCustomUIPage<?> page) {}
}
