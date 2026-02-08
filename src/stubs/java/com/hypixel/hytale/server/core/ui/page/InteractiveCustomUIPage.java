package com.hypixel.hytale.server.core.ui.page;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.codec.BuilderCodec;

/**
 * Stub — Interactive custom UI page with data binding.
 * Real class: com.hypixel.hytale.server.core.ui.page.InteractiveCustomUIPage
 *
 * <p>Subclass this to create server-driven UI pages with event handling.
 * The type parameter D is the data class for UI events, decoded via BuilderCodec.</p>
 */
public abstract class InteractiveCustomUIPage<D> {

    private final PlayerRef playerRef;
    private final CustomPageLifetime lifetime;
    private final BuilderCodec<D> codec;

    protected InteractiveCustomUIPage(PlayerRef playerRef, CustomPageLifetime lifetime, BuilderCodec<D> codec) {
        this.playerRef = playerRef;
        this.lifetime = lifetime;
        this.codec = codec;
    }

    /**
     * Build the UI layout. Called when the page is first opened and on sendUpdate().
     *
     * @param ref   entity reference
     * @param cmd   command builder for UI element manipulation
     * @param events event builder for binding UI interactions
     * @param store entity store
     */
    public abstract void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store);

    /**
     * Handle a data event from the UI (button click, input change, etc).
     *
     * @param ref   entity reference
     * @param store entity store
     * @param data  decoded event data
     */
    public abstract void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, D data);

    /** Request a UI rebuild (calls build() again with fresh data). */
    public void sendUpdate() {}

    public PlayerRef getPlayerRef() { return playerRef; }
    public CustomPageLifetime getLifetime() { return lifetime; }
    public BuilderCodec<D> getCodec() { return codec; }
}
