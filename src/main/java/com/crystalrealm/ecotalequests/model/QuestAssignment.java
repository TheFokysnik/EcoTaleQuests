package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Запись о назначении квеста игроку.
 *
 * <p>Используется для GLOBAL_UNIQUE и LIMITED_SLOTS квестов,
 * чтобы трекать кто какой квест взял и когда он должен быть завершён.</p>
 */
public class QuestAssignment {

    private final UUID questId;
    private final UUID playerUuid;
    private final long assignedAt;
    private final long expiresAt;
    private boolean released;

    public QuestAssignment(@Nonnull UUID questId,
                           @Nonnull UUID playerUuid,
                           long assignedAt,
                           long expiresAt) {
        this.questId = questId;
        this.playerUuid = playerUuid;
        this.assignedAt = assignedAt;
        this.expiresAt = expiresAt;
        this.released = false;
    }

    /**
     * Создаёт новое назначение с текущим временем.
     *
     * @param questId       ID квеста
     * @param playerUuid    UUID игрока
     * @param durationMinutes продолжительность (0 = без таймера)
     */
    public static QuestAssignment create(@Nonnull UUID questId,
                                         @Nonnull UUID playerUuid,
                                         int durationMinutes) {
        long now = System.currentTimeMillis();
        long expires = durationMinutes > 0
                ? now + (long) durationMinutes * 60_000L
                : Long.MAX_VALUE;
        return new QuestAssignment(questId, playerUuid, now, expires);
    }

    // ── Getters ─────────────────────────────────────────────────

    @Nonnull public UUID getQuestId() { return questId; }
    @Nonnull public UUID getPlayerUuid() { return playerUuid; }
    public long getAssignedAt() { return assignedAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isReleased() { return released; }

    /** Проверяет, истёк ли таймер назначения. */
    public boolean isTimerExpired() {
        return expiresAt != Long.MAX_VALUE && System.currentTimeMillis() > expiresAt;
    }

    /** Оставшееся время в миллисекундах (0 если без таймера или истёк). */
    public long getRemainingMs() {
        if (expiresAt == Long.MAX_VALUE) return Long.MAX_VALUE;
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /** Оставшееся время в минутах (округлённое вверх). */
    public int getRemainingMinutes() {
        long ms = getRemainingMs();
        if (ms == Long.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.ceil(ms / 60_000.0);
    }

    /** Помечает назначение как освобождённое (квест возвращён на доску). */
    public void release() {
        this.released = true;
    }

    @Override
    public String toString() {
        return "QuestAssignment{quest=" + questId +
                ", player=" + playerUuid +
                ", remaining=" + getRemainingMinutes() + "m}";
    }
}
