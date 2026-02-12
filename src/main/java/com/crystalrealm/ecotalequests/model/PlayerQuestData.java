package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Прогресс конкретного игрока по конкретному квесту.
 *
 * <p>Создаётся когда игрок принимает квест. Хранит текущий прогресс,
 * статус и время принятия.</p>
 */
public class PlayerQuestData {

    private final UUID playerUuid;
    private final UUID questId;
    private QuestStatus status;
    private double currentProgress;
    private final long acceptedAt;
    private long completedAt;

    public PlayerQuestData(@Nonnull UUID playerUuid,
                           @Nonnull UUID questId,
                           @Nonnull QuestStatus status,
                           double currentProgress,
                           long acceptedAt,
                           long completedAt) {
        this.playerUuid = playerUuid;
        this.questId = questId;
        this.status = status;
        this.currentProgress = currentProgress;
        this.acceptedAt = acceptedAt;
        this.completedAt = completedAt;
    }

    /**
     * Создаёт новые данные для только что принятого квеста.
     */
    public static PlayerQuestData create(@Nonnull UUID playerUuid, @Nonnull UUID questId) {
        return new PlayerQuestData(
                playerUuid, questId,
                QuestStatus.ACTIVE,
                0.0,
                System.currentTimeMillis(),
                0
        );
    }

    // ── Getters ─────────────────────────────────────────────────

    @Nonnull public UUID getPlayerUuid() { return playerUuid; }
    @Nonnull public UUID getQuestId() { return questId; }
    @Nonnull public QuestStatus getStatus() { return status; }
    public double getCurrentProgress() { return currentProgress; }
    public long getAcceptedAt() { return acceptedAt; }
    public long getCompletedAt() { return completedAt; }

    // ── Mutators ────────────────────────────────────────────────

    public void setStatus(@Nonnull QuestStatus status) {
        this.status = status;
    }

    /**
     * Увеличивает прогресс на указанную величину.
     *
     * @param amount количество прогресса
     * @param requiredAmount цель квеста
     * @return true если квест завершён
     */
    public boolean addProgress(double amount, double requiredAmount) {
        if (!status.isTrackable()) return false;

        this.currentProgress += amount;
        if (this.currentProgress >= requiredAmount) {
            this.currentProgress = requiredAmount;
            this.status = QuestStatus.COMPLETED;
            this.completedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * Прогресс в процентах (0.0–1.0).
     */
    public double getProgressPercent(double requiredAmount) {
        if (requiredAmount <= 0) return 1.0;
        return Math.min(currentProgress / requiredAmount, 1.0);
    }

    /**
     * Помечает квест как отменённый.
     */
    public void abandon() {
        this.status = QuestStatus.ABANDONED;
    }

    /**
     * Помечает квест как просроченный (пул daily/weekly истёк).
     */
    public void expire() {
        if (this.status == QuestStatus.ACTIVE) {
            this.status = QuestStatus.EXPIRED;
        }
    }

    /**
     * Помечает квест как проваленный (таймер выполнения истёк).
     */
    public void fail() {
        if (this.status == QuestStatus.ACTIVE) {
            this.status = QuestStatus.FAILED;
            this.completedAt = System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "PlayerQuestData{player=" + playerUuid +
                ", quest=" + questId +
                ", status=" + status +
                ", progress=" + currentProgress + '}';
    }
}
