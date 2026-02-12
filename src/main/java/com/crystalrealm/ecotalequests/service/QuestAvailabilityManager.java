package com.crystalrealm.ecotalequests.service;

import com.crystalrealm.ecotalequests.model.Quest;
import com.crystalrealm.ecotalequests.model.QuestAccessType;
import com.crystalrealm.ecotalequests.model.QuestAssignment;
import com.crystalrealm.ecotalequests.storage.QuestStorage;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Потокобезопасный менеджер доступности квестов.
 *
 * <p>Управляет блокировками для GLOBAL_UNIQUE и LIMITED_SLOTS квестов.
 * Использует Read-Write Lock для масштабируемости при 500+ игроках.</p>
 */
public class QuestAvailabilityManager {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestStorage storage;

    /**
     * Активные назначения: questId → список активных QuestAssignment.
     * При GLOBAL_UNIQUE — максимум 1 запись.
     * При LIMITED_SLOTS — до maxSlots записей.
     */
    private final Map<UUID, List<QuestAssignment>> assignments = new ConcurrentHashMap<>();

    /** Глобальный RW-lock для атомарных операций с назначениями. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public QuestAvailabilityManager(@Nonnull QuestStorage storage) {
        this.storage = storage;
    }

    // ═════════════════════════════════════════════════════════════
    //  INITIALIZATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Загружает активные назначения из хранилища.
     */
    public void initialize() {
        List<QuestAssignment> loaded = storage.loadActiveAssignments();
        lock.writeLock().lock();
        try {
            assignments.clear();
            for (QuestAssignment a : loaded) {
                if (!a.isReleased() && !a.isTimerExpired()) {
                    assignments.computeIfAbsent(a.getQuestId(), k -> new ArrayList<>()).add(a);
                }
            }
            LOGGER.info("QuestAvailabilityManager loaded {} active assignments.", loaded.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  AVAILABILITY CHECK
    // ═════════════════════════════════════════════════════════════

    /**
     * Проверяет, доступен ли квест для принятия.
     *
     * @param quest     квест
     * @param playerUuid UUID игрока (для проверки что не дублирует)
     * @return true если слот доступен
     */
    public boolean isAvailable(@Nonnull Quest quest, @Nonnull UUID playerUuid) {
        if (quest.getAccessType() == QuestAccessType.INDIVIDUAL) return true;

        lock.readLock().lock();
        try {
            List<QuestAssignment> questAssignments = assignments.get(quest.getQuestId());
            if (questAssignments == null || questAssignments.isEmpty()) return true;

            // Проверяем, не взял ли уже этот игрок
            for (QuestAssignment a : questAssignments) {
                if (a.getPlayerUuid().equals(playerUuid) && !a.isReleased()) {
                    return false; // уже назначен
                }
            }

            // Чистим истёкшие
            long activeCount = questAssignments.stream()
                    .filter(a -> !a.isReleased() && !a.isTimerExpired())
                    .count();

            if (quest.getAccessType() == QuestAccessType.GLOBAL_UNIQUE) {
                return activeCount == 0;
            }

            // LIMITED_SLOTS
            return activeCount < quest.getMaxSlots();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Возвращает количество занятых слотов для квеста.
     */
    public int getOccupiedSlots(@Nonnull UUID questId) {
        lock.readLock().lock();
        try {
            List<QuestAssignment> questAssignments = assignments.get(questId);
            if (questAssignments == null) return 0;
            return (int) questAssignments.stream()
                    .filter(a -> !a.isReleased() && !a.isTimerExpired())
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  ASSIGNMENT / RELEASE
    // ═════════════════════════════════════════════════════════════

    /**
     * Пытается занять слот для квеста. Атомарная операция.
     *
     * @return назначение при успехе, null при неудаче (слоты заняты)
     */
    @Nullable
    public QuestAssignment tryAssign(@Nonnull Quest quest, @Nonnull UUID playerUuid) {
        if (quest.getAccessType() == QuestAccessType.INDIVIDUAL) {
            // Для индивидуальных квестов назначение не нужно,
            // но создаём запись для единообразия
            return QuestAssignment.create(quest.getQuestId(), playerUuid, quest.getDurationMinutes());
        }

        lock.writeLock().lock();
        try {
            if (!isAvailableUnlocked(quest, playerUuid)) {
                LOGGER.debug("Slot unavailable for quest {} (player {})", quest.getShortId(), playerUuid);
                return null;
            }

            QuestAssignment assignment = QuestAssignment.create(
                    quest.getQuestId(), playerUuid, quest.getDurationMinutes());

            assignments.computeIfAbsent(quest.getQuestId(), k -> new ArrayList<>()).add(assignment);
            storage.saveAssignment(assignment);

            LOGGER.info("Assigned quest {} to player {} (type={})",
                    quest.getShortId(), playerUuid, quest.getAccessType());
            return assignment;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Освобождает слот квеста (при завершении, провале или отмене).
     */
    public void releaseAssignment(@Nonnull UUID questId, @Nonnull UUID playerUuid) {
        lock.writeLock().lock();
        try {
            List<QuestAssignment> questAssignments = assignments.get(questId);
            if (questAssignments == null) return;

            for (QuestAssignment a : questAssignments) {
                if (a.getPlayerUuid().equals(playerUuid) && !a.isReleased()) {
                    a.release();
                    storage.saveAssignment(a);
                    LOGGER.info("Released quest {} assignment for player {}", questId, playerUuid);
                    break;
                }
            }

            // Чистим полностью released записи
            questAssignments.removeIf(QuestAssignment::isReleased);
            if (questAssignments.isEmpty()) {
                assignments.remove(questId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  TIMER EXPIRY CHECK
    // ═════════════════════════════════════════════════════════════

    /**
     * Находит все назначения с истёкшим таймером.
     *
     * @return список истёкших назначений
     */
    @Nonnull
    public List<QuestAssignment> getExpiredAssignments() {
        List<QuestAssignment> expired = new ArrayList<>();
        lock.readLock().lock();
        try {
            for (List<QuestAssignment> questAssignments : assignments.values()) {
                for (QuestAssignment a : questAssignments) {
                    if (!a.isReleased() && a.isTimerExpired()) {
                        expired.add(a);
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return expired;
    }

    /**
     * Возвращает назначение для конкретного игрока и квеста.
     */
    @Nullable
    public QuestAssignment getAssignment(@Nonnull UUID questId, @Nonnull UUID playerUuid) {
        lock.readLock().lock();
        try {
            List<QuestAssignment> questAssignments = assignments.get(questId);
            if (questAssignments == null) return null;
            return questAssignments.stream()
                    .filter(a -> a.getPlayerUuid().equals(playerUuid) && !a.isReleased())
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  INTERNAL (unguarded, call under write lock)
    // ═════════════════════════════════════════════════════════════

    private boolean isAvailableUnlocked(@Nonnull Quest quest, @Nonnull UUID playerUuid) {
        List<QuestAssignment> questAssignments = assignments.get(quest.getQuestId());
        if (questAssignments == null || questAssignments.isEmpty()) return true;

        for (QuestAssignment a : questAssignments) {
            if (a.getPlayerUuid().equals(playerUuid) && !a.isReleased()) return false;
        }

        long activeCount = questAssignments.stream()
                .filter(a -> !a.isReleased() && !a.isTimerExpired())
                .count();

        return switch (quest.getAccessType()) {
            case GLOBAL_UNIQUE -> activeCount == 0;
            case LIMITED_SLOTS -> activeCount < quest.getMaxSlots();
            default -> true;
        };
    }

    /**
     * Очищает все данные.
     */
    public void shutdown() {
        lock.writeLock().lock();
        try {
            assignments.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
