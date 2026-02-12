package com.crystalrealm.ecotalequests.model;

/**
 * Тип доступности квеста — определяет, сколько игроков могут принять квест одновременно.
 *
 * <ul>
 *   <li>{@link #INDIVIDUAL} — стандартный, каждый игрок может принять независимо</li>
 *   <li>{@link #GLOBAL_UNIQUE} — только один игрок на весь сервер</li>
 *   <li>{@link #LIMITED_SLOTS} — ограниченное число одновременных участников</li>
 * </ul>
 */
public enum QuestAccessType {

    /** Индивидуальный квест — каждый игрок может принять свою копию. */
    INDIVIDUAL("individual"),

    /** Глобально уникальный — только один игрок может выполнять. */
    GLOBAL_UNIQUE("global_unique"),

    /** Ограниченные слоты — maxSlots одновременных участников. */
    LIMITED_SLOTS("limited_slots");

    private final String id;

    QuestAccessType(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public static QuestAccessType fromId(String id) {
        if (id == null) return INDIVIDUAL;
        for (QuestAccessType t : values()) {
            if (t.id.equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) return t;
        }
        return INDIVIDUAL;
    }
}
