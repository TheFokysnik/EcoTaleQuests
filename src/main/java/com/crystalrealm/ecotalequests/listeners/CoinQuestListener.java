package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.provider.economy.EconomyBridge;
import com.crystalrealm.ecotalequests.provider.leveling.LevelBridge;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.server.core.HytaleServer;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Слушатель экономических операций для квестов типа EARN_COINS.
 *
 * <p>Использует polling-подход: каждые 2 секунды проверяет баланс
 * через {@link EconomyBridge}. Если баланс вырос — разница
 * засчитывается как заработанная валюта.</p>
 */
public class CoinQuestListener {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final long POLL_INTERVAL_SECONDS = 2;

    private final QuestTracker questTracker;
    private final EconomyBridge economyBridge;
    private final LevelBridge levelBridge;
    private final Map<UUID, Double> lastBalance = new ConcurrentHashMap<>();
    private boolean registered = false;
    private ScheduledFuture<?> pollTask;

    public CoinQuestListener(@Nonnull QuestTracker questTracker,
                             @Nonnull EconomyBridge economyBridge,
                             @Nonnull LevelBridge levelBridge) {
        this.questTracker = questTracker;
        this.economyBridge = economyBridge;
        this.levelBridge = levelBridge;
    }

    /**
     * Регистрирует polling-слушатель через EconomyBridge.
     */
    public void register() {
        if (!economyBridge.isAvailable()) {
            LOGGER.warn("Economy provider not available — coin quest tracking disabled.");
            LOGGER.info("Will check availability on first poll.");
        }

        // Запускаем polling
        pollTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::pollBalances,
                POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS
        );

        registered = true;
        LOGGER.info("CoinQuestListener registered (polling every {}s via {}).",
                POLL_INTERVAL_SECONDS, economyBridge.getProviderName());
    }

    /**
     * Проверяет балансы всех кешированных игроков.
     */
    private void pollBalances() {
        if (!economyBridge.isAvailable()) return;

        for (UUID uuid : MessageUtil.getCachedPlayerUuids()) {
            try {
                double currentBalance = economyBridge.getBalance(uuid);
                if (currentBalance < 0) continue; // ошибка получения баланса

                Double previousBalance = lastBalance.get(uuid);
                if (previousBalance == null) {
                    // Первый раз видим игрока — запоминаем баланс
                    lastBalance.put(uuid, currentBalance);
                    continue;
                }

                if (currentBalance > previousBalance) {
                    double earned = currentBalance - previousBalance;
                    int playerLevel = levelBridge.getPlayerLevel(uuid);
                    questTracker.handleCoinsEarned(uuid, earned, playerLevel);
                }

                // Всегда обновляем баланс (может уменьшиться при покупках)
                lastBalance.put(uuid, currentBalance);
            } catch (Exception e) {
                LOGGER.debug("Error polling balance for {}: {}", uuid, e.getMessage());
            }
        }
    }

    public boolean isRegistered() { return registered; }

    /**
     * Останавливает polling.
     */
    public void shutdown() {
        if (pollTask != null) {
            pollTask.cancel(false);
        }
        lastBalance.clear();
    }
}
