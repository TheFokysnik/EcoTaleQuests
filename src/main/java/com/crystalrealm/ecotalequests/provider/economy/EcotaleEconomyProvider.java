package com.crystalrealm.ecotalequests.provider.economy;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Economy provider for the Ecotale plugin.
 * Uses reflection to call {@code com.ecotale.api.EcotaleAPI} methods,
 * avoiding compile-time dependency.
 */
public class EcotaleEconomyProvider implements EconomyProvider {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private boolean available;
    private Method depositMethod;
    private Method getBalanceMethod;
    private Method isAvailableMethod;

    public EcotaleEconomyProvider() {
        resolve();
    }

    private void resolve() {
        try {
            Class<?> clazz = Class.forName("com.ecotale.api.EcotaleAPI");
            depositMethod = clazz.getMethod("deposit", UUID.class, double.class, String.class);
            getBalanceMethod = clazz.getMethod("getBalance", UUID.class);
            isAvailableMethod = clazz.getMethod("isAvailable");
            available = checkAvailable();
            if (available) {
                LOGGER.info("EcotaleAPI resolved successfully.");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("EcotaleAPI not found — Ecotale economy provider disabled.");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve EcotaleAPI: {}", e.getMessage());
            available = false;
        }
    }

    private boolean checkAvailable() {
        if (isAvailableMethod == null) return false;
        try {
            Object result = isAvailableMethod.invoke(null);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            return false;
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "Ecotale";
    }

    @Override
    public boolean isAvailable() {
        return available || checkAvailable();
    }

    @Override
    public boolean deposit(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (depositMethod == null || amount <= 0) return false;
        try {
            Object result = depositMethod.invoke(null, playerUuid, amount, reason);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            LOGGER.warn("Ecotale deposit failed for {} ({}): {}", playerUuid, amount, e.getMessage());
            return false;
        }
    }

    @Override
    public double getBalance(@Nonnull UUID playerUuid) {
        if (getBalanceMethod == null) return -1;
        try {
            Object result = getBalanceMethod.invoke(null, playerUuid);
            if (result instanceof Number n) return n.doubleValue();
        } catch (Exception e) {
            LOGGER.warn("Ecotale getBalance failed for {}: {}", playerUuid, e.getMessage());
        }
        return -1;
    }
}
