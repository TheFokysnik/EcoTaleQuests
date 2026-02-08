package com.crystalrealm.ecotalequests.storage;

import com.crystalrealm.ecotalequests.model.PlayerQuestData;
import com.crystalrealm.ecotalequests.model.Quest;
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
}
