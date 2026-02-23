package com.crystalrealm.ecotalequests.provider.leveling;

import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level provider for <b>MMOSkillTree</b> by Ziggfreed.
 *
 * <p>MMOSkillTree uses Hytale's ECS architecture and requires
 * {@code Store<EntityStore>} and {@code Ref<EntityStore>} to query player data.
 * This provider caches these references when players join (via {@link #onPlayerJoin})
 * and uses them for level/XP lookups.</p>
 *
 * <h3>API</h3>
 * <ul>
 *   <li>{@code MMOSkillTreeAPI.getTotalLevel(Store, Ref)} — combined level</li>
 *   <li>{@code MMOSkillTreeAPI.addXp(Store, Ref, SkillType, long)} — grant XP</li>
 * </ul>
 */
public class MMOSkillTreeProvider implements LevelProvider {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final String API_CLASS = "com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI";
    private static final String SKILL_TYPE_CLASS = "com.ziggfreed.mmoskilltree.data.SkillType";

    /** Cached player ECS context: uuid → {Store, Ref} */
    private final ConcurrentHashMap<UUID, Object[]> playerContext = new ConcurrentHashMap<>();

    private boolean available;
    private Method getTotalLevelMethod;
    private Method addXpMethod;
    private Class<?> storeClass;
    private Class<?> refClass;
    private Class<?> skillTypeClass;
    private Object defaultSkillType;

    private String configSkillType;

    public MMOSkillTreeProvider(String configSkillType) {
        this.configSkillType = configSkillType;
        resolve();
    }

    private void resolve() {
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            skillTypeClass = Class.forName(SKILL_TYPE_CLASS);
            storeClass = Class.forName("com.hypixel.hytale.component.Store");
            refClass = Class.forName("com.hypixel.hytale.component.Ref");

            // getTotalLevel(Store, Ref) → int
            getTotalLevelMethod = apiClass.getMethod("getTotalLevel", storeClass, refClass);

            // addXp(Store, Ref, SkillType, long) → boolean
            addXpMethod = apiClass.getMethod("addXp", storeClass, refClass, skillTypeClass, long.class);

            // Resolve default SkillType from config
            resolveDefaultSkillType();

            available = true;
            LOGGER.info("MMOSkillTreeAPI resolved successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("MMOSkillTree not found — provider disabled.");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve MMOSkillTreeAPI: {}", e.getMessage());
            available = false;
        }
    }

    private void resolveDefaultSkillType() {
        if (configSkillType == null || configSkillType.isEmpty()) {
            LOGGER.info("MMOSkillTree: no DefaultSkillType set — XP granting will go to SWORDS.");
            configSkillType = "SWORDS";
        }
        try {
            Method valueOf = skillTypeClass.getMethod("valueOf", String.class);
            defaultSkillType = valueOf.invoke(null, configSkillType.toUpperCase());
            LOGGER.info("MMOSkillTree: default skill type = {}", configSkillType.toUpperCase());
        } catch (Exception e) {
            LOGGER.warn("MMOSkillTree: invalid skill type '{}', falling back to SWORDS", configSkillType);
            try {
                Method valueOf = skillTypeClass.getMethod("valueOf", String.class);
                defaultSkillType = valueOf.invoke(null, "SWORDS");
            } catch (Exception ex) {
                LOGGER.error("MMOSkillTree: cannot resolve SWORDS skill type: {}", ex.getMessage());
            }
        }
    }

    @Override
    public void onPlayerJoin(@Nonnull UUID uuid, @Nonnull Object store, @Nonnull Object ref) {
        playerContext.put(uuid, new Object[]{store, ref});
    }

    @Override
    public void onPlayerLeave(@Nonnull UUID uuid) {
        playerContext.remove(uuid);
    }

    @Nonnull
    @Override
    public String getName() {
        return "MMOSkillTree";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public int getPlayerLevel(@Nonnull UUID playerUuid) {
        if (!isAvailable() || getTotalLevelMethod == null) return 1;
        Object[] ctx = playerContext.get(playerUuid);
        if (ctx == null) {
            LOGGER.warn("MMOSkillTree: no cached context for {} — returning level 1", playerUuid);
            return 1;
        }
        try {
            Object result = getTotalLevelMethod.invoke(null, ctx[0], ctx[1]);
            if (result instanceof Number n) return n.intValue();
        } catch (Exception e) {
            LOGGER.warn("MMOSkillTree getTotalLevel failed for {}: {}", playerUuid, e.getMessage());
        }
        return 1;
    }

    @Override
    public boolean grantXP(@Nonnull UUID playerUuid, double amount, @Nonnull String reason) {
        if (!isAvailable() || addXpMethod == null || defaultSkillType == null) return false;
        Object[] ctx = playerContext.get(playerUuid);
        if (ctx == null) {
            LOGGER.warn("MMOSkillTree: no cached context for {} — cannot grant XP", playerUuid);
            return false;
        }
        try {
            Object result = addXpMethod.invoke(null, ctx[0], ctx[1], defaultSkillType, (long) amount);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            LOGGER.warn("MMOSkillTree addXp failed for {} ({}): {}", playerUuid, amount, e.getMessage());
            return false;
        }
    }
}
