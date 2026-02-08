package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.model.QuestType;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.event.EventRegistry;
import org.zuxaw.plugin.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Слушатель убийств мобов для обновления квестов типа KILL_MOB.
 *
 * <p>Интегрируется через RPG Leveling API — тот же паттерн,
 * что в EcoTaleIncome.</p>
 */
public class MobKillQuestListener {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestTracker questTracker;
    private boolean registered = false;

    public MobKillQuestListener(@Nonnull QuestTracker questTracker) {
        this.questTracker = questTracker;
    }

    /**
     * Регистрирует слушатель.
     *
     * @param rpgApi RPG Leveling API (может быть null)
     */
    public void register(@Nullable RPGLevelingAPI rpgApi) {
        if (rpgApi == null) {
            LOGGER.warn("RPG Leveling API not available — mob kill quest tracking disabled.");
            LOGGER.warn("Quests of type KILL_MOB will not track progress without RPG Leveling.");
            return;
        }

        rpgApi.registerExperienceGainedListener(event -> {
            com.hypixel.hytale.server.core.universe.PlayerRef playerRef = event.getPlayer();
            UUID playerUuid = playerRef.getUuid();
            int playerLevel = resolvePlayerLevel(event);

            // Кешируем PlayerRef для отправки сообщений
            MessageUtil.cachePlayerRef(playerUuid, playerRef);

            // ── GAIN_XP: track all XP gains ──
            double xpAmount = event.getXpAmount();
            if (xpAmount > 0) {
                questTracker.handleXPGained(playerUuid, xpAmount, playerLevel);
            }

            // ── KILL_MOB: track mob kills ──
            if (!event.getSource().equals(XPSource.ENTITY_KILL)) return;

            EntityKillContext killCtx = event.getEntityKillContext();
            if (killCtx == null) return;

            String entityType = resolveEntityName(killCtx);

            questTracker.handleAction(playerUuid, QuestType.KILL_MOB, entityType, 1, playerLevel);
        });

        registered = true;
        LOGGER.info("MobKillQuestListener registered via RPG Leveling API.");
    }

    public boolean isRegistered() { return registered; }

    /**
     * Определяет имя убитой сущности через reflection.
     */
    private String resolveEntityName(EntityKillContext killCtx) {
        // getEntityName()
        try {
            java.lang.reflect.Method m = killCtx.getClass().getMethod("getEntityName");
            Object result = m.invoke(killCtx);
            if (result instanceof String s && !s.isEmpty()) return s;
        } catch (Exception ignored) {}

        // getEntityType()
        try {
            java.lang.reflect.Method m = killCtx.getClass().getMethod("getEntityType");
            Object result = m.invoke(killCtx);
            if (result instanceof String s && !s.isEmpty()) return s;
            if (result != null) return result.toString();
        } catch (Exception ignored) {}

        // getPrefabName()
        try {
            java.lang.reflect.Method m = killCtx.getClass().getMethod("getPrefabName");
            Object result = m.invoke(killCtx);
            if (result instanceof String s && !s.isEmpty()) return s;
        } catch (Exception ignored) {}

        return "mob";
    }

    /**
     * Определяет уровень игрока из события.
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
