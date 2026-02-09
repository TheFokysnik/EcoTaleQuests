package com.crystalrealm.ecotalequests.tracker;

import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.generator.QuestGenerator;
import com.crystalrealm.ecotalequests.model.*;
import com.crystalrealm.ecotalequests.reward.QuestRewardCalculator;
import com.crystalrealm.ecotalequests.storage.QuestStorage;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;
import com.crystalrealm.ecotalequests.lang.LangManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Центральный трекер квестов — управляет пулом, принятием, прогрессом
 * и завершением квестов.
 *
 * <p>Все операции thread-safe. Вызывается из listeners, commands и
 * планировщика.</p>
 */
public class QuestTracker {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestsConfig config;
    private final QuestStorage storage;
    private final QuestGenerator generator;
    private final QuestRewardCalculator rewardCalculator;
    private final LangManager langManager;

    /** Кеш активных квестов игрока: playerUuid → list of active PlayerQuestData */
    private final Map<UUID, List<PlayerQuestData>> activeQuestCache = new ConcurrentHashMap<>();

    public QuestTracker(@Nonnull QuestsConfig config,
                        @Nonnull QuestStorage storage,
                        @Nonnull QuestGenerator generator,
                        @Nonnull QuestRewardCalculator rewardCalculator,
                        @Nonnull LangManager langManager) {
        this.config = config;
        this.storage = storage;
        this.generator = generator;
        this.rewardCalculator = rewardCalculator;
        this.langManager = langManager;
    }

    // ═════════════════════════════════════════════════════════════
    //  POOL MANAGEMENT
    // ═════════════════════════════════════════════════════════════

    /**
     * Обновляет пулы квестов, если они истекли.
     */
    public void refreshPools(int averagePlayerLevel) {
        refreshPool(QuestPeriod.DAILY, averagePlayerLevel);
        refreshPool(QuestPeriod.WEEKLY, averagePlayerLevel);
    }

    private void refreshPool(QuestPeriod period, int playerLevel) {
        List<Quest> current = storage.loadQuestPool(period);

        // Если пул пуст или все квесты истекли — генерируем новый
        boolean needsRefresh = current.isEmpty() ||
                current.stream().allMatch(Quest::isExpired);

        if (needsRefresh) {
            List<Quest> newPool;
            if (period == QuestPeriod.WEEKLY) {
                newPool = generator.generateWeeklyPool(playerLevel);
            } else {
                newPool = generator.generateDailyPool(playerLevel);
            }

            storage.saveQuestPool(period, newPool);
            LOGGER.info("Refreshed {} quest pool: {} quests generated.", period, newPool.size());
        }
    }

    /**
     * Возвращает доступные квесты для игрока (из пула, не принятые).
     */
    @Nonnull
    public List<Quest> getAvailableQuests(@Nonnull UUID playerUuid, @Nonnull QuestPeriod period) {
        List<Quest> pool = storage.loadQuestPool(period);
        List<PlayerQuestData> playerQuests = getPlayerQuests(playerUuid);

        Set<UUID> activeOrCompletedIds = playerQuests.stream()
                .filter(pq -> pq.getStatus() == QuestStatus.ACTIVE || pq.getStatus() == QuestStatus.COMPLETED)
                .map(PlayerQuestData::getQuestId)
                .collect(Collectors.toSet());

        return pool.stream()
                .filter(q -> !q.isExpired())
                .filter(q -> !activeOrCompletedIds.contains(q.getQuestId()))
                .toList();
    }

    // ═════════════════════════════════════════════════════════════
    //  ACCEPT / ABANDON
    // ═════════════════════════════════════════════════════════════

