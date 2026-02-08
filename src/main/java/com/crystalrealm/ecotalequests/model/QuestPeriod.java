package com.crystalrealm.ecotalequests.model;

/**
 * Период квеста — ежедневный или еженедельный.
 */
public enum QuestPeriod {

    /** Ежедневный квест — сбрасывается в полночь серверного времени. */
    DAILY("daily", 3),

    /** Еженедельный квест — сбрасывается в понедельник в полночь. */
    WEEKLY("weekly", 1);

    private final String id;
    private final int defaultMaxActive;

    QuestPeriod(String id, int defaultMaxActive) {
        this.id = id;
        this.defaultMaxActive = defaultMaxActive;
    }

    public String getId() { return id; }

    /** Максимальное количество одновременно активных квестов этого периода (по умолчанию). */
    public int getDefaultMaxActive() { return defaultMaxActive; }

    public static QuestPeriod fromId(String id) {
        for (QuestPeriod p : values()) {
            if (p.id.equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
        }
        return null;
    }
}
