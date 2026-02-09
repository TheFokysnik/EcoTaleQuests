package com.hypixel.hytale.server.npc.entities;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;

/**
 * Stub — Hytale NPC entity component.
 * Real class: com.hypixel.hytale.server.npc.entities.NPCEntity
 *
 * <p>Provides NPC identification: type ID (e.g. "skeleton"),
 * role name, role object, and damage tracking data.</p>
 */
public class NPCEntity implements Component<EntityStore> {

    /** NPC type identifier, e.g. "skeleton", "zombie", "spider". */
    public String getNPCTypeId() { return null; }

    /** NPC role name (human-readable). */
    public String getRoleName() { return null; }

    /** NPC role object with hostility info. */
    public Role getRole() { return null; }

    /** Damage tracking data for this NPC. */
    public DamageData getDamageData() { return null; }

    public static ComponentType<EntityStore, NPCEntity> getComponentType() {
        return new ComponentType<>();
    }

    /**
     * Damage tracking for an NPC — records attackers.
     */
    public static class DamageData {
        /** The entity that dealt the most total damage. */
        public Ref<EntityStore> getMostDamagingAttacker() { return null; }

        /** Any valid attacker (fallback). */
        public Ref<EntityStore> getAnyAttacker() { return null; }
    }
}
