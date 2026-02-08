package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * Stub — Base class for Hytale Java plugins.
 * Real parent: PluginBase → JavaPlugin.
 *
 * <p>Return types match the real server API packages.</p>
 */
public abstract class JavaPlugin {

    public JavaPlugin(JavaPluginInit init) {}

    protected void setup() {}
    protected void start() {}
    protected void shutdown() {}

    @Nonnull
    public Path getDataDirectory() {
        throw new UnsupportedOperationException("Stub");
    }

    @Nonnull
    public String getName() {
        throw new UnsupportedOperationException("Stub");
    }

    @Nonnull
    public String getBasePermission() {
        throw new UnsupportedOperationException("Stub");
    }

    @Nonnull
    public EventRegistry getEventRegistry() {
        throw new UnsupportedOperationException("Stub");
    }

    @Nonnull
    public CommandRegistry getCommandRegistry() {
        throw new UnsupportedOperationException("Stub");
    }

    public ComponentRegistryProxy<EntityStore> getEntityStoreRegistry() { return new ComponentRegistryProxy<>(); }
}
