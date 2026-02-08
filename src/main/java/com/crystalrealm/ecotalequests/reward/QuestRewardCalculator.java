package com.crystalrealm.ecotalequests.reward;

import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.model.Quest;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.crystalrealm.ecotale.api.EcoTaleAPI;
import com.crystalrealm.ecotale.api.economy.EconomyService;
import com.crystalrealm.ecotale.api.economy.TransactionContext;
import com.crystalrealm.ecotale.api.economy.TransactionResult;
import com.crystalrealm.ecotale.api.economy.TransactionSource;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Рассчитывает и выдаёт награды за выполненные квесты.
 *
 * <p>Использует EcoTale API для депозита монет. Награда скейлится
 * по уровню игрока и VIP-множителям.</p>
 */
public class QuestRewardCalculator {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final String PLUGIN_NAME = "EcoTaleQuests";

    private final QuestsConfig config;

    public QuestRewardCalculator(@Nonnull QuestsConfig config) {
        this.config = config;
    }

    /**
     * Рассчитывает финальную награду за квест с учётом уровня.
     *
     * @param quest        выполненный квест
     * @param playerLevel  уровень игрока
     * @return финальная сумма монет
     */
    public double calculateFinalReward(@Nonnull Quest quest, int playerLevel) {
        double baseCoins = quest.getReward().getBaseCoins();
        double levelMult = config.getRewards().getLevelMultiplier(playerLevel);

        // VIP-множитель через EcoTale API
        double vipMult = 1.0;
        if (EcoTaleAPI.isAvailable()) {
            try {
                vipMult = EcoTaleAPI.get().getMultipliers()
                        .getMultiplier(UUID.randomUUID()); // будет переопределён при вызове
            } catch (Exception ignored) {
                // API может быть недоступен при расчёте
            }
        }

        return Math.round(baseCoins * levelMult * 100.0) / 100.0;
    }

    /**
     * Выдаёт награду игроку через EcoTale API.
     *
     * @param playerUuid UUID игрока
     * @param quest      выполненный квест
     * @param playerLevel уровень игрока
     * @return true если награда успешно выдана
     */
    public boolean grantReward(@Nonnull UUID playerUuid,
                               @Nonnull Quest quest,
                               int playerLevel) {
        if (!EcoTaleAPI.isAvailable()) {
            LOGGER.error("EcoTale API is not available — cannot grant quest reward!");
            return false;
        }

        double finalAmount = calculateFinalReward(quest, playerLevel);

        // VIP-множитель для конкретного игрока
        try {
            double vipMult = EcoTaleAPI.get().getMultipliers().getMultiplier(playerUuid);
            finalAmount = Math.round(finalAmount * vipMult * 100.0) / 100.0;
        } catch (Exception e) {
            LOGGER.debug("Could not get VIP multiplier for {}: {}", playerUuid, e.getMessage());
        }

        if (finalAmount < 0.01) {
            LOGGER.warn("Quest reward too small ({}) for player {}", finalAmount, playerUuid);
            return false;
        }

        try {
            EconomyService economy = EcoTaleAPI.get().getEconomy();
            TransactionContext ctx = TransactionContext.ofPlugin(
                    TransactionSource.QUEST,
                    "Quest completed: " + quest.getName() + " (" + quest.getShortId() + ")",
                    PLUGIN_NAME
            );

            TransactionResult result = economy.deposit(playerUuid, finalAmount, ctx);

            if (result.getStatus() == TransactionResult.Status.SUCCESS) {
                LOGGER.info("Granted {} coins to {} for quest {} ({})",
                        finalAmount, playerUuid, quest.getShortId(), quest.getName());
                return true;
            } else {
                LOGGER.error("Failed to deposit quest reward: {}", result.getStatus());
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error granting quest reward to " + playerUuid, e);
            return false;
        }
    }

    /**
     * Рассчитывает бонусный XP за квест.
     */
    public int calculateBonusXP(@Nonnull Quest quest, int playerLevel) {
        int baseXp = quest.getReward().getBonusXp();
        double levelMult = config.getRewards().getLevelMultiplier(playerLevel);
        return (int) Math.round(baseXp * levelMult);
    }
}
