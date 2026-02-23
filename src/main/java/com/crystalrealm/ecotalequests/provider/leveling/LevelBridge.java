package com.crystalrealm.ecotalequests.provider.leveling;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Leveling bridge that delegates to a pluggable {@link LevelProvider}.
 *
 * <p>Supports multiple registered providers with config-driven or
 * auto-detected activation.</p>
 */
public class LevelBridge {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final Map<String, LevelProvider> providers = new LinkedHashMap<>();
    private LevelProvider activeProvider;

    public LevelBridge() {
        registerProvider("rpgleveling", new RPGLevelingProvider());
        registerProvider("endlessleveling", new EndlessLevelingProvider());
    }

    /**
     * Registers a level provider.
     *
     * @param key      unique identifier (e.g. "rpgleveling", "generic")
     * @param provider the provider implementation
     */
    public void registerProvider(@Nonnull String key, @Nonnull LevelProvider provider) {
        providers.put(key.toLowerCase(), provider);
        LOGGER.info("Registered level provider: {} ({})", key, provider.getName());
    }

    /**
     * Selects and activates a provider by config key.
     * Falls back to the first available provider if not found.
     *
     * @param preferredKey config key (e.g. "rpgleveling")
     * @return true if a provider was activated
     */
    public boolean activate(@Nonnull String preferredKey) {
        LevelProvider preferred = providers.get(preferredKey.toLowerCase());
        if (preferred != null && preferred.isAvailable()) {
            activeProvider = preferred;
            LOGGER.info("Level provider activated: {} ({})", preferredKey, preferred.getName());
            return true;
        }

        if (preferred != null) {
            LOGGER.warn("Preferred level provider '{}' not available. Trying others...", preferredKey);
        } else {
            LOGGER.warn("Level provider '{}' not found. Trying others...", preferredKey);
        }

        for (Map.Entry<String, LevelProvider> entry : providers.entrySet()) {
            if (entry.getValue().isAvailable()) {
                activeProvider = entry.getValue();
                LOGGER.info("Level provider fallback: {} ({})", entry.getKey(), entry.getValue().getName());
                return true;
            }
        }

        LOGGER.info("No level provider available — all level-dependent features will use level 1.");
        activeProvider = null;
        return false;
    }

    /** Whether a level provider is active and available. */
    public boolean isAvailable() {
        return activeProvider != null && activeProvider.isAvailable();
    }

    /** Name of the active level provider. */
    @Nonnull
    public String getProviderName() {
        return activeProvider != null ? activeProvider.getName() : "none";
    }

    /**
     * Gets a player's level via the active provider.
     *
     * @return player level, or 1 if no provider available
     */
    public int getPlayerLevel(@Nonnull UUID playerUuid) {
        if (activeProvider == null) return 1;
        try {
            return activeProvider.getPlayerLevel(playerUuid);
        } catch (Exception e) {
            LOGGER.warn("Level getPlayerLevel failed ({}): {}", playerUuid, e.getMessage());
            return 1;
        }
    }

    /**
     * Grants XP to a player via the active provider.
     */
    public boolean grantXP(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (activeProvider == null || amount <= 0) return false;
        try {
            return activeProvider.grantXP(playerUuid, amount, reason);
        } catch (Exception e) {
            LOGGER.warn("Level grantXP failed ({}, {}): {}", playerUuid, amount, e.getMessage());
            return false;
        }
    }

    /** Notify all providers about a new player (caches ECS Store/Ref for providers that need it). */
    public void onPlayerJoin(@Nonnull UUID uuid, @Nonnull Object store, @Nonnull Object ref) {
        for (LevelProvider p : providers.values()) {
            try { p.onPlayerJoin(uuid, store, ref); } catch (Exception ignored) {}
        }
    }

    /** Notify all providers that a player left. */
    public void onPlayerLeave(@Nonnull UUID uuid) {
        for (LevelProvider p : providers.values()) {
            try { p.onPlayerLeave(uuid); } catch (Exception ignored) {}
        }
    }
}
