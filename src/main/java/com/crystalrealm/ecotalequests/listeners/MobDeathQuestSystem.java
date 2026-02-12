package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.model.QuestType;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

public class MobDeathQuestSystem extends DeathSystems.OnDeathSystem {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestTracker questTracker;
    private final ComponentType<EntityStore, NPCEntity> npcType;
    private final ComponentType<EntityStore, Player> playerType;

    // Cached reflection methods (resolved once)
    private volatile Method getComponentMethod;
    private volatile boolean methodResolved = false;

    public MobDeathQuestSystem(@Nonnull QuestTracker questTracker) {
        this.questTracker = questTracker;
        this.npcType = NPCEntity.getComponentType();
        this.playerType = Player.getComponentType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return npcType;
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent death,
                                 Store<EntityStore> store,
                                 CommandBuffer<EntityStore> commandBuffer) {
        try {
            processNpcDeath(ref, death, store, commandBuffer);
        } catch (Throwable e) {
            LOGGER.warn("Error in MobDeathQuestSystem: {}", e.getMessage(), e);
        }
    }

    // ==================================================================
    //  Reflection-based component access
    // ==================================================================

    @SuppressWarnings("unchecked")
    private <C> C getComp(Object accessor, Ref<EntityStore> ref, ComponentType<EntityStore, C> type) {
        try {
            // Try to find getComponent method if not yet resolved
            if (!methodResolved || getComponentMethod == null) {
                getComponentMethod = findGetComponentMethod(accessor);
                methodResolved = true;
                if (getComponentMethod != null) {
                    LOGGER.info("Resolved getComponent method: {} on {}",
                            getComponentMethod.toGenericString(), accessor.getClass().getName());
                }
            }
            if (getComponentMethod != null) {
                return (C) getComponentMethod.invoke(accessor, ref, type);
            }

            // Brute-force: try all methods named getComponent
            for (Method m : accessor.getClass().getMethods()) {
                if (m.getName().equals("getComponent") && m.getParameterCount() == 2) {
                    try {
                        m.setAccessible(true);
                        Object result = m.invoke(accessor, ref, type);
                        // Cache this method for future calls
                        getComponentMethod = m;
                        LOGGER.info("Found getComponent via brute force: {}", m.toGenericString());
                        return (C) result;
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            LOGGER.warn("getComp reflection failed: {}", e.getMessage());
        }
        return null;
    }

    private Method findGetComponentMethod(Object accessor) {
        // Strategy 1: exact match Ref.class, ComponentType.class
        try {
            Method m = accessor.getClass().getMethod("getComponent", Ref.class, ComponentType.class);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ignored) {}

        // Strategy 2: scan all getComponent methods with 2 params
        for (Method m : accessor.getClass().getMethods()) {
            if (m.getName().equals("getComponent") && m.getParameterCount() == 2) {
                Class<?>[] params = m.getParameterTypes();
                if (params[0].isAssignableFrom(Ref.class)) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }

        // Log all available methods for diagnostics
        LOGGER.warn("Could not find getComponent on {}. Available methods:", accessor.getClass().getName());
        for (Method m : accessor.getClass().getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            LOGGER.warn("  {} {}({}params) -> {}", m.getName(), m.getParameterCount(),
                    m.getParameterTypes().length > 0 ? m.getParameterTypes()[0].getSimpleName() + "..." : "",
                    m.getReturnType().getSimpleName());
        }
        return null;
    }

    // ==================================================================
    //  Main logic
    // ==================================================================

    private void processNpcDeath(Ref<EntityStore> ref, DeathComponent death,
                                 Store<EntityStore> store,
                                 CommandBuffer<EntityStore> commandBuffer) {
        // Try commandBuffer first (rpgstats pattern), then store
        Object accessor = commandBuffer != null ? commandBuffer : store;

        // 1. Get NPC component
        NPCEntity npc = getComp(accessor, ref, npcType);
        if (npc == null && accessor == commandBuffer) {
            // Fallback to store
            npc = getComp(store, ref, npcType);
        }
        if (npc == null) {
            LOGGER.info("processNpcDeath: NPCEntity is null for ref={}", ref);
            return;
        }

        // 2. Get NPC type identifier
        String npcTypeId = safeNpcTypeId(npc);
        String roleName = safeRoleName(npc);
        LOGGER.info("NPC death detected: npcTypeId={}, role={}", npcTypeId, roleName);

        // 3. Resolve attacker
        Ref<EntityStore> attackerRef = resolveAttackerRef(npc, death);
        if (attackerRef == null || !attackerRef.isValid()) {
            LOGGER.info("No valid attacker for NPC death: npcTypeId={}", npcTypeId);
            return;
        }

        // 4. Verify attacker is a Player
        Player killer = getComp(accessor, attackerRef, playerType);
        if (killer == null && accessor == commandBuffer) {
            killer = getComp(store, attackerRef, playerType);
        }
        if (killer == null) {
            LOGGER.info("Attacker is not a player: npcTypeId={}", npcTypeId);
            return;
        }

        // 5. Get player UUID via PlayerRef
        PlayerRef playerRef = getComp(accessor, attackerRef, PlayerRef.getComponentType());
        if (playerRef == null && accessor == commandBuffer) {
            playerRef = getComp(store, attackerRef, PlayerRef.getComponentType());
        }
        UUID playerUuid;
        if (playerRef != null) {
            playerUuid = playerRef.getUuid();
            try { MessageUtil.cachePlayerRef(playerUuid, playerRef); } catch (Exception ignored) {}
        } else {
            playerUuid = killer.getUuid();
        }

        if (playerUuid == null) {
            LOGGER.info("Could not resolve player UUID for kill: npcTypeId={}", npcTypeId);
            return;
        }

        // 6. Track the kill
        int playerLevel = resolvePlayerLevel(playerUuid);
        LOGGER.info("Mob kill tracked: player={}, npcTypeId={}, role={}, level={}",
                playerUuid, npcTypeId, roleName, playerLevel);
        questTracker.handleAction(playerUuid, QuestType.KILL_MOB, npcTypeId, 1, playerLevel);
    }

    // ==================================================================
    //  Attacker resolution
    // ==================================================================

    private Ref<EntityStore> resolveAttackerRef(NPCEntity npc, DeathComponent death) {
        // Strategy 1: DeathComponent -> Damage -> EntitySource
        if (death != null) {
            try {
                Damage damage = death.getDeathInfo();
                if (damage != null) {
                    Damage.Source source = damage.getSource();
                    if (source instanceof Damage.EntitySource entitySource) {
                        Ref<EntityStore> ref = entitySource.getRef();
                        if (ref != null && ref.isValid()) {
                            LOGGER.info("Attacker resolved from death info");
                            return ref;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.info("DeathInfo resolution failed: {}", e.getMessage());
            }
        }

        // Strategy 2: NPCEntity damage data (via reflection — method may not exist in all API versions)
        try {
            java.lang.reflect.Method getDmg = npc.getClass().getMethod("getDamageData");
            Object damageData = getDmg.invoke(npc);
            if (damageData != null) {
                java.lang.reflect.Method getMost = damageData.getClass().getMethod("getMostDamagingAttacker");
                @SuppressWarnings("unchecked")
                Ref<EntityStore> ref = (Ref<EntityStore>) getMost.invoke(damageData);
                if (ref != null && ref.isValid()) {
                    LOGGER.info("Attacker resolved from damage data (most damaging)");
                    return ref;
                }
                java.lang.reflect.Method getAny = damageData.getClass().getMethod("getAnyAttacker");
                @SuppressWarnings("unchecked")
                Ref<EntityStore> any = (Ref<EntityStore>) getAny.invoke(damageData);
                if (any != null && any.isValid()) {
                    LOGGER.info("Attacker resolved from damage data (any)");
                    return any;
                }
            }
        } catch (NoSuchMethodException ignored) {
            // getDamageData() not available in this Hytale version — skip silently
        } catch (Exception e) {
            LOGGER.info("DamageData resolution failed: {}", e.getMessage());
        }

        return null;
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private static String safeNpcTypeId(NPCEntity npc) {
        try {
            String typeId = npc.getNPCTypeId();
            if (typeId != null && !typeId.isBlank()) return typeId.toLowerCase().trim();
        } catch (Exception ignored) {}
        return "mob";
    }

    private static String safeRoleName(NPCEntity npc) {
        try {
            String roleName = npc.getRoleName();
            if (roleName != null && !roleName.isBlank()) return roleName;
            Role role = npc.getRole();
            if (role != null) {
                String rn = role.getRoleName();
                if (rn != null && !rn.isBlank()) return rn;
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private static int resolvePlayerLevel(UUID playerUuid) {
        try {
            Class<?> rpgClass = Class.forName("org.zuxaw.plugin.api.RPGLevelingAPI");
            Object api = null;
            for (String name : new String[]{"get", "getInstance", "getAPI"}) {
                try {
                    Method m = rpgClass.getMethod(name);
                    api = m.invoke(null);
                    if (api != null) break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (api != null) {
                Method getLevel = api.getClass().getMethod("getPlayerLevel", UUID.class);
                Object level = getLevel.invoke(api, playerUuid);
                if (level instanceof Number n) return n.intValue();
            }
        } catch (Exception ignored) {}
        return 1;
    }
}
