package com.crystalrealm.ecotalequests.provider.economy;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Universal economy adapter that connects to ANY economy plugin via reflection.
 *
 * <p>No coding required — the server owner specifies class name and method names
 * in the config, and this adapter resolves them at runtime.</p>
 *
 * <h3>Supported patterns</h3>
 * <ul>
 *   <li><b>Static methods:</b> {@code EconomyAPI.deposit(UUID, double)}</li>
 *   <li><b>Instance via getter:</b> {@code EconomyAPI.getInstance().deposit(UUID, double)}</li>
 * </ul>
 *
 * <h3>Config example</h3>
 * <pre>{@code
 * "EconomyProvider": "generic",
 * "GenericEconomy": {
 *   "ClassName": "com.example.economy.EconomyAPI",
 *   "InstanceMethod": "",
 *   "DepositMethod": "deposit",
 *   "BalanceMethod": "getBalance",
 *   "DepositHasReason": true
 * }
 * }</pre>
 */
public class GenericEconomyProvider implements EconomyProvider {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final String className;
    private final String instanceMethodName;
    private final String depositMethodName;
    private final String balanceMethodName;
    private final boolean depositHasReason;

    private boolean available;
    private Class<?> apiClass;
    private Method instanceMethod;
    private Method depositMethod;
    private Method balanceMethod;

    public GenericEconomyProvider(@Nonnull String className,
                                  @Nonnull String instanceMethod,
                                  @Nonnull String depositMethod,
                                  @Nonnull String balanceMethod,
                                  boolean depositHasReason) {
        this.className = className;
        this.instanceMethodName = instanceMethod;
        this.depositMethodName = depositMethod;
        this.balanceMethodName = balanceMethod;
        this.depositHasReason = depositHasReason;
        resolve();
    }

    private void resolve() {
        try {
            apiClass = Class.forName(className);
            Class<?> targetClass = apiClass;

            if (instanceMethodName != null && !instanceMethodName.isEmpty()) {
                instanceMethod = apiClass.getMethod(instanceMethodName);
                targetClass = instanceMethod.getReturnType();
            }

            depositMethod = findDepositMethod(targetClass);
            if (depositMethod == null) {
                LOGGER.error("GenericEconomy: deposit method '{}' not found in {}", depositMethodName, targetClass.getName());
                available = false;
                return;
            }

            balanceMethod = findBalanceMethod(targetClass);
            if (balanceMethod == null) {
                LOGGER.error("GenericEconomy: balance method '{}' not found in {}", balanceMethodName, targetClass.getName());
                available = false;
                return;
            }

            available = true;
            LOGGER.info("GenericEconomy: resolved '{}' successfully.", className);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("GenericEconomy: class '{}' not found.", className);
            available = false;
        } catch (Exception e) {
            LOGGER.error("GenericEconomy: failed to resolve: {}", e.getMessage());
            available = false;
        }
    }

    private Method findDepositMethod(Class<?> target) {
        if (depositHasReason) {
            try { return target.getMethod(depositMethodName, UUID.class, double.class, String.class); }
            catch (NoSuchMethodException ignored) {}
        }
        try { return target.getMethod(depositMethodName, UUID.class, double.class); }
        catch (NoSuchMethodException ignored) {}
        try { return target.getMethod(depositMethodName, String.class, double.class); }
        catch (NoSuchMethodException ignored) {}
        if (!depositHasReason) {
            try { return target.getMethod(depositMethodName, UUID.class, double.class, String.class); }
            catch (NoSuchMethodException ignored) {}
        }
        try { return target.getMethod(depositMethodName, UUID.class, int.class); }
        catch (NoSuchMethodException ignored) {}
        return null;
    }

    private Method findBalanceMethod(Class<?> target) {
        try { return target.getMethod(balanceMethodName, UUID.class); }
        catch (NoSuchMethodException ignored) {}
        try { return target.getMethod(balanceMethodName, String.class); }
        catch (NoSuchMethodException ignored) {}
        try { return target.getMethod(balanceMethodName); }
        catch (NoSuchMethodException ignored) {}
        return null;
    }

    private Object getApiInstance() throws Exception {
        if (instanceMethod == null) return null;
        return instanceMethod.invoke(null);
    }

    @Nonnull
    @Override
    public String getName() {
        return "Generic (" + className + ")";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public boolean deposit(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (!available || depositMethod == null) return false;
        try {
            Object target = getApiInstance();
            Class<?>[] paramTypes = depositMethod.getParameterTypes();
            Object result;
            if (paramTypes.length == 3 && paramTypes[2] == String.class) {
                result = depositMethod.invoke(target, playerUuid, amount, reason);
            } else if (paramTypes.length == 2 && paramTypes[0] == UUID.class && paramTypes[1] == double.class) {
                result = depositMethod.invoke(target, playerUuid, amount);
            } else if (paramTypes.length == 2 && paramTypes[0] == String.class) {
                result = depositMethod.invoke(target, playerUuid.toString(), amount);
            } else if (paramTypes.length == 2 && paramTypes[0] == UUID.class && paramTypes[1] == int.class) {
                result = depositMethod.invoke(target, playerUuid, (int) amount);
            } else {
                return false;
            }
            if (result instanceof Boolean b) return b;
            return true;
        } catch (Exception e) {
            LOGGER.warn("GenericEconomy deposit failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public double getBalance(@Nonnull UUID playerUuid) {
        if (!available || balanceMethod == null) return -1;
        try {
            Object target = getApiInstance();
            Class<?>[] paramTypes = balanceMethod.getParameterTypes();
            Object result;
            if (paramTypes.length == 1 && paramTypes[0] == UUID.class) {
                result = balanceMethod.invoke(target, playerUuid);
            } else if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                result = balanceMethod.invoke(target, playerUuid.toString());
            } else if (paramTypes.length == 0) {
                result = balanceMethod.invoke(target);
            } else {
                return -1;
            }
            if (result instanceof Number n) return n.doubleValue();
            return -1;
        } catch (Exception e) {
            LOGGER.warn("GenericEconomy getBalance failed: {}", e.getMessage());
            return -1;
        }
    }
}
