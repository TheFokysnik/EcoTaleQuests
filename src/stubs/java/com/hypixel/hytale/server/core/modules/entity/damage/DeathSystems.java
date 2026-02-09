package com.hypixel.hytale.server.core.modules.entity.damage;

import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Stub — Hytale death system container.
 * Real class: com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems
 *
 * <p>Contains {@link OnDeathSystem} — an ECS system that triggers
 * when a {@link DeathComponent} is added to an entity (i.e. the entity dies).</p>
 */
public class DeathSystems {

    /**
     * Abstract system that reacts to entity death.
     * Extends {@link RefChangeSystem} so the JVM bridge method
     * {@code onComponentAdded(Ref, Component, Store, CommandBuffer)} is generated.
     *
     * <p>The {@link #getQuery()} method should return the component type
     * that filters which dead entities this system processes
     * (e.g. {@code NPCEntity.getComponentType()} for NPC deaths).</p>
     */
    public static abstract class OnDeathSystem
            extends RefChangeSystem<EntityStore, DeathComponent> {
    }
}
