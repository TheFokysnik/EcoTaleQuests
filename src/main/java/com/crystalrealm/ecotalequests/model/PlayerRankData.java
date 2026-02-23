package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    /** Per-rank completion count: rank id (E, D, C, ...) -> count. */
    private final Map<String, Integer> completedByRank;
    /** Last known player display name (for leaderboard). */
    @Nullable
    private String lastKnownName;

    public PlayerRankData(@Nonnull UUID playerUuid, int rankPoints,
                          int totalCompleted, int totalFailed) {
        this(playerUuid, rankPoints, totalCompleted, totalFailed, null);
    }

    public PlayerRankData(@Nonnull UUID playerUuid, int rankPoints,
                          int totalCompleted, int totalFailed,
                          Map<String, Integer> completedByRank) {
        this.playerUuid = playerUuid;
        this.rankPoints = rankPoints;
        this.totalCompleted = totalCompleted;
        this.totalFailed = totalFailed;
        this.completedByRank = completedByRank != null ? new HashMap<>(completedByRank) : new HashMap<>();
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

    /** Returns unmodifiable map of completed quests per rank. */
    @Nonnull
    public Map<String, Integer> getCompletedByRank() {
        return Collections.unmodifiableMap(completedByRank);
    }

    /**
     * Increments the completed count for a specific quest rank.
     *
     * @param rankId the rank ID (e.g. "E", "D", "C")
     */
    public void incrementCompletedForRank(@Nonnull String rankId) {
        completedByRank.merge(rankId, 1, Integer::sum);
    }

    /** Returns last known player display name (may be null for old data). */
    @Nullable
    public String getLastKnownName() { return lastKnownName; }

    /** Updates the player's last known display name. */
    public void setLastKnownName(@Nullable String name) { this.lastKnownName = name; }

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
                ", failed=" + totalFailed +
                ", byRank=" + completedByRank + '}';
    }
}
