package com.crystalrealm.ecotalequests.storage;

import com.crystalrealm.ecotalequests.model.PlayerQuestData;
import com.crystalrealm.ecotalequests.model.PlayerRankData;
import com.crystalrealm.ecotalequests.model.Quest;
import com.crystalrealm.ecotalequests.model.QuestAssignment;
import com.crystalrealm.ecotalequests.model.QuestBoardLocation;
import com.crystalrealm.ecotalequests.model.QuestPeriod;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Интерфейс хранилища данных квестов.
 */
public interface QuestStorage {

    /** Инициализирует хранилище. */
    void initialize();

    /** Сохраняет все данные на диск. */
    void save();

    /** Закрывает хранилище. */
    void shutdown();

    // ── Quest Pool ──────────────────────────────────────────────

    /** Сохраняет сгенерированный пул квестов. */
    void saveQuestPool(@Nonnull QuestPeriod period, @Nonnull List<Quest> quests);

    /** Загружает текущий пул квестов для периода. */
    @Nonnull
    List<Quest> loadQuestPool(@Nonnull QuestPeriod period);

    /** Возвращает квест по ID. */
    Quest getQuest(@Nonnull UUID questId);

    // ── Player Progress ─────────────────────────────────────────

    /** Сохраняет прогресс игрока по квесту. */
    void savePlayerQuest(@Nonnull PlayerQuestData data);

    /** Загружает все активные квесты игрока. */
    @Nonnull
    List<PlayerQuestData> loadPlayerQuests(@Nonnull UUID playerUuid);

    /** Загружает прогресс игрока по конкретному квесту. */
    PlayerQuestData loadPlayerQuest(@Nonnull UUID playerUuid, @Nonnull UUID questId);

    /** Удаляет данные квеста игрока (при abandon). */
    void removePlayerQuest(@Nonnull UUID playerUuid, @Nonnull UUID questId);

    // ── Statistics ──────────────────────────────────────────────

    /** Количество выполненных квестов игроком. */
    int getCompletedCount(@Nonnull UUID playerUuid);

    /** Количество отмен (abandon) за сегодня. */
    int getAbandonCountToday(@Nonnull UUID playerUuid);

    /** Записывает отмену квеста. */
    void recordAbandon(@Nonnull UUID playerUuid);

    // ── Rank Data ───────────────────────────────────────────────

    /** Загружает ранговые данные игрока. */
    PlayerRankData loadRankData(@Nonnull UUID playerUuid);

    /** Сохраняет ранговые данные игрока. */
    void saveRankData(@Nonnull PlayerRankData data);

    // ── Board Locations ─────────────────────────────────────────

    /** Загружает все доски квестов. */
    @Nonnull
    List<QuestBoardLocation> loadBoardLocations();

    /** Сохраняет доску квестов. */
    void saveBoardLocation(@Nonnull QuestBoardLocation board);

    /** Удаляет доску квестов. */
    void removeBoardLocation(@Nonnull UUID boardId);

    // ── Quest Assignments ───────────────────────────────────────

    /** Загружает активные назначения (не released, не expired). */
    @Nonnull
    List<QuestAssignment> loadActiveAssignments();

    /** Сохраняет назначение. */
    void saveAssignment(@Nonnull QuestAssignment assignment);
}
