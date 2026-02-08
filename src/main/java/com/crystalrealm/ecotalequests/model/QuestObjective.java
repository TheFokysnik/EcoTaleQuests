package com.crystalrealm.ecotalequests.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Цель квеста — что именно нужно сделать и в каком количестве.
 *
 * <p>Примеры:
 * <ul>
 *   <li>KILL_MOB + target="zombie" + requiredAmount=10</li>
 *   <li>MINE_ORE + target="iron" + requiredAmount=20</li>
 *   <li>EARN_COINS + target=null + requiredAmount=500.0</li>
 * </ul>
 */
public class QuestObjective {

    private final QuestType type;

    /**
     * Идентификатор цели (имя моба, тип руды и т.д.).
     * Может быть {@code null} для типов вроде EARN_COINS / GAIN_XP,
     * или {@code "*"} для любых мобов/блоков.
     */
    private final String target;

    /** Необходимое количество для завершения квеста. */
    private final double requiredAmount;

    public QuestObjective(@Nonnull QuestType type,
                          @Nullable String target,
                          double requiredAmount) {
        this.type = type;
        this.target = target;
        this.requiredAmount = requiredAmount;
    }

    @Nonnull
    public QuestType getType() { return type; }

    @Nullable
    public String getTarget() { return target; }

    public double getRequiredAmount() { return requiredAmount; }

    /**
     * Проверяет, подходит ли данное действие под эту цель.
     *
     * @param actionType    тип выполненного действия
     * @param actionTarget  конкретная цель действия (имя моба, руда и т.д.)
     * @return true если действие засчитывается для этого квеста
     */
    public boolean matches(@Nonnull QuestType actionType, @Nullable String actionTarget) {
        if (this.type != actionType) return false;

        // Если цель не указана или "*" — принимаем любую
        if (target == null || target.isEmpty() || "*".equals(target)) return true;

        // Иначе — точное совпадение (case-insensitive, partial match)
        if (actionTarget == null) return false;
        String lowerTarget = target.toLowerCase();
        String lowerAction = actionTarget.toLowerCase();
        return lowerAction.contains(lowerTarget) || lowerTarget.contains(lowerAction);
    }

    @Override
    public String toString() {
        return "QuestObjective{type=" + type +
                ", target='" + target + '\'' +
                ", required=" + requiredAmount + '}';
    }
}
