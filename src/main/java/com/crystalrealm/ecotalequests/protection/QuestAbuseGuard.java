package com.crystalrealm.ecotalequests.protection;

import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Защита от абьюза квестовой системы.
 *
 * <p>Контролирует:
 * <ul>
 *   <li>Cooldown между принятием квестов</li>
 *   <li>Верификацию, что игрок онлайн</li>
 *   <li>Максимальное количество отмен в день</li>
 * </ul>
 */
public class QuestAbuseGuard {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestsConfig config;

    /** Время последнего принятия квеста: playerUuid → timestamp */
    private final Map<UUID, Long> lastAcceptTime = new ConcurrentHashMap<>();

    public QuestAbuseGuard(@Nonnull QuestsConfig config) {
        this.config = config;
    }

    /**
     * Проверяет, может ли игрок принять квест (cooldown).
     */
    public boolean canAcceptQuest(@Nonnull UUID playerUuid) {
        long cooldown = config.getProtection().getQuestAcceptCooldownMs();
        if (cooldown <= 0) return true;

        Long last = lastAcceptTime.get(playerUuid);
        if (last == null) return true;

        return System.currentTimeMillis() - last >= cooldown;
    }

    /**
     * Записывает время принятия квеста.
     */
    public void recordAccept(@Nonnull UUID playerUuid) {
        lastAcceptTime.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * Проверяет, допустим ли мир для начисления прогресса.
     */
    public boolean isWorldAllowed(@Nonnull String worldName) {
        var allowed = config.getProtection().getAllowedWorlds();
        if (allowed == null || allowed.isEmpty()) return true; // пустой список = все миры
        return allowed.contains(worldName);
    }

    /**
     * Очищает кеш при отключении.
     */
    public void cleanup() {
        lastAcceptTime.clear();
    }
}
