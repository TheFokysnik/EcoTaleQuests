package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Ранговые данные игрока — текущий ранг, очки, статистика.
 *
 * <p>Хранится в storage, обновляется при выполнении/провале квестов.</p>
 */
public class PlayerRankData {

    private final UUID playerUuid;
    private int rankPoints;
    private int totalCompleted;
    private int totalFailed;

    public PlayerRankData(@Nonnull UUID playerUuid, int rankPoints,
                          int totalCompleted, int totalFailed) {
        this.playerUuid = playerUuid;
        this.rankPoints = rankPoints;
        this.totalCompleted = totalCompleted;
        this.totalFailed = totalFailed;
    }

    /** Создаёт новые данные для нового игрока (ранг E, 0 очков). */
    public static PlayerRankData createNew(@Nonnull UUID playerUuid) {
        return new PlayerRankData(playerUuid, 0, 0, 0);
    }

    // ── Getters ─────────────────────────────────────────────────

    @Nonnull public UUID getPlayerUuid() { return playerUuid; }
    public int getRankPoints() { return rankPoints; }
    public int getTotalCompleted() { return totalCompleted; }
    public int getTotalFailed() { return totalFailed; }

    /** Вычисляет текущий ранг на основе очков. */
    @Nonnull
    public QuestRank getRank() {
        return QuestRank.fromPoints(rankPoints);
    }

    // ── Mutations ───────────────────────────────────────────────

    /**
     * Добавляет очки за выполнение квеста.
     *
     * @param points очки (положительные)
     * @return true если ранг повысился
     */
    public boolean addPoints(int points) {
        if (points <= 0) return false;
        QuestRank oldRank = getRank();
        this.rankPoints += points;
        this.totalCompleted++;
        return getRank() != oldRank;
    }

    /**
     * Штраф за провал квеста (очки не уходят ниже 0, и ранг не понижается ниже E).
     *
     * @param penalty штрафные очки (положительные)
     * @return true если ранг понизился
     */
    public boolean penalize(int penalty) {
        if (penalty <= 0) return false;
        QuestRank oldRank = getRank();
        this.rankPoints = Math.max(0, this.rankPoints - penalty);
        this.totalFailed++;
        return getRank() != oldRank;
    }

    /**
     * Очки, необходимые до следующего ранга.
     */
    public int pointsToNextRank() {
        return getRank().pointsToNext(rankPoints);
    }

    /**
     * Прогресс к следующему рангу (0.0 – 1.0).
     */
    public double progressToNextRank() {
        QuestRank current = getRank();
        QuestRank next = current.next();
        if (next == null) return 1.0; // максимальный ранг
        int range = next.getRequiredPoints() - current.getRequiredPoints();
        if (range <= 0) return 1.0;
        int progress = rankPoints - current.getRequiredPoints();
        return Math.min(1.0, (double) progress / range);
    }

    @Override
    public String toString() {
        return "PlayerRankData{uuid=" + playerUuid +
                ", rank=" + getRank().getId() +
                ", points=" + rankPoints +
                ", completed=" + totalCompleted +
                ", failed=" + totalFailed + '}';
    }
}
