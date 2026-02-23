package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Listener for XP tracking via leveling API event subscription.
 *
 * <p>Tracks GAIN_XP quests via ExperienceGainedEvent.
 * <b>KILL_MOB quests are handled separately by {@link MobDeathQuestSystem}</b>,
 * which uses the native ECS DeathSystems.OnDeathSystem
 * with direct access to NPCEntity.getNPCTypeId().</p>
 *
 * <p>Since v1.4.0, uses raw API object via reflection instead of
 * direct typesafe import, enabling the plugin to work without
 * RPG Leveling being installed on the server.</p>
 */
public class MobKillQuestListener {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestTracker questTracker;
    private boolean registered = false;

    public MobKillQuestListener(@Nonnull QuestTracker questTracker) {
        this.questTracker = questTracker;
    }

    /**
     * Registers the XP event listener using a raw API object (via reflection).
     *
     * @param rawRpgApi raw RPGLevelingAPI instance (may be null)
     */
    public void registerWithRawApi(@Nullable Object rawRpgApi) {
        if (rawRpgApi == null) {
            LOGGER.warn("Leveling API not available — XP quest tracking disabled.");
            LOGGER.warn("Quests of type GAIN_XP will not track progress without a leveling plugin.");
            LOGGER.info("KILL_MOB quests use native DeathSystem — no leveling API required.");
            return;
        }

        try {
            // Find registerExperienceGainedListener method
            Method registerMethod = null;
            for (Method m : rawRpgApi.getClass().getMethods()) {
                if (m.getName().equals("registerExperienceGainedListener") && m.getParameterCount() == 1) {
                    registerMethod = m;
                    break;
                }
            }
            if (registerMethod == null) {
                LOGGER.warn("registerExperienceGainedListener not found in leveling API — XP tracking disabled.");
                return;
            }

            // Create a lambda-compatible listener using the functional interface parameter type
            Class<?> listenerType = registerMethod.getParameterTypes()[0];

            // We need to create a proxy for the listener interface
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    rawRpgApi.getClass().getClassLoader(),
                    new Class<?>[]{listenerType},
                    (proxy, method, args) -> {
                        if (method.getName().equals("onExperienceGained") || method.getParameterCount() == 1) {
                            handleXPEvent(args[0]);
                        }
                        return null;
                    }
            );

            registerMethod.invoke(rawRpgApi, listener);
            registered = true;
            LOGGER.info("MobKillQuestListener registered via Leveling API (XP tracking only).");
            LOGGER.info("KILL_MOB tracking delegated to MobDeathQuestSystem (native ECS).");
        } catch (Exception e) {
            LOGGER.warn("Failed to register XP listener via leveling API: {}", e.getMessage());
        }
    }

    /**
     * Handles an XP gain event via reflection.
     */
    private void handleXPEvent(@Nonnull Object event) {
        try {
            // Get player ref
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Object playerRef = getPlayer.invoke(event);
            if (playerRef == null) return;

            Method getUuid = playerRef.getClass().getMethod("getUuid");
            UUID playerUuid = (UUID) getUuid.invoke(playerRef);
            if (playerUuid == null) return;

            int playerLevel = resolvePlayerLevel(event);

            // Cache PlayerRef for message sending
            if (playerRef instanceof com.hypixel.hytale.server.core.universe.PlayerRef ref) {
                MessageUtil.cachePlayerRef(playerUuid, ref);
            }

            // Track XP gain
            Method getXp = event.getClass().getMethod("getXpAmount");
            Object xpResult = getXp.invoke(event);
            if (xpResult instanceof Number n) {
                double xpAmount = n.doubleValue();
                if (xpAmount > 0) {
                    questTracker.handleXPGained(playerUuid, xpAmount, playerLevel);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("XP event handling failed: {}", e.getMessage());
        }
    }

    public boolean isRegistered() { return registered; }

    /**
     * Resolves the player level from the event.
     */
    private int resolvePlayerLevel(@Nonnull Object event) {
        try {
            Method m = event.getClass().getMethod("getPlayerLevel");
            Object result = m.invoke(event);
            if (result instanceof Number n) return n.intValue();
        } catch (Exception ignored) {}
        return 1;
    }
}
