package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;

/**
 * Награда за выполнение квеста.
 */
public class QuestReward {

    /** Базовая сумма монет (до скейлинга). */
    private final double baseCoins;

    /** Бонусный XP (если RPG Leveling доступен). */
    private final int bonusXp;

    public QuestReward(double baseCoins, int bonusXp) {
        this.baseCoins = baseCoins;
        this.bonusXp = bonusXp;
    }

    public QuestReward(double baseCoins) {
        this(baseCoins, 0);
    }

    public double getBaseCoins() { return baseCoins; }
    public int getBonusXp() { return bonusXp; }

    /**
     * Применяет скейлинг на основе уровня игрока.
     *
     * @param levelMultiplier множитель от уровня (например, 1.0 + level * 0.02)
     * @param vipMultiplier   VIP/группа-множитель
     * @return скейленная сумма монет
     */
    public double calculateFinalCoins(double levelMultiplier, double vipMultiplier) {
        return Math.round(baseCoins * levelMultiplier * vipMultiplier * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return "QuestReward{coins=" + baseCoins + ", xp=" + bonusXp + '}';
    }
}
