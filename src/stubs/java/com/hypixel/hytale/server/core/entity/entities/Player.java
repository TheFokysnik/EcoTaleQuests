package com.hypixel.hytale.server.core.entity.entities;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.ui.PageManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Stub — Hytale Player entity.
 * Real class: com.hypixel.hytale.server.core.entity.entities.Player
 */
public class Player implements CommandSender {

    public UUID getUuid() { return UUID.randomUUID(); }

    public String getDisplayName() { return ""; }

    public boolean hasPermission(String permission) { return false; }

    public void sendMessage(Message message) {}

    public void sendMessage(String message) {}

    public void sendActionBar(String message) {}

    public PlayerRef getPlayerRef() { return new PlayerRef(); }

    /** Get the entity reference for ECS operations. */
    @SuppressWarnings("unchecked")
    public Ref<EntityStore> getReference() {
        throw new UnsupportedOperationException("Stub");
    }

    /** Get the UI page manager for this player. */
    public PageManager getPageManager() {
        return new PageManager();
    }

    public static ComponentType<EntityStore, Player> getComponentType() {
        return new ComponentType<>();
    }

    /**
     * Stub inner — PlayerRef for sendMessage(Message) pattern.
     */
    public static class PlayerRef {
        public void sendMessage(Object message) {}
        public String getUsername() { return ""; }
    }
}
