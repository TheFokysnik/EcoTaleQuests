package com.hypixel.hytale.server.core.entity;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Stub — Hytale UUID component attached to entities.
 * Real class: com.hypixel.hytale.server.core.entity.UUIDComponent
 */
public class UUIDComponent implements Component<EntityStore> {

    /** The unique identifier of this entity. */
    public UUID getUuid() { return null; }

    public static ComponentType<EntityStore, UUIDComponent> getComponentType() {
        return new ComponentType<>();
    }
}
