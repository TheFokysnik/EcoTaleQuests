package com.crystalrealm.ecotalequests.service;

import com.crystalrealm.ecotalequests.model.QuestAssignment;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Сервис таймеров квестов.
 *
 * <p>Отслеживает обратный отсчёт для квестов с durationMinutes > 0.
 * Вызывается периодически из планировщика (каждые 10 секунд).
 * При истечении таймера вызывает callback для провала квеста.</p>
 */
public class TimerService {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    /**
     * Активные таймеры: составной ключ (questId:playerUuid) → QuestAssignment
     */
    private final Map<String, QuestAssignment> activeTimers = new ConcurrentHashMap<>();

    /** Callback при истечении: (questId, playerUuid) → fail quest */
    private BiConsumer<UUID, UUID> onTimerExpired;

    /** Grace-период после релога (мс). По умолчанию 60 секунд. */
    private long relogGracePeriodMs = 60_000L;

    /** Игроки в grace-периоде: playerUuid → время релога */
    private final Map<UUID, Long> relogGrace = new ConcurrentHashMap<>();

    public TimerService() {}

    /**
     * Устанавливает callback для обработки истечения таймера.
     */
    public void setOnTimerExpired(@Nonnull BiConsumer<UUID, UUID> callback) {
        this.onTimerExpired = callback;
    }

    /**
     * Устанавливает grace-период для релога.
     */
    public void setRelogGracePeriodMs(long ms) {
        this.relogGracePeriodMs = ms;
    }

    // ═════════════════════════════════════════════════════════════
    //  TIMER MANAGEMENT
    // ═════════════════════════════════════════════════════════════

    /**
     * Регистрирует таймер для назначения квеста.
     */
    public void registerTimer(@Nonnull QuestAssignment assignment) {
        if (assignment.getExpiresAt() == Long.MAX_VALUE) return; // без таймера
        String key = timerKey(assignment.getQuestId(), assignment.getPlayerUuid());
        activeTimers.put(key, assignment);
        LOGGER.debug("Timer registered: {} (expires in {} min)",
                key, assignment.getRemainingMinutes());
    }

    /**
     * Восстанавливает таймеры из персистентных назначений после рестарта сервера.
     * Вызывается один раз при старте плагина после загрузки assignments из хранилища.
     *
     * @param assignments список активных (не released, не expired) QuestAssignment
     * @return количество восстановленных таймеров
     */
    public int restoreTimers(@Nonnull List<QuestAssignment> assignments) {
        int restored = 0;
        for (QuestAssignment a : assignments) {
            if (a.isReleased() || a.isTimerExpired()) continue;
            if (a.getExpiresAt() == Long.MAX_VALUE) continue;
            String key = timerKey(a.getQuestId(), a.getPlayerUuid());
            activeTimers.put(key, a);
            restored++;
        }
        if (restored > 0) {
            LOGGER.info("Restored {} quest timers from persistent storage.", restored);
        }
        return restored;
    }

    /**
     * Удаляет таймер (при успешном завершении, отмене).
     */
    public void removeTimer(@Nonnull UUID questId, @Nonnull UUID playerUuid) {
        String key = timerKey(questId, playerUuid);
        activeTimers.remove(key);
    }

    /**
     * Проверяет все таймеры и обрабатывает истёкшие.
     * Вызывается периодически из планировщика.
     */
    public void tick() {
        if (onTimerExpired == null) return;

        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();

        for (Map.Entry<String, QuestAssignment> entry : activeTimers.entrySet()) {
            QuestAssignment assignment = entry.getValue();
            if (assignment.isReleased()) {
                expired.add(entry.getKey());
                continue;
            }

            if (assignment.isTimerExpired()) {
                UUID playerUuid = assignment.getPlayerUuid();

                // Проверяем grace-период для релога
                Long graceStart = relogGrace.get(playerUuid);
                if (graceStart != null) {
                    if (now - graceStart < relogGracePeriodMs) {
                        continue; // ещё в grace-периоде, не фейлим
                    }
                    relogGrace.remove(playerUuid); // grace закончился
                }

                LOGGER.info("Timer expired: quest={} player={}",
                        assignment.getQuestId(), playerUuid);
                try {
                    onTimerExpired.accept(assignment.getQuestId(), playerUuid);
                } catch (Exception e) {
                    LOGGER.error("Error handling timer expiry", e);
                }
                expired.add(entry.getKey());
            }
        }

        expired.forEach(activeTimers::remove);
    }

    // ═════════════════════════════════════════════════════════════
    //  RELOG HANDLING
    // ═════════════════════════════════════════════════════════════

    /**
     * Записывает время начала grace-периода при отключении игрока.
     */
    public void onPlayerDisconnect(@Nonnull UUID playerUuid) {
        // Проверяем, есть ли активные таймеры для этого игрока
        boolean hasTimers = activeTimers.values().stream()
                .anyMatch(a -> a.getPlayerUuid().equals(playerUuid) && !a.isReleased());
        if (hasTimers) {
            relogGrace.put(playerUuid, System.currentTimeMillis());
            LOGGER.debug("Relog grace started for player {}", playerUuid);
        }
    }

    /**
     * Снимает grace-период при повторном подключении.
     */
    public void onPlayerConnect(@Nonnull UUID playerUuid) {
        relogGrace.remove(playerUuid);
    }

    // ═════════════════════════════════════════════════════════════
    //  QUERIES
    // ═════════════════════════════════════════════════════════════

    /**
     * Возвращает оставшееся время для квеста игрока (в секундах).
     *
     * @return секунды, -1 если нет таймера
     */
    public long getRemainingSeconds(@Nonnull UUID questId, @Nonnull UUID playerUuid) {
        String key = timerKey(questId, playerUuid);
        QuestAssignment a = activeTimers.get(key);
        if (a == null || a.getExpiresAt() == Long.MAX_VALUE) return -1;
        return Math.max(0, (a.getExpiresAt() - System.currentTimeMillis()) / 1000);
    }

    /**
     * Форматирует оставшееся время для отображения в GUI.
     *
     * @return строка вида "12:34" или "0:45" или "--:--"
     */
    @Nonnull
    public String formatRemainingTime(@Nonnull UUID questId, @Nonnull UUID playerUuid) {
        long seconds = getRemainingSeconds(questId, playerUuid);
        if (seconds < 0) return "--:--";
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }

    /**
     * Количество активных таймеров.
     */
    public int getActiveTimerCount() {
        return activeTimers.size();
    }

    // ═════════════════════════════════════════════════════════════
    //  SHUTDOWN
    // ═════════════════════════════════════════════════════════════

    public void shutdown() {
        activeTimers.clear();
        relogGrace.clear();
    }

    // ═════════════════════════════════════════════════════════════
    //  INTERNAL
    // ═════════════════════════════════════════════════════════════

    private static String timerKey(@Nonnull UUID questId, @Nonnull UUID playerUuid) {
        return questId.toString() + ":" + playerUuid.toString();
    }
}
