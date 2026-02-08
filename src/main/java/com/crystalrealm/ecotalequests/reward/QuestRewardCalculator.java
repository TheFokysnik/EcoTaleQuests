package com.crystalrealm.ecotalequests.reward;

import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.model.Quest;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Рассчитывает и выдаёт награды за выполненные квесты.
 *
 * <p>Использует Ecotale API ({@code com.ecotale.api.EcotaleAPI}) для депозита валюты.
 * Награда скейлится по уровню игрока.</p>
 */
public class QuestRewardCalculator {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestsConfig config;

    // Кешированные ссылки на методы EcotaleAPI (reflection)
    private static volatile boolean apiResolved = false;
    private static Method depositMethod;
    private static Method isAvailableMethod;

    public QuestRewardCalculator(@Nonnull QuestsConfig config) {
        this.config = config;
    }

    /**
     * Рассчитывает финальную награду за квест с учётом уровня.
     */
    public double calculateFinalReward(@Nonnull Quest quest, int playerLevel) {
        double baseCoins = quest.getReward().getBaseCoins();
        double levelMult = config.getRewards().getLevelMultiplier(playerLevel);
        return Math.round(baseCoins * levelMult * 100.0) / 100.0;
    }

    /**
     * Выдаёт награду игроку через Ecotale API (com.ecotale.api.EcotaleAPI).
     */
    public boolean grantReward(@Nonnull UUID playerUuid,
                               @Nonnull Quest quest,
                               int playerLevel) {
        resolveApi();

        if (!isEcotaleAvailable()) {
            LOGGER.error("Ecotale API is not available — cannot grant quest reward!");
            return false;
        }

        double finalAmount = calculateFinalReward(quest, playerLevel);

        if (finalAmount < 0.01) {
            LOGGER.warn("Quest reward too small ({}) for player {}", finalAmount, playerUuid);
            return false;
        }

        try {
            String reason = "Quest: " + quest.getName() + " (" + quest.getShortId() + ")";
            Object result = depositMethod.invoke(null, playerUuid, finalAmount, reason);
            if (result instanceof Boolean success && success) {
                LOGGER.info("Granted {} currency to {} for quest {} ({})",
                        finalAmount, playerUuid, quest.getShortId(), quest.getName());
                return true;
            } else {
                LOGGER.error("Deposit returned false for quest reward (player={}, amount={})",
                        playerUuid, finalAmount);
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

    // ═════════════════════════════════════════════════════════════
    //  ECOTALE API REFLECTION (com.ecotale.api.EcotaleAPI)
    // ═════════════════════════════════════════════════════════════

    private static void resolveApi() {
        if (apiResolved) return;
        try {
            Class<?> clazz = Class.forName("com.ecotale.api.EcotaleAPI");
            depositMethod = clazz.getMethod("deposit", UUID.class, double.class, String.class);
            isAvailableMethod = clazz.getMethod("isAvailable");
            LOGGER.info("Ecotale API (com.ecotale.api) resolved for quest rewards.");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Ecotale API class not found — quest rewards will not work!");
        } catch (Exception e) {
            LOGGER.error("Failed to resolve Ecotale API methods: {}", e.getMessage());
        }
        apiResolved = true;
    }

    private static boolean isEcotaleAvailable() {
        if (isAvailableMethod == null) return false;
        try {
            Object result = isAvailableMethod.invoke(null);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            return false;
        }
    }
}
