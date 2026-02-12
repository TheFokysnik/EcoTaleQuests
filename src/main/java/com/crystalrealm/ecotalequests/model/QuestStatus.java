package com.crystalrealm.ecotalequests.model;

/**
 * Статус квеста для конкретного игрока.
 */
public enum QuestStatus {

    /** Квест доступен для принятия. */
    AVAILABLE("available"),

    /** Квест принят, в процессе выполнения. */
    ACTIVE("active"),

    /** Квест выполнен, награда выдана. */
    COMPLETED("completed"),

    /** Квест провален (истёк срок пула daily/weekly). */
    EXPIRED("expired"),

    /** Квест провален (истёк таймер выполнения). */
    FAILED("failed"),

    /** Квест отменён игроком. */
    ABANDONED("abandoned");

    private final String id;

    QuestStatus(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    /** Можно ли учитывать прогресс для этого статуса. */
    public boolean isTrackable() {
        return this == ACTIVE;
    }

    public static QuestStatus fromId(String id) {
        for (QuestStatus s : values()) {
            if (s.id.equalsIgnoreCase(id) || s.name().equalsIgnoreCase(id)) return s;
        }
        return null;
    }
}
