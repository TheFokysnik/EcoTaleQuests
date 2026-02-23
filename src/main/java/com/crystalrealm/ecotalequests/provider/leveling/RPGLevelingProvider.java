package com.crystalrealm.ecotalequests.provider.leveling;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Level provider for RPG Leveling by zuxaw.
 * Uses reflection to call {@code org.zuxaw.plugin.api.RPGLevelingAPI},
 * avoiding compile-time dependency.
 */
public class RPGLevelingProvider implements LevelProvider {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private boolean available;
    private Object apiInstance;
    private Method getPlayerLevelMethod;
    private Method addXPMethod;

    public RPGLevelingProvider() {
        resolve();
    }

    private void resolve() {
        try {
            Class<?> rpgClass = Class.forName("org.zuxaw.plugin.api.RPGLevelingAPI");

            // Try to get the API singleton: get(), getInstance(), getAPI()
            for (String methodName : new String[]{"get", "getInstance", "getAPI"}) {
                try {
                    Method m = rpgClass.getMethod(methodName);
                    apiInstance = m.invoke(null);
                    if (apiInstance != null) {
                        LOGGER.info("RPGLevelingAPI found via {}()", methodName);
                        break;
                    }
                } catch (NoSuchMethodException ignored) {}
            }

            if (apiInstance == null) {
                LOGGER.info("RPGLevelingAPI class found but no singleton accessor returned an instance.");
                available = false;
                return;
            }

            // Resolve methods
            getPlayerLevelMethod = apiInstance.getClass().getMethod("getPlayerLevel", UUID.class);

            // Try addXP (may not exist in all versions)
            try {
                addXPMethod = apiInstance.getClass().getMethod("addXP", UUID.class, double.class);
            } catch (NoSuchMethodException e) {
                try {
                    addXPMethod = apiInstance.getClass().getMethod("addExperience", UUID.class, double.class);
                } catch (NoSuchMethodException ignored) {
                    LOGGER.info("RPGLevelingAPI: addXP/addExperience method not found — XP granting disabled.");
                }
            }

            available = true;
            LOGGER.info("RPGLevelingAPI resolved successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("RPGLevelingAPI not found — RPG level provider disabled.");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve RPGLevelingAPI: {}", e.getMessage());
            available = false;
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "RPG Leveling";
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
            LOGGER.warn("RPG getPlayerLevel failed for {}: {}", playerUuid, e.getMessage());
        }
        return 1;
    }

    @Override
    public boolean grantXP(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (!isAvailable() || addXPMethod == null) return false;
        try {
            addXPMethod.invoke(apiInstance, playerUuid, amount);
            return true;
        } catch (Exception e) {
            LOGGER.warn("RPG grantXP failed for {} ({}): {}", playerUuid, amount, e.getMessage());
            return false;
        }
    }

    /** Returns the raw API instance for event subscription (e.g. XP listeners). */
    @javax.annotation.Nullable
    public Object getRawApi() {
        return apiInstance;
    }
}
