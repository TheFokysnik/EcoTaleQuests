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
    private final Path boardsFile;
    private final Path assignmentsFile;

    /** Кеш квестов: questId → Quest */
    private final Map<UUID, Quest> questCache = new ConcurrentHashMap<>();

    /** Кеш данных игроков: playerUuid → (questId → PlayerQuestData) */
    private final Map<UUID, Map<UUID, PlayerQuestData>> playerCache = new ConcurrentHashMap<>();

    /** Статистика отмен: playerUuid → (date → count) */
    private final Map<UUID, Map<String, Integer>> abandonStats = new ConcurrentHashMap<>();

    /** Общее кол-во завершённых: playerUuid → count */
    private final Map<UUID, Integer> completedCounts = new ConcurrentHashMap<>();

    /** Ранговые данные: playerUuid → PlayerRankData */
    private final Map<UUID, PlayerRankData> rankDataCache = new ConcurrentHashMap<>();

    /** Доски квестов */
    private final List<QuestBoardLocation> boardLocations = Collections.synchronizedList(new ArrayList<>());

    /** Назначения квестов */
    private final List<QuestAssignment> questAssignments = Collections.synchronizedList(new ArrayList<>());

    public JsonQuestStorage(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.questsDir = dataDirectory.resolve("quests");
        this.playersDir = dataDirectory.resolve("players");
        this.boardsFile = dataDirectory.resolve("boards.json");
        this.assignmentsFile = dataDirectory.resolve("assignments.json");
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

            // Загружаем доски квестов
            loadBoardsFromDisk();

            // Загружаем назначения
            loadAssignmentsFromDisk();

            LOGGER.info("JsonQuestStorage initialized. Quests: {}, Players: {}, Boards: {}, Assignments: {}",
                    questCache.size(), playerCache.size(), boardLocations.size(), questAssignments.size());
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
            saveBoardsToDisk();
            saveAssignmentsToDisk();
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
        rankDataCache.clear();
        boardLocations.clear();
        questAssignments.clear();
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

        // Persist to disk immediately so data survives crashes/restarts
        savePlayerFile(data.getPlayerUuid());
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
    //  RANK DATA
    // ═════════════════════════════════════════════════════════════

    @Override
    public PlayerRankData loadRankData(@Nonnull UUID playerUuid) {
        return rankDataCache.get(playerUuid);
    }

    @Override
    public void saveRankData(@Nonnull PlayerRankData data) {
        rankDataCache.put(data.getPlayerUuid(), data);
    }

    // ═════════════════════════════════════════════════════════════
    //  BOARD LOCATIONS
    // ═════════════════════════════════════════════════════════════

    @Override
    @Nonnull
    public List<QuestBoardLocation> loadBoardLocations() {
        return new ArrayList<>(boardLocations);
    }

    @Override
    public void saveBoardLocation(@Nonnull QuestBoardLocation board) {
        // Remove if already exists (update)
        boardLocations.removeIf(b -> b.getBoardId().equals(board.getBoardId()));
        boardLocations.add(board);
        saveBoardsToDisk();
    }

    @Override
    public void removeBoardLocation(@Nonnull UUID boardId) {
        boardLocations.removeIf(b -> b.getBoardId().equals(boardId));
        saveBoardsToDisk();
    }

    // ═════════════════════════════════════════════════════════════
    //  QUEST ASSIGNMENTS
    // ═════════════════════════════════════════════════════════════

    @Override
    @Nonnull
    public List<QuestAssignment> loadActiveAssignments() {
        return questAssignments.stream()
                .filter(a -> !a.isReleased() && !a.isTimerExpired())
                .toList();
    }

    @Override
    public void saveAssignment(@Nonnull QuestAssignment assignment) {
        // Remove existing entry for same quest+player
        questAssignments.removeIf(a ->
                a.getQuestId().equals(assignment.getQuestId())
                && a.getPlayerUuid().equals(assignment.getPlayerUuid()));
        if (!assignment.isReleased()) {
            questAssignments.add(assignment);
        }
        saveAssignmentsToDisk();
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

            // Load rank data
            if (data.rankPoints > 0 || data.totalCompleted > 0 || data.totalFailed > 0) {
                PlayerRankData rankData = new PlayerRankData(
                        playerUuid, data.rankPoints, data.totalCompleted, data.totalFailed);
                rankDataCache.put(playerUuid, rankData);
            }

            // Restore quest definitions into questCache so active quests are displayable
            if (data.questDefinitions != null) {
                for (QuestData qd : data.questDefinitions) {
                    Quest quest = qd.toQuest();
                    if (quest != null) {
                        questCache.putIfAbsent(quest.getQuestId(), quest);
                    }
                }
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

        // Rank data
        PlayerRankData rankData = rankDataCache.get(playerUuid);
        if (rankData != null) {
            data.rankPoints = rankData.getRankPoints();
            data.totalCompleted = rankData.getTotalCompleted();
            data.totalFailed = rankData.getTotalFailed();
        }

        Map<UUID, PlayerQuestData> quests = playerCache.get(playerUuid);
        if (quests != null) {
            data.quests = quests.values().stream()
                    .map(PlayerQuestEntry::fromPlayerQuestData)
                    .toList();

            // Snapshot quest definitions for ACTIVE quests so they survive pool refresh / restart
            data.questDefinitions = quests.values().stream()
                    .filter(pqd -> pqd.getStatus() == QuestStatus.ACTIVE)
                    .map(pqd -> questCache.get(pqd.getQuestId()))
                    .filter(java.util.Objects::nonNull)
                    .map(QuestData::fromQuest)
                    .toList();
        }

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save player file: " + file, e);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  DISK I/O — Boards
    // ═════════════════════════════════════════════════════════════

    private void loadBoardsFromDisk() {
        if (!Files.exists(boardsFile)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(boardsFile), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<BoardData>>() {}.getType();
            List<BoardData> data = GSON.fromJson(reader, listType);
            if (data != null) {
                for (BoardData bd : data) {
                    QuestBoardLocation board = bd.toBoard();
                    if (board != null) boardLocations.add(board);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load boards", e);
        }
    }

    private void saveBoardsToDisk() {
        try {
            List<BoardData> data = boardLocations.stream()
                    .map(BoardData::fromBoard)
                    .toList();
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(boardsFile), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save boards", e);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  DISK I/O — Assignments
    // ═════════════════════════════════════════════════════════════

    private void loadAssignmentsFromDisk() {
        if (!Files.exists(assignmentsFile)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(assignmentsFile), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<AssignmentData>>() {}.getType();
            List<AssignmentData> data = GSON.fromJson(reader, listType);
            if (data != null) {
                for (AssignmentData ad : data) {
                    QuestAssignment assignment = ad.toAssignment();
                    if (assignment != null && !assignment.isTimerExpired()) {
                        questAssignments.add(assignment);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load assignments", e);
        }
    }

    private void saveAssignmentsToDisk() {
        try {
            List<AssignmentData> data = questAssignments.stream()
                    .filter(a -> !a.isReleased())
                    .map(AssignmentData::fromAssignment)
                    .toList();
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(assignmentsFile), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save assignments", e);
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
        String accessType;
        int maxSlots;
        int durationMinutes;
        String requiredRank;
        int rankPoints;
        long createdAt;
        long expiresAt;

        Quest toQuest() {
            try {
                return new Quest(
                        UUID.fromString(questId), name, description,
                        QuestPeriod.fromId(period),
                        new QuestObjective(QuestType.fromId(objectiveType), objectiveTarget, objectiveAmount),
                        new QuestReward(rewardCoins, rewardXp),
                        minLevel,
                        accessType != null ? QuestAccessType.fromId(accessType) : QuestAccessType.INDIVIDUAL,
                        maxSlots,
                        durationMinutes,
                        requiredRank != null ? QuestRank.fromId(requiredRank) : null,
                        rankPoints > 0 ? rankPoints : 10,
                        createdAt, expiresAt
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
            d.accessType = q.getAccessType().getId();
            d.maxSlots = q.getMaxSlots();
            d.durationMinutes = q.getDurationMinutes();
            d.requiredRank = q.getRequiredRank() != null ? q.getRequiredRank().name() : null;
            d.rankPoints = q.getRankPoints();
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
        // Rank data
        int rankPoints;
        int totalCompleted;
        int totalFailed;
        // Quest definitions snapshot — persisted so active quests survive pool refresh / restart
        List<QuestData> questDefinitions;
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

    /** Serializable board data. */
    static class BoardData {
        String boardId;
        String worldName;
        int x, y, z;
        String type;
        String placedBy;
        long placedAt;

        QuestBoardLocation toBoard() {
            try {
                return new QuestBoardLocation(
                        UUID.fromString(boardId), worldName, x, y, z,
                        QuestBoardLocation.BoardType.fromId(type),
                        UUID.fromString(placedBy), placedAt
                );
            } catch (Exception e) {
                return null;
            }
        }

        static BoardData fromBoard(QuestBoardLocation b) {
            BoardData d = new BoardData();
            d.boardId = b.getBoardId().toString();
            d.worldName = b.getWorldName();
            d.x = b.getX();
            d.y = b.getY();
            d.z = b.getZ();
            d.type = b.getType().getId();
            d.placedBy = b.getPlacedBy().toString();
            d.placedAt = b.getPlacedAt();
            return d;
        }
    }

    /** Serializable assignment data. */
    static class AssignmentData {
        String questId;
        String playerUuid;
        long assignedAt;
        long expiresAt;

        QuestAssignment toAssignment() {
            try {
                return new QuestAssignment(
                        UUID.fromString(questId),
                        UUID.fromString(playerUuid),
                        assignedAt, expiresAt
                );
            } catch (Exception e) {
                return null;
            }
        }

        static AssignmentData fromAssignment(QuestAssignment a) {
            AssignmentData d = new AssignmentData();
            d.questId = a.getQuestId().toString();
            d.playerUuid = a.getPlayerUuid().toString();
            d.assignedAt = a.getAssignedAt();
            d.expiresAt = a.getExpiresAt();
            return d;
        }
    }
}
