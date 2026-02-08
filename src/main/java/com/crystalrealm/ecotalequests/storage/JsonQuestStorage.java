package com.crystalrealm.ecotalequests.storage;

import com.crystalrealm.ecotalequests.model.*;
import com.crystalrealm.ecotalequests.util.PluginLogger;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-реализация хранилища квестов.
 *
 * <p>Хранит данные в папке {@code data/} плагина:
 * <ul>
 *   <li>{@code quests/daily_pool.json} — текущий пул дневных квестов</li>
 *   <li>{@code quests/weekly_pool.json} — текущий пул недельных квестов</li>
 *   <li>{@code players/<uuid>.json} — прогресс каждого игрока</li>
 * </ul>
 */
public class JsonQuestStorage implements QuestStorage {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path dataDirectory;
    private final Path questsDir;
    private final Path playersDir;

    /** Кеш квестов: questId → Quest */
    private final Map<UUID, Quest> questCache = new ConcurrentHashMap<>();

    /** Кеш данных игроков: playerUuid → (questId → PlayerQuestData) */
    private final Map<UUID, Map<UUID, PlayerQuestData>> playerCache = new ConcurrentHashMap<>();

    /** Статистика отмен: playerUuid → (date → count) */
    private final Map<UUID, Map<String, Integer>> abandonStats = new ConcurrentHashMap<>();

    /** Общее кол-во завершённых: playerUuid → count */
    private final Map<UUID, Integer> completedCounts = new ConcurrentHashMap<>();

    public JsonQuestStorage(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.questsDir = dataDirectory.resolve("quests");
        this.playersDir = dataDirectory.resolve("players");
    }

    // ═════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════════════

    @Override
    public void initialize() {
        try {
            Files.createDirectories(questsDir);
            Files.createDirectories(playersDir);

            // Загружаем пулы квестов
            loadQuestPoolFromDisk(QuestPeriod.DAILY);
            loadQuestPoolFromDisk(QuestPeriod.WEEKLY);

            // Загружаем данные игроков
            loadAllPlayerData();

            LOGGER.info("JsonQuestStorage initialized. Quests: {}, Players: {}",
                    questCache.size(), playerCache.size());
        } catch (IOException e) {
            LOGGER.error("Failed to initialize storage", e);
        }
    }

    @Override
    public void save() {
        try {
            saveQuestPoolToDisk(QuestPeriod.DAILY);
            saveQuestPoolToDisk(QuestPeriod.WEEKLY);
            saveAllPlayerData();
            LOGGER.debug("Storage saved successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to save storage", e);
        }
    }

    @Override
    public void shutdown() {
        save();
        questCache.clear();
        playerCache.clear();
        abandonStats.clear();
        completedCounts.clear();
        LOGGER.info("JsonQuestStorage shut down.");
    }

    // ═════════════════════════════════════════════════════════════
    //  QUEST POOL
    // ═════════════════════════════════════════════════════════════

    @Override
    public void saveQuestPool(@Nonnull QuestPeriod period, @Nonnull List<Quest> quests) {
        // Обновляем кеш
        for (Quest q : quests) {
            questCache.put(q.getQuestId(), q);
        }
        saveQuestPoolToDisk(period);
    }

    @Override
    @Nonnull
    public List<Quest> loadQuestPool(@Nonnull QuestPeriod period) {
        List<Quest> result = new ArrayList<>();
        for (Quest q : questCache.values()) {
            if (q.getPeriod() == period && !q.isExpired()) {
                result.add(q);
            }
        }
        return result;
    }

    @Override
    public Quest getQuest(@Nonnull UUID questId) {
        return questCache.get(questId);
    }

    // ═════════════════════════════════════════════════════════════
    //  PLAYER PROGRESS
    // ═════════════════════════════════════════════════════════════

