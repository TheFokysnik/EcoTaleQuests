package com.crystalrealm.ecotalequests.provider.leveling;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Universal leveling adapter that connects to ANY leveling plugin via reflection.
 *
 * <p>The server owner specifies class name and method names in the config,
 * and this adapter resolves them at runtime.</p>
 *
 * <h3>Config example</h3>
 * <pre>{@code
 * "LevelProvider": "generic",
 * "GenericLeveling": {
 *   "ClassName": "com.example.leveling.LevelAPI",
 *   "InstanceMethod": "getInstance",
 *   "GetLevelMethod": "getPlayerLevel",
 *   "GrantXPMethod": "addXP"
 * }
 * }</pre>
 */
public class GenericLevelProvider implements LevelProvider {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final String className;
    private final String instanceMethodName;
    private final String getLevelMethodName;
    private final String grantXPMethodName;

    private boolean available;
    private Class<?> apiClass;
    private Method instanceMethod;
    private Method getLevelMethod;
    private Method grantXPMethod;

    public GenericLevelProvider(@Nonnull String className,
                                @Nonnull String instanceMethod,
                                @Nonnull String getLevelMethod,
                                @Nonnull String grantXPMethod) {
        this.className = className;
        this.instanceMethodName = instanceMethod;
        this.getLevelMethodName = getLevelMethod;
        this.grantXPMethodName = grantXPMethod;
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

            // Resolve getLevel method (UUID → int/Number)
            getLevelMethod = findGetLevelMethod(targetClass);
            if (getLevelMethod == null) {
                LOGGER.error("GenericLevel: getLevel method '{}' not found in {}", getLevelMethodName, targetClass.getName());
                available = false;
                return;
            }

            // Resolve grantXP method (UUID, double → void/boolean)
            if (grantXPMethodName != null && !grantXPMethodName.isEmpty()) {
                grantXPMethod = findGrantXPMethod(targetClass);
                if (grantXPMethod == null) {
                    LOGGER.warn("GenericLevel: grantXP method '{}' not found — XP granting disabled.", grantXPMethodName);
                }
            }

            available = true;
            LOGGER.info("GenericLevel: resolved '{}' successfully.", className);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("GenericLevel: class '{}' not found.", className);
            available = false;
        } catch (Exception e) {
            LOGGER.error("GenericLevel: failed to resolve: {}", e.getMessage());
            available = false;
        }
    }

    private Method findGetLevelMethod(Class<?> target) {
        try { return target.getMethod(getLevelMethodName, UUID.class); }
        catch (NoSuchMethodException ignored) {}
        try { return target.getMethod(getLevelMethodName, String.class); }
        catch (NoSuchMethodException ignored) {}
        return null;
    }

    private Method findGrantXPMethod(Class<?> target) {
        try { return target.getMethod(grantXPMethodName, UUID.class, double.class); }
        catch (NoSuchMethodException ignored) {}
        try { return target.getMethod(grantXPMethodName, UUID.class, int.class); }
        catch (NoSuchMethodException ignored) {}
        try { return target.getMethod(grantXPMethodName, UUID.class, double.class, String.class); }
        catch (NoSuchMethodException ignored) {}
        try { return target.getMethod(grantXPMethodName, String.class, double.class); }
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
    public int getPlayerLevel(@Nonnull UUID playerUuid) {
        if (!available || getLevelMethod == null) return 1;
        try {
            Object target = getApiInstance();
            Class<?>[] paramTypes = getLevelMethod.getParameterTypes();
            Object result;
            if (paramTypes.length == 1 && paramTypes[0] == UUID.class) {
                result = getLevelMethod.invoke(target, playerUuid);
            } else if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                result = getLevelMethod.invoke(target, playerUuid.toString());
            } else {
                return 1;
            }
            if (result instanceof Number n) return n.intValue();
            return 1;
        } catch (Exception e) {
            LOGGER.warn("GenericLevel getPlayerLevel failed: {}", e.getMessage());
            return 1;
        }
    }

    @Override
    public boolean grantXP(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (!available || grantXPMethod == null) return false;
        try {
            Object target = getApiInstance();
            Class<?>[] paramTypes = grantXPMethod.getParameterTypes();
            if (paramTypes.length == 3 && paramTypes[2] == String.class) {
                grantXPMethod.invoke(target, playerUuid, amount, reason);
            } else if (paramTypes.length == 2 && paramTypes[0] == UUID.class && paramTypes[1] == double.class) {
                grantXPMethod.invoke(target, playerUuid, amount);
            } else if (paramTypes.length == 2 && paramTypes[0] == UUID.class && paramTypes[1] == int.class) {
                grantXPMethod.invoke(target, playerUuid, (int) amount);
            } else if (paramTypes.length == 2 && paramTypes[0] == String.class) {
                grantXPMethod.invoke(target, playerUuid.toString(), amount);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("GenericLevel grantXP failed: {}", e.getMessage());
            return false;
        }
    }
}
