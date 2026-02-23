package com.crystalrealm.ecotalequests.provider.economy;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Economy provider for <b>EconomyAPI</b> by Sennecoolgames.
 *
 * <p>EconomyAPI is a universal economy bridge that supports multiple backends:
 * EcoTale, TheEconomy, HyEssentialsX, VaultUnlocked, and more.</p>
 *
 * <h3>API</h3>
 * <ul>
 *   <li>{@code EconomyAPI.getAPI()} — singleton</li>
 *   <li>{@code api.deposit(UUID, double, String)} — deposit</li>
 *   <li>{@code api.getBalance(UUID)} — balance query</li>
 * </ul>
 *
 * @see <a href="https://www.curseforge.com/hytale/mods/economyapi">CurseForge</a>
 */
public class EconomyApiProvider implements EconomyProvider {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final String API_CLASS = "com.sennecoolgames.economyapi.EconomyAPI";

    private boolean available;
    private Object apiInstance;
    private Method depositMethod;
    private Method getBalanceMethod;

    public EconomyApiProvider() {
        resolve();
    }

    private void resolve() {
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            Method getApi = apiClass.getMethod("getAPI");
            apiInstance = getApi.invoke(null);
            if (apiInstance == null) {
                LOGGER.info("EconomyAPI.getAPI() returned null — provider disabled.");
                available = false;
                return;
            }

            depositMethod = apiInstance.getClass().getMethod("deposit", UUID.class, double.class, String.class);
            getBalanceMethod = apiInstance.getClass().getMethod("getBalance", UUID.class);

            available = true;
            LOGGER.info("EconomyAPI resolved successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("EconomyAPI not found — provider disabled.");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve EconomyAPI: {}", e.getMessage());
            available = false;
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "EconomyAPI";
    }

    @Override
    public boolean isAvailable() {
        return available && apiInstance != null;
    }

    @Override
    public boolean deposit(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (!isAvailable() || depositMethod == null) return false;
        try {
            depositMethod.invoke(apiInstance, playerUuid, amount, reason);
            return true;
        } catch (Exception e) {
            LOGGER.warn("EconomyAPI deposit failed for {} ({}): {}", playerUuid, amount, e.getMessage());
            return false;
        }
    }

    @Override
    public double getBalance(@Nonnull UUID playerUuid) {
        if (!isAvailable() || getBalanceMethod == null) return 0;
        try {
            Object result = getBalanceMethod.invoke(apiInstance, playerUuid);
            if (result instanceof Number n) return n.doubleValue();
        } catch (Exception e) {
            LOGGER.warn("EconomyAPI getBalance failed for {}: {}", playerUuid, e.getMessage());
        }
        return 0;
    }
}
