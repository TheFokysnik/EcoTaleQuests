package com.hypixel.hytale.server.core.modules.entity.damage;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Stub — Component added to an entity when it dies.
 * Real class: com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *
 * <p>Contains information about the killing blow via {@link #getDeathInfo()}.</p>
 */
public class DeathComponent implements Component<EntityStore> {

    /**
     * Returns the Damage that killed this entity.
     * Use {@link Damage#getSource()} to find who dealt the killing blow.
     */
    public Damage getDeathInfo() { return null; }

    public static ComponentType<EntityStore, DeathComponent> getComponentType() {
        return new ComponentType<>();
    }
}
