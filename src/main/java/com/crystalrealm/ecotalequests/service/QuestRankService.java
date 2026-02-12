package com.crystalrealm.ecotalequests.service;

import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.model.PlayerRankData;
import com.crystalrealm.ecotalequests.model.Quest;
import com.crystalrealm.ecotalequests.model.QuestRank;
import com.crystalrealm.ecotalequests.storage.QuestStorage;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;
import com.crystalrealm.ecotalequests.lang.LangManager;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис ранговой системы квестовой гильдии.
 *
 * <p>Управляет очками ранга, продвижением и понижением.
 * Потокобезопасный — все мутации через synchronized.</p>
 */
public class QuestRankService {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestStorage storage;
    private final QuestsConfig config;
    private final LangManager langManager;

    /** Кеш ранговых данных: playerUuid → PlayerRankData */
    private final Map<UUID, PlayerRankData> rankCache = new ConcurrentHashMap<>();

    public QuestRankService(@Nonnull QuestStorage storage,
                            @Nonnull QuestsConfig config,
                            @Nonnull LangManager langManager) {
        this.storage = storage;
        this.config = config;
        this.langManager = langManager;
    }

    // ═════════════════════════════════════════════════════════════
    //  RANK QUERIES
    // ═════════════════════════════════════════════════════════════

    /**
     * Возвращает текущий ранг игрока.
     */
    @Nonnull
    public QuestRank getPlayerRank(@Nonnull UUID playerUuid) {
        return getOrCreateRankData(playerUuid).getRank();
    }

    /**
     * Возвращает данные ранга игрока.
     */
    @Nonnull
    public PlayerRankData getRankData(@Nonnull UUID playerUuid) {
        return getOrCreateRankData(playerUuid);
    }

    /**
     * Проверяет, может ли игрок принять квест по рангу.
     *
     * @return true если ранг игрока >= требуемому рангу квеста
     */
    public boolean canAcceptByRank(@Nonnull UUID playerUuid, @Nonnull Quest quest) {
        QuestRank required = quest.getRequiredRank();
        if (required == null) return true; // без ограничения ранга

        QuestRank playerRank = getPlayerRank(playerUuid);
        return playerRank.canAccess(required);
    }

    // ═════════════════════════════════════════════════════════════
    //  RANK MUTATIONS
    // ═════════════════════════════════════════════════════════════

    /**
     * Начисляет очки ранга за выполнение квеста.
     *
     * @return true если ранг повысился
     */
    public boolean awardRankPoints(@Nonnull UUID playerUuid, @Nonnull Quest quest) {
        int points = quest.getRankPoints();
        if (points <= 0) return false;

        // Бонус за более высокий ранг квеста
        QuestRank required = quest.getRequiredRank();
        if (required != null && required.ordinal() > 0) {
            // +20% за каждый уровень ранга квеста выше E
            double mult = 1.0 + required.ordinal() * 0.2;
            points = (int) Math.round(points * mult);
        }

        PlayerRankData data = getOrCreateRankData(playerUuid);
        QuestRank oldRank = data.getRank();
        boolean rankChanged;

        synchronized (data) {
            rankChanged = data.addPoints(points);
            storage.saveRankData(data);
        }

        if (rankChanged) {
            QuestRank newRank = data.getRank();
            LOGGER.info("Player {} rank UP: {} → {} ({} pts)",
                    playerUuid, oldRank, newRank, data.getRankPoints());
            notifyRankUp(playerUuid, oldRank, newRank);
        }

        return rankChanged;
    }

    /**
     * Штрафует очки ранга за провал квеста.
     *
     * @return true если ранг понизился
     */
    public boolean penalizeRankPoints(@Nonnull UUID playerUuid, int penaltyPoints) {
        if (penaltyPoints <= 0) return false;
        if (!config.getRanks().isPenalizeOnFail()) return false;

        PlayerRankData data = getOrCreateRankData(playerUuid);
        QuestRank oldRank = data.getRank();
        boolean rankChanged;

        synchronized (data) {
            rankChanged = data.penalize(penaltyPoints);
            storage.saveRankData(data);
        }

        if (rankChanged) {
            QuestRank newRank = data.getRank();
            LOGGER.info("Player {} rank DOWN: {} → {} ({} pts)",
                    playerUuid, oldRank, newRank, data.getRankPoints());
            notifyRankDown(playerUuid, oldRank, newRank);
        }

        return rankChanged;
    }

    // ═════════════════════════════════════════════════════════════
    //  NOTIFICATIONS
    // ═════════════════════════════════════════════════════════════

    private void notifyRankUp(@Nonnull UUID playerUuid, @Nonnull QuestRank oldRank, @Nonnull QuestRank newRank) {
        String msg = langManager.getForPlayer(playerUuid, "rank.up",
                "old_rank", oldRank.name(),
                "new_rank", newRank.name(),
                "color", newRank.getColor());
        MessageUtil.sendMessage(playerUuid, msg);
    }

    private void notifyRankDown(@Nonnull UUID playerUuid, @Nonnull QuestRank oldRank, @Nonnull QuestRank newRank) {
        String msg = langManager.getForPlayer(playerUuid, "rank.down",
                "old_rank", oldRank.name(),
                "new_rank", newRank.name(),
                "color", newRank.getColor());
        MessageUtil.sendMessage(playerUuid, msg);
    }

    // ═════════════════════════════════════════════════════════════
    //  CACHE MANAGEMENT
    // ═════════════════════════════════════════════════════════════

    @Nonnull
    private PlayerRankData getOrCreateRankData(@Nonnull UUID playerUuid) {
        return rankCache.computeIfAbsent(playerUuid, uuid -> {
            PlayerRankData loaded = storage.loadRankData(uuid);
            if (loaded != null) return loaded;
            PlayerRankData fresh = PlayerRankData.createNew(uuid);
            storage.saveRankData(fresh);
            return fresh;
        });
    }

    /**
     * Инвалидирует кеш ранга для игрока.
     */
    public void invalidateCache(@Nonnull UUID playerUuid) {
        rankCache.remove(playerUuid);
    }

    /**
     * Очищает весь кеш.
     */
    public void clearCache() {
        rankCache.clear();
    }
}
