package com.crystalrealm.ecotalequests.service;

import com.crystalrealm.ecotalequests.model.QuestBoardLocation;
import com.crystalrealm.ecotalequests.storage.QuestStorage;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер досок квестов в мире.
 *
 * <p>Управляет размещением, удалением и поиском досок квестов.
 * Координаты досок хранятся персистентно.</p>
 *
 * <p>Права доступа:
 * <ul>
 *   <li>{@code ecotale.quests.board.place} — размещение доски</li>
 *   <li>{@code ecotale.quests.board.remove} — удаление доски</li>
 * </ul>
 */
public class QuestBoardManager {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    public static final String PERMISSION_PLACE = "ecotale.quests.board.place";
    public static final String PERMISSION_REMOVE = "ecotale.quests.board.remove";

    private final QuestStorage storage;

    /** Все доски: positionKey → QuestBoardLocation */
    private final Map<String, QuestBoardLocation> boards = new ConcurrentHashMap<>();

    /** Индекс по boardId */
    private final Map<UUID, QuestBoardLocation> boardsById = new ConcurrentHashMap<>();

    public QuestBoardManager(@Nonnull QuestStorage storage) {
        this.storage = storage;
    }

    // ═════════════════════════════════════════════════════════════
    //  INITIALIZATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Загружает все доски из хранилища.
     */
    public void initialize() {
        List<QuestBoardLocation> loaded = storage.loadBoardLocations();
        boards.clear();
        boardsById.clear();
        for (QuestBoardLocation board : loaded) {
            boards.put(board.positionKey(), board);
            boardsById.put(board.getBoardId(), board);
        }
        LOGGER.info("QuestBoardManager loaded {} boards.", boards.size());
    }

    // ═════════════════════════════════════════════════════════════
    //  BOARD CRUD
    // ═════════════════════════════════════════════════════════════

    /**
     * Размещает новую доску квестов.
     *
     * @return результат размещения
     */
    @Nonnull
    public PlaceResult placeBoard(@Nonnull String worldName,
                                  int x, int y, int z,
                                  @Nonnull QuestBoardLocation.BoardType type,
                                  @Nonnull UUID placedBy) {
        String key = worldName + ":" + x + ":" + y + ":" + z;

        if (boards.containsKey(key)) {
            return PlaceResult.ALREADY_EXISTS;
        }

        QuestBoardLocation board = QuestBoardLocation.create(worldName, x, y, z, type, placedBy);
        boards.put(board.positionKey(), board);
        boardsById.put(board.getBoardId(), board);
        storage.saveBoardLocation(board);

        LOGGER.info("Board placed at {} by {} (type={})", board.positionKey(), placedBy, type);
        return PlaceResult.SUCCESS;
    }

    /**
     * Удаляет доску квестов по позиции.
     *
     * @return true если доска была удалена
     */
    public boolean removeBoard(@Nonnull String worldName, int x, int y, int z) {
        String key = worldName + ":" + x + ":" + y + ":" + z;
        QuestBoardLocation board = boards.remove(key);
        if (board == null) return false;

        boardsById.remove(board.getBoardId());
        storage.removeBoardLocation(board.getBoardId());

        LOGGER.info("Board removed at {}", key);
        return true;
    }

    /**
     * Удаляет доску квестов по ID.
     */
    public boolean removeBoardById(@Nonnull UUID boardId) {
        QuestBoardLocation board = boardsById.remove(boardId);
        if (board == null) return false;

        boards.remove(board.positionKey());
        storage.removeBoardLocation(boardId);

        LOGGER.info("Board removed: {}", boardId);
        return true;
    }

    // ═════════════════════════════════════════════════════════════
    //  QUERIES
    // ═════════════════════════════════════════════════════════════

    /**
     * Проверяет, является ли данная позиция доской квестов.
     */
    @Nullable
    public QuestBoardLocation getBoardAt(@Nonnull String worldName, int x, int y, int z) {
        String key = worldName + ":" + x + ":" + y + ":" + z;
        return boards.get(key);
    }

    /**
     * Получает доску по ID.
     */
    @Nullable
    public QuestBoardLocation getBoardById(@Nonnull UUID boardId) {
        return boardsById.get(boardId);
    }

    /**
     * Возвращает все доски.
     */
    @Nonnull
    public Collection<QuestBoardLocation> getAllBoards() {
        return Collections.unmodifiableCollection(boards.values());
    }

    /**
     * Количество досок.
     */
    public int getBoardCount() {
        return boards.size();
    }

    /**
     * Возвращает ближайшую доску к позиции.
     *
     * @param worldName мир
     * @param x         координата X
     * @param y         координата Y
     * @param z         координата Z
     * @param maxDistance максимальное расстояние
     * @return ближайшая доска или null
     */
    @Nullable
    public QuestBoardLocation getNearestBoard(@Nonnull String worldName,
                                              int x, int y, int z,
                                              double maxDistance) {
        QuestBoardLocation nearest = null;
        double minDist = maxDistance * maxDistance;

        for (QuestBoardLocation board : boards.values()) {
            if (!board.getWorldName().equals(worldName)) continue;
            double dx = board.getX() - x;
            double dy = board.getY() - y;
            double dz = board.getZ() - z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < minDist) {
                minDist = distSq;
                nearest = board;
            }
        }
        return nearest;
    }

    // ═════════════════════════════════════════════════════════════
    //  SHUTDOWN
    // ═════════════════════════════════════════════════════════════

    public void shutdown() {
        boards.clear();
        boardsById.clear();
    }

    // ═════════════════════════════════════════════════════════════
    //  RESULT ENUM
    // ═════════════════════════════════════════════════════════════

    public enum PlaceResult {
        SUCCESS,
        ALREADY_EXISTS,
        NO_PERMISSION
    }
}
