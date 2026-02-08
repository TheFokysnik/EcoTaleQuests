package com.hypixel.hytale.server.core.universe;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Stub — PlayerRef is the ECS component for player entities.
 * Used in EntityEventSystem.handle() to identify the player who triggered an event.
 *
 * <p>Real class: com.hypixel.hytale.server.core.universe.PlayerRef</p>
 */
public class PlayerRef implements Component<EntityStore> {

    public String getUsername() { return ""; }

    public UUID getUuid() { return UUID.randomUUID(); }

    public boolean isValid() { return true; }

    public int getIndex() { return 0; }

    /** Get the entity reference for this player. */
    public Ref<EntityStore> getReference() { return null; }

    /** Send a message to this player. */
    public void sendMessage(Object message) {}

    public static ComponentType<EntityStore, PlayerRef> getComponentType() {
        return new ComponentType<>();
    }
}
