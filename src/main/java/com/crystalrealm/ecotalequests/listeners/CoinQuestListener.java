package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.server.core.HytaleServer;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Слушатель экономических операций для квестов типа EARN_COINS.
 *
 * <p>Использует polling-подход: каждые 2 секунды проверяет баланс
 * через старый {@code com.ecotale.api.EcotaleAPI.getBalance(UUID)}.
 * Если баланс вырос — разница засчитывается как заработанная валюта.</p>
 */
public class CoinQuestListener {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final long POLL_INTERVAL_SECONDS = 2;

    private final QuestTracker questTracker;
    private final Map<UUID, Double> lastBalance = new ConcurrentHashMap<>();
    private boolean registered = false;
    private ScheduledFuture<?> pollTask;

    // Кешированные ссылки на reflection
    private Method getBalanceMethod;
    private Method isAvailableMethod;

    public CoinQuestListener(@Nonnull QuestTracker questTracker) {
        this.questTracker = questTracker;
    }

    /**
     * Регистрирует polling-слушатель через Ecotale API.
     */
    public void register() {
        try {
            Class<?> clazz = Class.forName("com.ecotale.api.EcotaleAPI");
            getBalanceMethod = clazz.getMethod("getBalance", UUID.class);
            isAvailableMethod = clazz.getMethod("isAvailable");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Ecotale API (com.ecotale.api) not found — coin quest tracking disabled.");
            return;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve Ecotale API methods: {}", e.getMessage());
            return;
        }

        // Проверяем доступность API (может быть ещё не инициализирован)
        if (!isApiAvailable()) {
            LOGGER.info("Ecotale API not yet available — will check on first poll.");
        }

        // Запускаем polling
        pollTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::pollBalances,
                POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS
        );

        registered = true;
        LOGGER.info("CoinQuestListener registered (polling every {}s via com.ecotale.api).",
                POLL_INTERVAL_SECONDS);
    }

    /**
     * Проверяет балансы всех кешированных игроков.
     */
    private void pollBalances() {
        if (!isApiAvailable()) return;

        for (UUID uuid : MessageUtil.getCachedPlayerUuids()) {
            try {
                double currentBalance = getBalance(uuid);
                if (currentBalance < 0) continue; // ошибка получения баланса

                Double previousBalance = lastBalance.get(uuid);
                if (previousBalance == null) {
                    // Первый раз видим игрока — запоминаем баланс
                    lastBalance.put(uuid, currentBalance);
                    continue;
                }

                if (currentBalance > previousBalance) {
                    double earned = currentBalance - previousBalance;
                    int playerLevel = resolvePlayerLevel(uuid);
                    questTracker.handleCoinsEarned(uuid, earned, playerLevel);
                }

                // Всегда обновляем баланс (может уменьшиться при покупках)
                lastBalance.put(uuid, currentBalance);
            } catch (Exception e) {
                LOGGER.debug("Error polling balance for {}: {}", uuid, e.getMessage());
            }
        }
    }

    private double getBalance(UUID uuid) {
        try {
            Object result = getBalanceMethod.invoke(null, uuid);
            if (result instanceof Number n) return n.doubleValue();
        } catch (Exception e) {
            LOGGER.debug("getBalance failed for {}: {}", uuid, e.getMessage());
        }
        return -1;
    }

    private boolean isApiAvailable() {
        if (isAvailableMethod == null) return false;
        try {
            Object result = isAvailableMethod.invoke(null);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            return false;
        }
    }

    private int resolvePlayerLevel(UUID playerUuid) {
        try {
            Class<?> rpgClass = Class.forName("org.zuxaw.plugin.api.RPGLevelingAPI");
            Object api = null;
            for (String methodName : new String[]{"get", "getInstance", "getAPI"}) {
                try {
                    Method m = rpgClass.getMethod(methodName);
                    api = m.invoke(null);
                    if (api != null) break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (api != null) {
                Method getLevel = api.getClass().getMethod("getPlayerLevel", UUID.class);
                Object level = getLevel.invoke(api, playerUuid);
                if (level instanceof Number n) return n.intValue();
            }
        } catch (Exception ignored) {}
        return 1;
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
