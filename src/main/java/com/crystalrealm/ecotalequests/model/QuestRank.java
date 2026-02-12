package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Ранг квестовой гильдии (в стиле гильдии искателей приключений).
 *
 * <p>Иерархия: E → D → C → B → A → S.
 * Каждый ранг имеет порог очков (rank points), при достижении которого
 * игрок повышается. Более высокий ранг открывает доступ к сложным квестам.</p>
 */
public enum QuestRank {

    E("E", 0,    "#888888", "Новичок",   "Novice"),
    D("D", 100,  "#55ff55", "Ученик",    "Apprentice"),
    C("C", 300,  "#55ccff", "Рейнджер",  "Ranger"),
    B("B", 700,  "#aa55ff", "Ветеран",   "Veteran"),
    A("A", 1500, "#ffaa00", "Элита",     "Elite"),
    S("S", 3000, "#ff5555", "Легенда",   "Legend");

    private final String id;
    private final int requiredPoints;
    private final String color;
    private final String displayNameRu;
    private final String displayNameEn;

    QuestRank(String id, int requiredPoints, String color,
              String displayNameRu, String displayNameEn) {
        this.id = id;
        this.requiredPoints = requiredPoints;
        this.color = color;
        this.displayNameRu = displayNameRu;
        this.displayNameEn = displayNameEn;
    }

    public String getId() { return id; }
    public int getRequiredPoints() { return requiredPoints; }
    public String getColor() { return color; }
    public String getDisplayNameRu() { return displayNameRu; }
    public String getDisplayNameEn() { return displayNameEn; }

    /**
     * Возвращает следующий ранг или null, если это максимальный.
     */
    @Nullable
    public QuestRank next() {
        QuestRank[] values = values();
        int idx = ordinal() + 1;
        return idx < values.length ? values[idx] : null;
    }

    /**
     * Возвращает предыдущий ранг или null, если это минимальный.
     */
    @Nullable
    public QuestRank previous() {
        int idx = ordinal() - 1;
        return idx >= 0 ? values()[idx] : null;
    }

    /**
     * Может ли игрок с данным рангом брать квесты указанного ранга.
     */
    public boolean canAccess(@Nonnull QuestRank questRank) {
        return this.ordinal() >= questRank.ordinal();
    }

    /**
     * Определяет ранг по количеству очков (выбирает максимальный достижимый).
     */
    @Nonnull
    public static QuestRank fromPoints(int points) {
        QuestRank result = E;
        for (QuestRank rank : values()) {
            if (points >= rank.requiredPoints) {
                result = rank;
            } else {
                break;
            }
        }
        return result;
    }

    @Nullable
    public static QuestRank fromId(String id) {
        if (id == null) return null;
        for (QuestRank r : values()) {
            if (r.id.equalsIgnoreCase(id) || r.name().equalsIgnoreCase(id)) return r;
        }
        return null;
    }

    /**
     * Очки до следующего ранга. Если максимальный ранг — возвращает 0.
     */
    public int pointsToNext(int currentPoints) {
        QuestRank nextRank = next();
        if (nextRank == null) return 0;
        return Math.max(0, nextRank.requiredPoints - currentPoints);
    }
}
