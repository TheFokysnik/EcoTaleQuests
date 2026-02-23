package com.crystalrealm.ecotalequests.provider.economy;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Economy bridge that delegates to a pluggable {@link EconomyProvider}.
 *
 * <p>Supports multiple registered providers with config-driven or
 * auto-detected activation. Falls back to the first available provider
 * if the preferred one is unavailable.</p>
 */
public class EconomyBridge {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final Map<String, EconomyProvider> providers = new LinkedHashMap<>();
    private EconomyProvider activeProvider;

    public EconomyBridge() {
        registerProvider("ecotale", new EcotaleEconomyProvider());
        registerProvider("economyapi", new EconomyApiProvider());
    }

    /**
     * Registers an economy provider.
     *
     * @param key      unique identifier (e.g. "ecotale", "generic", "vault")
     * @param provider the provider implementation
     */
    public void registerProvider(@Nonnull String key, @Nonnull EconomyProvider provider) {
        providers.put(key.toLowerCase(), provider);
        LOGGER.info("Registered economy provider: {} ({})", key, provider.getName());
    }

    /**
     * Selects and activates a provider by config key.
     * Falls back to the first available provider if not found.
     *
     * @param preferredKey config key (e.g. "ecotale")
     * @return true if a provider was activated
     */
    public boolean activate(@Nonnull String preferredKey) {
        EconomyProvider preferred = providers.get(preferredKey.toLowerCase());
        if (preferred != null && preferred.isAvailable()) {
            activeProvider = preferred;
            LOGGER.info("Economy provider activated: {} ({})", preferredKey, preferred.getName());
            return true;
        }

        if (preferred != null) {
            LOGGER.warn("Preferred economy provider '{}' not available. Trying others...", preferredKey);
        } else {
            LOGGER.warn("Economy provider '{}' not found. Trying others...", preferredKey);
        }

        for (Map.Entry<String, EconomyProvider> entry : providers.entrySet()) {
            if (entry.getValue().isAvailable()) {
                activeProvider = entry.getValue();
                LOGGER.info("Economy provider fallback: {} ({})", entry.getKey(), entry.getValue().getName());
                return true;
            }
        }

        LOGGER.error("No economy provider available! Currency operations will not work.");
        activeProvider = null;
        return false;
    }

    /** Whether an economy provider is active and available. */
    public boolean isAvailable() {
        return activeProvider != null && activeProvider.isAvailable();
    }

    /** Name of the active economy provider. */
    @Nonnull
    public String getProviderName() {
        return activeProvider != null ? activeProvider.getName() : "none";
    }

    /**
     * Deposits currency to a player's account.
     */
    public boolean deposit(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (activeProvider == null || amount <= 0) return false;
        try {
            return activeProvider.deposit(playerUuid, amount, reason);
        } catch (Exception e) {
            LOGGER.warn("Economy deposit failed ({} to {}): {}", amount, playerUuid, e.getMessage());
            return false;
        }
    }

    /**
     * Gets a player's balance.
     */
    public double getBalance(@Nonnull UUID playerUuid) {
        if (activeProvider == null) return -1;
        try {
            return activeProvider.getBalance(playerUuid);
        } catch (Exception e) {
            LOGGER.warn("Economy getBalance failed ({}): {}", playerUuid, e.getMessage());
            return -1;
        }
    }
}
