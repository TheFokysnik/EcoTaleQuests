package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Полная модель квеста. Каждый квест имеет уникальный UUID.
 *
 * <p>Квест создаётся генератором и добавляется в пул доступных квестов.
 * Когда игрок принимает квест, создаётся {@link PlayerQuestData}
 * для отслеживания прогресса.</p>
 */
public class Quest {

    private final UUID questId;
    private final String name;
    private final String description;
    private final QuestPeriod period;
    private final QuestObjective objective;
    private final QuestReward reward;

    /** Минимальный уровень игрока для принятия (0 = без ограничений). */
    private final int minLevel;

    /** Эпоха создания (мс). */
    private final long createdAt;

    /** Эпоха истечения (мс). */
    private final long expiresAt;

    public Quest(@Nonnull UUID questId,
                 @Nonnull String name,
                 @Nonnull String description,
                 @Nonnull QuestPeriod period,
                 @Nonnull QuestObjective objective,
                 @Nonnull QuestReward reward,
                 int minLevel,
                 long createdAt,
                 long expiresAt) {
        this.questId = questId;
        this.name = name;
        this.description = description;
        this.period = period;
        this.objective = objective;
        this.reward = reward;
        this.minLevel = minLevel;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // ── Getters ─────────────────────────────────────────────────

    @Nonnull public UUID getQuestId() { return questId; }
    @Nonnull public String getName() { return name; }
    @Nonnull public String getDescription() { return description; }
    @Nonnull public QuestPeriod getPeriod() { return period; }
    @Nonnull public QuestObjective getObjective() { return objective; }
    @Nonnull public QuestReward getReward() { return reward; }
    public int getMinLevel() { return minLevel; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }

    /** Проверяет, истёк ли квест. */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /** Краткий ID для отображения (первые 8 символов UUID). */
    @Nonnull
    public String getShortId() {
        return questId.toString().substring(0, 8);
    }

    @Override
    public String toString() {
        return "Quest{" + getShortId() + " '" + name + "' " + period + " " + objective + '}';
    }
}
