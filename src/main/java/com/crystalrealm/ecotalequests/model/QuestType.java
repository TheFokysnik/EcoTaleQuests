package com.crystalrealm.ecotalequests.model;

/**
 * Тип квеста — определяет какое игровое действие нужно совершить.
 */
public enum QuestType {

    /** Убить определённое количество мобов. */
    KILL_MOB("kill_mob", "mob"),

    /** Добыть руду. */
    MINE_ORE("mine_ore", "ore"),

    /** Рубить деревья. */
    CHOP_WOOD("chop_wood", "wood"),

    /** Собрать урожай. */
    HARVEST_CROP("harvest_crop", "crop"),

    /** Заработать определённую сумму монет. */
    EARN_COINS("earn_coins", "coins"),

    /** Набрать определённое количество XP (требует RPG Leveling). */
    GAIN_XP("gain_xp", "xp");

    private final String id;
    private final String category;

    QuestType(String id, String category) {
        this.id = id;
        this.category = category;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }

    /**
     * Парсит тип квеста из строки (case-insensitive).
     */
    public static QuestType fromId(String id) {
        for (QuestType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
