package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import org.zuxaw.plugin.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Listener RPGLevelingAPI for XP tracking.
 *
 * <p>Tracks GAIN_XP quests via ExperienceGainedEvent.
 * <b>KILL_MOB quests are handled separately by {@link MobDeathQuestSystem}</b>,
 * which uses the native ECS DeathSystems.OnDeathSystem
 * with direct access to NPCEntity.getNPCTypeId().</p>
 *
 * <p>Before v1.1.0 this class also attempted to track mob kills,
 * but EntityKillContext contains no entity type info -
 * only UUID and level.</p>
 */
public class MobKillQuestListener {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestTracker questTracker;
    private boolean registered = false;

    public MobKillQuestListener(@Nonnull QuestTracker questTracker) {
        this.questTracker = questTracker;
    }

    /**
     * Registers the XP event listener.
     *
     * @param rpgApi RPG Leveling API (may be null)
     */
    public void register(@Nullable RPGLevelingAPI rpgApi) {
        if (rpgApi == null) {
            LOGGER.warn("RPG Leveling API not available - XP quest tracking disabled.");
            LOGGER.warn("Quests of type GAIN_XP will not track progress without RPG Leveling.");
            LOGGER.info("KILL_MOB quests use native DeathSystem - no RPG API required.");
            return;
        }

        rpgApi.registerExperienceGainedListener(event -> {
            com.hypixel.hytale.server.core.universe.PlayerRef playerRef = event.getPlayer();
            UUID playerUuid = playerRef.getUuid();
            int playerLevel = resolvePlayerLevel(event);

            // Cache PlayerRef for message sending
            MessageUtil.cachePlayerRef(playerUuid, playerRef);

            // -- GAIN_XP: track all XP gains --
            double xpAmount = event.getXpAmount();
            if (xpAmount > 0) {
                questTracker.handleXPGained(playerUuid, xpAmount, playerLevel);
            }

            // Note: KILL_MOB is NO LONGER handled here.
            // EntityKillContext has no entity type info (only UUID + level).
            // MobDeathQuestSystem (DeathSystems.OnDeathSystem) handles
            // mob kills via NPCEntity.getNPCTypeId() for reliable identification.
        });

        registered = true;
        LOGGER.info("MobKillQuestListener registered via RPG Leveling API (XP tracking only).");
        LOGGER.info("KILL_MOB tracking delegated to MobDeathQuestSystem (native ECS).");
    }

    public boolean isRegistered() { return registered; }

    /**
     * Resolves the player level from the event.
     */
    private int resolvePlayerLevel(ExperienceGainedEvent event) {
        try {
            java.lang.reflect.Method m = event.getClass().getMethod("getPlayerLevel");
            Object result = m.invoke(event);
            if (result instanceof Number n) return n.intValue();
        } catch (Exception ignored) {}
        return 1;
    }
}