    /**
     * Принимает квест.
     *
     * @return Результат принятия
     */
    @Nonnull
    public AcceptResult acceptQuest(@Nonnull UUID playerUuid, @Nonnull UUID questId) {
        Quest quest = storage.getQuest(questId);
        if (quest == null) return AcceptResult.QUEST_NOT_FOUND;
        if (quest.isExpired()) return AcceptResult.QUEST_EXPIRED;

        // Проверяем лимиты
        List<PlayerQuestData> active = getActiveQuests(playerUuid);
        long activeCount = active.stream()
                .filter(pq -> {
                    Quest q = storage.getQuest(pq.getQuestId());
                    return q != null && q.getPeriod() == quest.getPeriod();
                })
                .count();

        int maxActive = quest.getPeriod() == QuestPeriod.WEEKLY
                ? config.getQuestLimits().getMaxWeeklyActive()
                : config.getQuestLimits().getMaxDailyActive();

        if (activeCount >= maxActive) return AcceptResult.LIMIT_REACHED;

        // Проверка на дубликат типа
        if (config.getProtection().isPreventDuplicateTypes()) {
            boolean hasSameType = active.stream().anyMatch(pq -> {
                Quest q = storage.getQuest(pq.getQuestId());
                return q != null && q.getObjective().getType() == quest.getObjective().getType()
                        && Objects.equals(q.getObjective().getTarget(), quest.getObjective().getTarget());
            });
            if (hasSameType) return AcceptResult.DUPLICATE_TYPE;
        }

        // Уже принят?
        PlayerQuestData existing = storage.loadPlayerQuest(playerUuid, questId);
        if (existing != null && existing.getStatus() == QuestStatus.ACTIVE) {
            return AcceptResult.ALREADY_ACTIVE;
        }

        // Принимаем
        PlayerQuestData data = PlayerQuestData.create(playerUuid, questId);
        storage.savePlayerQuest(data);
        invalidateCache(playerUuid);

        LOGGER.info("Player {} accepted quest {} ({})", playerUuid, quest.getShortId(), quest.getName());
        return AcceptResult.SUCCESS;
    }

    /**
     * Отменяет квест.
     */
    @Nonnull
    public AbandonResult abandonQuest(@Nonnull UUID playerUuid, @Nonnull UUID questId) {
        PlayerQuestData data = storage.loadPlayerQuest(playerUuid, questId);
        if (data == null) return AbandonResult.NOT_FOUND;
        if (data.getStatus() != QuestStatus.ACTIVE) return AbandonResult.NOT_ACTIVE;

        // Проверяем лимит отмен
        int abandonToday = storage.getAbandonCountToday(playerUuid);
        if (abandonToday >= config.getQuestLimits().getMaxAbandonPerDay()) {
            return AbandonResult.LIMIT_REACHED;
        }

        data.abandon();
        storage.savePlayerQuest(data);
        storage.recordAbandon(playerUuid);
        invalidateCache(playerUuid);

        LOGGER.info("Player {} abandoned quest {}", playerUuid, questId);
        return AbandonResult.SUCCESS;
    }

    // ═════════════════════════════════════════════════════════════
    //  PROGRESS TRACKING
    // ═════════════════════════════════════════════════════════════

    /**
     * Обрабатывает игровое действие и обновляет прогресс квестов.
     *
     * @param playerUuid  UUID игрока
     * @param actionType  тип действия (KILL_MOB, MINE_ORE, etc.)
     * @param actionTarget конкретная цель (имя моба, руда и т.д.)
     * @param amount      количество (обычно 1)
     * @param playerLevel уровень игрока (для скейлинга наград)
     */
    public void handleAction(@Nonnull UUID playerUuid,
                             @Nonnull QuestType actionType,
                             @Nullable String actionTarget,
                             double amount,
                             int playerLevel) {
        List<PlayerQuestData> active = getActiveQuests(playerUuid);

        for (PlayerQuestData pqd : active) {
            Quest quest = storage.getQuest(pqd.getQuestId());
            if (quest == null || quest.isExpired()) continue;

            QuestObjective obj = quest.getObjective();
            if (!obj.matches(actionType, actionTarget)) continue;

            boolean completed = pqd.addProgress(amount, obj.getRequiredAmount());
            storage.savePlayerQuest(pqd);

            if (completed) {
                onQuestCompleted(playerUuid, quest, playerLevel);
            } else {
                // Отправляем прогресс при каждом действии
                notifyActionProgress(playerUuid, quest, pqd);
                // Дополнительно уведомляем на 25/50/75% с прогресс-баром
                if (config.getGeneral().isNotifyOnProgress()) {
                    notifyMilestone(playerUuid, quest, pqd);
                }
            }
        }
    }

