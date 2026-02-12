package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Данные о локации доски квестов в мире.
 *
 * <p>Сохраняется персистентно. При взаимодействии игрока с блоком
 * на этой позиции — открывается GUI квестов.</p>
 */
public class QuestBoardLocation {

    /** Тип визуального варианта доски. */
    public enum BoardType {
        /** Уличная, на столбе. */
        OUTDOOR("outdoor"),
        /** Внутренняя, настенная. */
        INDOOR("indoor");

        private final String id;
        BoardType(String id) { this.id = id; }
        public String getId() { return id; }

        public static BoardType fromId(String id) {
            if (id == null) return OUTDOOR;
            for (BoardType t : values()) {
                if (t.id.equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) return t;
            }
            return OUTDOOR;
        }
    }

    private final UUID boardId;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final BoardType type;
    private final UUID placedBy;
    private final long placedAt;

    public QuestBoardLocation(@Nonnull UUID boardId,
                              @Nonnull String worldName,
                              int x, int y, int z,
                              @Nonnull BoardType type,
                              @Nonnull UUID placedBy,
                              long placedAt) {
        this.boardId = boardId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.placedBy = placedBy;
        this.placedAt = placedAt;
    }

    public static QuestBoardLocation create(@Nonnull String worldName,
                                            int x, int y, int z,
                                            @Nonnull BoardType type,
                                            @Nonnull UUID placedBy) {
        return new QuestBoardLocation(
                UUID.randomUUID(), worldName, x, y, z,
                type, placedBy, System.currentTimeMillis()
        );
    }

    // ── Getters ─────────────────────────────────────────────────

    @Nonnull public UUID getBoardId() { return boardId; }
    @Nonnull public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    @Nonnull public BoardType getType() { return type; }
    @Nonnull public UUID getPlacedBy() { return placedBy; }
    public long getPlacedAt() { return placedAt; }

    /**
     * Проверяет, совпадает ли указанная позиция с позицией доски.
     */
    public boolean matchesPosition(@Nonnull String world, int bx, int by, int bz) {
        return this.worldName.equals(world)
                && this.x == bx && this.y == by && this.z == bz;
    }

    /**
     * Уникальный ключ позиции для карты.
     */
    @Nonnull
    public String positionKey() {
        return worldName + ":" + x + ":" + y + ":" + z;
    }

    @Override
    public String toString() {
        return "QuestBoard{" + type.id + " at " + worldName + " " + x + "," + y + "," + z + '}';
    }
}
