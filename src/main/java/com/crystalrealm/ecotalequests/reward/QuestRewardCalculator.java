package com.crystalrealm.ecotalequests.reward;

import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.model.Quest;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Рассчитывает и выдаёт награды за выполненные квесты.
 *
 * <p>Использует Ecotale API ({@code com.ecotale.api.EcotaleAPI}) для депозита валюты.
 * Награда скейлится по уровню игрока и VIP-тиру.</p>
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
     * Рассчитывает финальную награду за квест с учётом уровня и VIP-множителя.
     */
    public double calculateFinalReward(@Nonnull Quest quest, int playerLevel, double vipMultiplier) {
        double baseCoins = quest.getReward().getBaseCoins();
        double levelMult = config.getRewards().getLevelMultiplier(playerLevel);
        return Math.round(baseCoins * levelMult * vipMultiplier * 100.0) / 100.0;
    }

    /**
     * Рассчитывает финальную награду (без VIP).
     */
    public double calculateFinalReward(@Nonnull Quest quest, int playerLevel) {
        return calculateFinalReward(quest, playerLevel, 1.0);
    }

    /**
     * Выдаёт награду игроку через Ecotale API (com.ecotale.api.EcotaleAPI).
     *
     * @param playerUuid UUID игрока
     * @param quest      завершённый квест
     * @param playerLevel уровень игрока
     * @param vipMultiplier VIP-множитель (1.0 для обычных игроков)
     */
    public boolean grantReward(@Nonnull UUID playerUuid,
                               @Nonnull Quest quest,
                               int playerLevel,
                               double vipMultiplier) {
        resolveApi();

        if (!isEcotaleAvailable()) {
            LOGGER.error("Ecotale API is not available — cannot grant quest reward!");
            return false;
        }

        double finalAmount = calculateFinalReward(quest, playerLevel, vipMultiplier);

        if (finalAmount < 0.01) {
            LOGGER.warn("Quest reward too small ({}) for player {}", finalAmount, playerUuid);
            return false;
        }

        try {
            String reason = "Quest: " + quest.getName() + " (" + quest.getShortId() + ")";
            Object result = depositMethod.invoke(null, playerUuid, finalAmount, reason);
            if (result instanceof Boolean success && success) {
                LOGGER.info("Granted {} currency to {} for quest {} ({}) [VIP ×{}]",
                        finalAmount, playerUuid, quest.getShortId(), quest.getName(), vipMultiplier);
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
     * Рассчитывает бонусный XP за квест с учётом VIP.
     */
    public int calculateBonusXP(@Nonnull Quest quest, int playerLevel, double vipMultiplier) {
        int baseXp = quest.getReward().getBonusXp();
        double levelMult = config.getRewards().getLevelMultiplier(playerLevel);
        return (int) Math.round(baseXp * levelMult * vipMultiplier);
    }

    /**
     * Рассчитывает бонусный XP (без VIP).
     */
    public int calculateBonusXP(@Nonnull Quest quest, int playerLevel) {
        return calculateBonusXP(quest, playerLevel, 1.0);
    }

    // ═════════════════════════════════════════════════════════════
    //  VIP TIER RESOLUTION
    // ═════════════════════════════════════════════════════════════

    /**
     * Результат определения VIP-тира: множитель + отображаемое имя.
     */
    public record VipResult(double multiplier, @Nullable String displayName) {
        public static final VipResult NONE = new VipResult(1.0, null);
    }

    /**
     * Определяет VIP-множитель для игрока по его пермишенам.
     * Перебирает тиры сверху вниз (от высшего к низшему),
     * возвращает первый подходящий.
     *
     * @param playerUuid UUID игрока (ищем в кеше PlayerRef → Player.hasPermission)
     */
    public VipResult resolveVipMultiplier(@Nonnull UUID playerUuid) {
        List<QuestsConfig.VipTier> tiers = config.getVipTiers().getTiers();
        if (tiers == null || tiers.isEmpty()) {
            return VipResult.NONE;
        }

        try {
            // Получаем PlayerRef из кеша MessageUtil
            Object playerRef = getPlayerRefFromCache(playerUuid);
            if (playerRef == null) {
                LOGGER.debug("No cached PlayerRef for {} — VIP multiplier defaulting to 1.0", playerUuid);
                return VipResult.NONE;
            }

            // Пробуем получить hasPermission через PlayerRef или через Player
            for (QuestsConfig.VipTier tier : tiers) {
                if (checkPermissionViaReflection(playerRef, tier.getPermission())) {
                    LOGGER.debug("Player {} matched VIP tier {} (×{})",
                            playerUuid, tier.getDisplayName(), tier.getMultiplier());
                    return new VipResult(tier.getMultiplier(), tier.getDisplayName());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("VIP permission check failed for {}: {}", playerUuid, e.getMessage());
        }

        return VipResult.NONE;
    }

    /**
     * Получает PlayerRef из кеша MessageUtil через рефлексию.
     */
    private static Object getPlayerRefFromCache(UUID playerUuid) {
        try {
            Class<?> clazz = Class.forName("com.crystalrealm.ecotalequests.util.MessageUtil");
            var field = clazz.getDeclaredField("PLAYER_REF_CACHE");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cache = (java.util.Map<UUID, Object>) field.get(null);
            return cache.get(playerUuid);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Проверяет пермишен через рефлексию. Пробуем:
     * 1. playerRef.hasPermission(String) — если доступен напрямую
     * 2. Находим Player через PlayerRef.getUuid() → Server API
     */
    private static boolean checkPermissionViaReflection(Object playerRef, String permission) {
        try {
            // Пытаемся напрямую вызвать hasPermission (если PlayerRef имеет этот метод)
            Method hasPerm = playerRef.getClass().getMethod("hasPermission", String.class);
            Object result = hasPerm.invoke(playerRef, permission);
            if (result instanceof Boolean b) return b;
        } catch (NoSuchMethodException ignored) {
            // PlayerRef не имеет hasPermission — пробуем через Player
        } catch (Exception e) {
            return false;
        }

        // Пробуем: playerRef → getReference() → entity → Player → hasPermission
        try {
            // Альтернатива: используем Server API для поиска Player по UUID
            Method getUuid = playerRef.getClass().getMethod("getUuid");
            UUID uuid = (UUID) getUuid.invoke(playerRef);

            // Пробуем через ServerManager.getServer().getPlayerByUuid()
            Class<?> serverMgr = Class.forName("com.hypixel.hytale.server.ServerManager");
            Method getServer = serverMgr.getMethod("getServer");
            Object server = getServer.invoke(null);
            if (server != null) {
                Method getPlayer = server.getClass().getMethod("getPlayerByUuid", UUID.class);
                Object player = getPlayer.invoke(server, uuid);
                if (player != null) {
                    Method hasPerm = player.getClass().getMethod("hasPermission", String.class);
                    Object result = hasPerm.invoke(player, permission);
                    if (result instanceof Boolean b) return b;
                }
            }
        } catch (Exception ignored) {
            // Fallback failed — no VIP check possible
        }

        return false;
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