    /**
     * Специальный метод для обработки заработанных монет.
     */
    public void handleCoinsEarned(@Nonnull UUID playerUuid, double amount, int playerLevel) {
        handleAction(playerUuid, QuestType.EARN_COINS, null, amount, playerLevel);
    }

    /**
     * Специальный метод для обработки полученного XP.
     */
    public void handleXPGained(@Nonnull UUID playerUuid, double amount, int playerLevel) {
        handleAction(playerUuid, QuestType.GAIN_XP, null, amount, playerLevel);
    }

    // ═════════════════════════════════════════════════════════════
    //  COMPLETION
    // ═════════════════════════════════════════════════════════════

    private void onQuestCompleted(UUID playerUuid, Quest quest, int playerLevel) {
        LOGGER.info("Player {} completed quest {} ({})",
                playerUuid, quest.getShortId(), quest.getName());

        // Resolve VIP multiplier from permissions
        QuestRewardCalculator.VipResult vip = rewardCalculator.resolveVipMultiplier(playerUuid);
        double vipMult = vip.multiplier();

        // Grant reward
        boolean rewarded = rewardCalculator.grantReward(playerUuid, quest, playerLevel, vipMult);

        // Always notify on completion, regardless of reward success
        double finalCoins = rewardCalculator.calculateFinalReward(quest, playerLevel, vipMult);
        String displayName = localizedQuestDesc(playerUuid, quest);
        String msg;
        if (rewarded) {
            String vipSuffix = vip.displayName() != null
                    ? " <gray>(<yellow>" + vip.displayName() + " ×"
                      + String.format("%.2f", vipMult) + "</yellow>)</gray>"
                    : "";
            msg = langManager.getForPlayer(playerUuid, "quest.completed",
                    "name", displayName,
                    "reward", MessageUtil.formatCoins(finalCoins),
                    "vip", vipSuffix);
        } else {
            // Reward failed (economy unavailable) — still notify completion
            msg = langManager.getForPlayer(playerUuid, "quest.completed_no_reward",
                    "name", displayName);
        }
        MessageUtil.sendMessage(playerUuid, msg);

        invalidateCache(playerUuid);
    }

    /**
     * Отправляет краткое сообщение прогресса при каждом действии.
     */
    private void notifyActionProgress(UUID playerUuid, Quest quest, PlayerQuestData pqd) {
        double required = quest.getObjective().getRequiredAmount();
        double current = pqd.getCurrentProgress();

        // Используем локализованное короткое имя (только цель)
        String shortName = localizedQuestShort(playerUuid, quest);
        String msg = langManager.getForPlayer(playerUuid, "quest.action_progress",
                "name", shortName,
                "current", String.valueOf((int) current),
                "required", String.valueOf((int) required));
        MessageUtil.sendMessage(playerUuid, msg);
    }

    private void notifyMilestone(UUID playerUuid, Quest quest, PlayerQuestData pqd) {
        double required = quest.getObjective().getRequiredAmount();
        double current = pqd.getCurrentProgress();

        // Уведомляем только на 25%, 50%, 75%
        double pct = current / required;
        if (isNotifiableMilestone(pct, (current - 1) / required)) {
            String bar = MessageUtil.progressBar(current, required, 10);
            String shortName = localizedQuestShort(playerUuid, quest);
            String msg = langManager.getForPlayer(playerUuid, "quest.progress",
                    "name", shortName,
                    "current", String.valueOf((int) current),
                    "required", String.valueOf((int) required),
                    "bar", bar);
            MessageUtil.sendMessage(playerUuid, msg);
        }
    }