    @Override
    public void savePlayerQuest(@Nonnull PlayerQuestData data) {
        playerCache.computeIfAbsent(data.getPlayerUuid(), k -> new ConcurrentHashMap<>())
                .put(data.getQuestId(), data);

        if (data.getStatus() == QuestStatus.COMPLETED) {
            completedCounts.merge(data.getPlayerUuid(), 1, Integer::sum);
        }
    }

    @Override
    @Nonnull
    public List<PlayerQuestData> loadPlayerQuests(@Nonnull UUID playerUuid) {
        Map<UUID, PlayerQuestData> quests = playerCache.get(playerUuid);
        if (quests == null) return Collections.emptyList();
        return new ArrayList<>(quests.values());
    }

    @Override
    public PlayerQuestData loadPlayerQuest(@Nonnull UUID playerUuid, @Nonnull UUID questId) {
        Map<UUID, PlayerQuestData> quests = playerCache.get(playerUuid);
        if (quests == null) return null;
        return quests.get(questId);
    }

    @Override
    public void removePlayerQuest(@Nonnull UUID playerUuid, @Nonnull UUID questId) {
        Map<UUID, PlayerQuestData> quests = playerCache.get(playerUuid);
        if (quests != null) {
            quests.remove(questId);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  STATISTICS
    // ═════════════════════════════════════════════════════════════

    @Override
    public int getCompletedCount(@Nonnull UUID playerUuid) {
        return completedCounts.getOrDefault(playerUuid, 0);
    }

    @Override
    public int getAbandonCountToday(@Nonnull UUID playerUuid) {
        Map<String, Integer> stats = abandonStats.get(playerUuid);
        if (stats == null) return 0;
        String today = LocalDate.now().toString();
        return stats.getOrDefault(today, 0);
    }

    @Override
    public void recordAbandon(@Nonnull UUID playerUuid) {
        String today = LocalDate.now().toString();
        abandonStats.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                .merge(today, 1, Integer::sum);
    }

    // ═════════════════════════════════════════════════════════════
    //  DISK I/O — Quest Pools
    // ═════════════════════════════════════════════════════════════

    private void loadQuestPoolFromDisk(QuestPeriod period) {
        Path file = questsDir.resolve(period.getId() + "_pool.json");
        if (!Files.exists(file)) return;

        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<QuestData>>() {}.getType();
            List<QuestData> data = GSON.fromJson(reader, listType);
            if (data != null) {
                for (QuestData qd : data) {
                    Quest quest = qd.toQuest();
                    if (quest != null && !quest.isExpired()) {
                        questCache.put(quest.getQuestId(), quest);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load quest pool: " + file, e);
        }
    }

    private void saveQuestPoolToDisk(QuestPeriod period) {
        Path file = questsDir.resolve(period.getId() + "_pool.json");
        List<QuestData> data = new ArrayList<>();

        for (Quest q : questCache.values()) {
            if (q.getPeriod() == period) {
                data.add(QuestData.fromQuest(q));
            }
        }

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save quest pool: " + file, e);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  DISK I/O — Player Data
    // ═════════════════════════════════════════════════════════════

    private void loadAllPlayerData() {
        try {
            if (!Files.isDirectory(playersDir)) return;

            Files.list(playersDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadPlayerFile);
        } catch (IOException e) {
            LOGGER.error("Failed to load player data", e);
        }
    }

    private void loadPlayerFile(Path file) {
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            PlayerFileData data = GSON.fromJson(reader, PlayerFileData.class);
            if (data == null) return;

            UUID playerUuid = UUID.fromString(data.playerUuid);
            Map<UUID, PlayerQuestData> quests = new ConcurrentHashMap<>();

            if (data.quests != null) {
                for (PlayerQuestEntry entry : data.quests) {
                    PlayerQuestData pqd = entry.toPlayerQuestData(playerUuid);
                    if (pqd != null) {
                        quests.put(pqd.getQuestId(), pqd);
                    }
                }
            }
            playerCache.put(playerUuid, quests);

            if (data.completedTotal > 0) {
                completedCounts.put(playerUuid, data.completedTotal);
            }

            if (data.abandonHistory != null) {
                abandonStats.put(playerUuid, new ConcurrentHashMap<>(data.abandonHistory));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load player file: " + file, e);
        }
    }

    private void saveAllPlayerData() {
        for (Map.Entry<UUID, Map<UUID, PlayerQuestData>> entry : playerCache.entrySet()) {
            UUID playerUuid = entry.getKey();
            savePlayerFile(playerUuid);
        }
    }

    private void savePlayerFile(UUID playerUuid) {
        Path file = playersDir.resolve(playerUuid.toString() + ".json");

        PlayerFileData data = new PlayerFileData();
        data.playerUuid = playerUuid.toString();
        data.completedTotal = completedCounts.getOrDefault(playerUuid, 0);
        data.abandonHistory = abandonStats.get(playerUuid);

        Map<UUID, PlayerQuestData> quests = playerCache.get(playerUuid);
        if (quests != null) {
            data.quests = quests.values().stream()
                    .map(PlayerQuestEntry::fromPlayerQuestData)
                    .toList();
        }

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save player file: " + file, e);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  JSON MODELS (for Gson serialization)
    // ═════════════════════════════════════════════════════════════

    /** Serializable quest data. */
    static class QuestData {
        String questId;
        String name;
        String description;
        String period;
        String objectiveType;
        String objectiveTarget;
        double objectiveAmount;
        double rewardCoins;
        int rewardXp;
        int minLevel;
        long createdAt;
        long expiresAt;

        Quest toQuest() {
            try {
                return new Quest(
                        UUID.fromString(questId), name, description,
                        QuestPeriod.fromId(period),
                        new QuestObjective(QuestType.fromId(objectiveType), objectiveTarget, objectiveAmount),
                        new QuestReward(rewardCoins, rewardXp),
                        minLevel, createdAt, expiresAt
                );
            } catch (Exception e) {
                return null;
            }
        }

        static QuestData fromQuest(Quest q) {
            QuestData d = new QuestData();
            d.questId = q.getQuestId().toString();
            d.name = q.getName();
            d.description = q.getDescription();
            d.period = q.getPeriod().getId();
            d.objectiveType = q.getObjective().getType().getId();
            d.objectiveTarget = q.getObjective().getTarget();
            d.objectiveAmount = q.getObjective().getRequiredAmount();
            d.rewardCoins = q.getReward().getBaseCoins();
            d.rewardXp = q.getReward().getBonusXp();
            d.minLevel = q.getMinLevel();
            d.createdAt = q.getCreatedAt();
            d.expiresAt = q.getExpiresAt();
            return d;
        }
    }

    /** Serializable player file. */
    static class PlayerFileData {
        String playerUuid;
        int completedTotal;
        List<PlayerQuestEntry> quests;
        Map<String, Integer> abandonHistory;
    }

    /** Serializable player quest entry. */
    static class PlayerQuestEntry {
        String questId;
        String status;
        double currentProgress;
        long acceptedAt;
        long completedAt;

        PlayerQuestData toPlayerQuestData(UUID playerUuid) {
            try {
                return new PlayerQuestData(
                        playerUuid,
                        UUID.fromString(questId),
                        QuestStatus.fromId(status),
                        currentProgress,
                        acceptedAt,
                        completedAt
                );
            } catch (Exception e) {
                return null;
            }
        }

        static PlayerQuestEntry fromPlayerQuestData(PlayerQuestData d) {
            PlayerQuestEntry e = new PlayerQuestEntry();
            e.questId = d.getQuestId().toString();
            e.status = d.getStatus().getId();
            e.currentProgress = d.getCurrentProgress();
            e.acceptedAt = d.getAcceptedAt();
            e.completedAt = d.getCompletedAt();
            return e;
        }
    }
}
