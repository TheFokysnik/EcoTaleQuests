package com.crystalrealm.ecotalequests.provider.leveling;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Level provider for <b>EndlessLeveling</b> by Airijko.
 *
 * <p>Uses reflection to call the {@code EndlessLevelingAPI} singleton,
 * avoiding compile-time dependency.</p>
 *
 * <h3>Resolved methods</h3>
 * <ul>
 *   <li>{@code EndlessLevelingAPI.get()} — singleton accessor</li>
 *   <li>{@code api.getPlayerLevel(UUID)} — player level</li>
 *   <li>{@code api.grantXp(UUID, double)} — grant XP</li>
 * </ul>
 */
public class EndlessLevelingProvider implements LevelProvider {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final String API_CLASS = "com.airijko.endlessleveling.api.EndlessLevelingAPI";

    private boolean available;
    private Object apiInstance;
    private Method getPlayerLevelMethod;
    private Method grantXpMethod;

    public EndlessLevelingProvider() {
        resolve();
    }

    private void resolve() {
        try {
            Class<?> apiClass = Class.forName(API_CLASS);

            // EndlessLevelingAPI.get() → singleton
            Method getMethod = apiClass.getMethod("get");
            apiInstance = getMethod.invoke(null);
            if (apiInstance == null) {
                LOGGER.info("EndlessLevelingAPI.get() returned null — provider disabled.");
                available = false;
                return;
            }

            // api.getPlayerLevel(UUID) → int
            getPlayerLevelMethod = apiInstance.getClass().getMethod("getPlayerLevel", UUID.class);

            // api.grantXp(UUID, double) → void
            try {
                grantXpMethod = apiInstance.getClass().getMethod("grantXp", UUID.class, double.class);
            } catch (NoSuchMethodException e) {
                LOGGER.info("EndlessLevelingAPI: grantXp method not found — XP granting disabled.");
            }

            available = true;
            LOGGER.info("EndlessLevelingAPI resolved successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("EndlessLeveling not found — provider disabled.");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve EndlessLevelingAPI: {}", e.getMessage());
            available = false;
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "Endless Leveling";
    }

    @Override
    public boolean isAvailable() {
        return available && apiInstance != null;
    }

    @Override
    public int getPlayerLevel(@Nonnull UUID playerUuid) {
        if (!isAvailable() || getPlayerLevelMethod == null) return 1;
        try {
            Object result = getPlayerLevelMethod.invoke(apiInstance, playerUuid);
            if (result instanceof Number n) return n.intValue();
        } catch (Exception e) {
            LOGGER.warn("EndlessLeveling getPlayerLevel failed for {}: {}", playerUuid, e.getMessage());
        }
        return 1;
    }

    @Override
    public boolean grantXP(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (!isAvailable() || grantXpMethod == null) return false;
        try {
            grantXpMethod.invoke(apiInstance, playerUuid, amount);
            return true;
        } catch (Exception e) {
            LOGGER.warn("EndlessLeveling grantXp failed for {} ({}): {}", playerUuid, amount, e.getMessage());
            return false;
        }
    }
}