    private boolean isNotifiableMilestone(double newPct, double oldPct) {
        return (newPct >= 0.25 && oldPct < 0.25) ||
               (newPct >= 0.50 && oldPct < 0.50) ||
               (newPct >= 0.75 && oldPct < 0.75);
    }

    // ═════════════════════════════════════════════════════════════
    //  LOCALIZED QUEST NAMES
    // ═════════════════════════════════════════════════════════════

    /**
     * Полное локализованное описание квеста, например:
     * «Добыть: Медь ×22» или «Заработать 500 валюты».
     * Используется в сообщениях о завершении и в GUI.
     */
    private String localizedQuestDesc(UUID playerUuid, Quest quest) {
        QuestObjective obj = quest.getObjective();
        String typeKey = "quest.desc." + obj.getType().getId();
        String target = obj.getTarget();
        String targetDisplay = (target != null && !target.isEmpty())
                ? langManager.getForPlayer(playerUuid, "target." + target.toLowerCase())
                : "";
        return langManager.getForPlayer(playerUuid, typeKey,
                "amount", String.valueOf((int) obj.getRequiredAmount()),
                "target", targetDisplay);
    }

    /**
     * Короткое локализованное имя квеста для прогресс-сообщений:
     * «Медь», «Зомби», «Валюта» и т.д.
     */
    private String localizedQuestShort(UUID playerUuid, Quest quest) {
        QuestObjective obj = quest.getObjective();
        String target = obj.getTarget();
        if (target != null && !target.isEmpty()) {
            return langManager.getForPlayer(playerUuid, "target." + target.toLowerCase());
        }
        // Для квестов без цели (earn_coins, gain_xp)
        return langManager.getForPlayer(playerUuid, "quest.type." + obj.getType().getId());
    }

    // ═════════════════════════════════════════════════════════════
    //  EXPIRY CHECK
    // ═════════════════════════════════════════════════════════════

    /**
     * Проверяет и помечает истёкшие квесты для всех кешированных игроков.
     */
    public void checkExpiredQuests() {
        for (Map.Entry<UUID, List<PlayerQuestData>> entry : activeQuestCache.entrySet()) {
            for (PlayerQuestData pqd : entry.getValue()) {
                if (pqd.getStatus() != QuestStatus.ACTIVE) continue;
                Quest quest = storage.getQuest(pqd.getQuestId());
                if (quest != null && quest.isExpired()) {
                    pqd.expire();
                    storage.savePlayerQuest(pqd);
                    LOGGER.debug("Quest {} expired for player {}", pqd.getQuestId(), entry.getKey());
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  QUERY
    // ═════════════════════════════════════════════════════════════

    @Nonnull
    public List<PlayerQuestData> getActiveQuests(@Nonnull UUID playerUuid) {
        return getPlayerQuests(playerUuid).stream()
                .filter(pq -> pq.getStatus() == QuestStatus.ACTIVE)
                .toList();
    }

    @Nonnull
    public List<PlayerQuestData> getPlayerQuests(@Nonnull UUID playerUuid) {
        return activeQuestCache.computeIfAbsent(playerUuid,
                uuid -> new ArrayList<>(storage.loadPlayerQuests(uuid)));
    }

    @Nullable
    public Quest getQuest(@Nonnull UUID questId) {
        return storage.getQuest(questId);
    }

    public int getCompletedCount(@Nonnull UUID playerUuid) {
        return storage.getCompletedCount(playerUuid);
    }

    public void invalidateCache(@Nonnull UUID playerUuid) {
        activeQuestCache.remove(playerUuid);
    }

    // ═════════════════════════════════════════════════════════════
    //  ENUMS
    // ═════════════════════════════════════════════════════════════

    public enum AcceptResult {
        SUCCESS, QUEST_NOT_FOUND, QUEST_EXPIRED, LIMIT_REACHED,
        DUPLICATE_TYPE, ALREADY_ACTIVE
    }

    public enum AbandonResult {
        SUCCESS, NOT_FOUND, NOT_ACTIVE, LIMIT_REACHED
    }
}
