package com.crystalrealm.ecotalequests.generator;

import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.model.*;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Генератор квестов — создаёт пул daily/weekly квестов
 * на основе конфигурации и уровня игрока.
 *
 * <p>Квесты генерируются случайным образом из шаблонов конфига.
 * Генератор гарантирует разнообразие типов в пуле.</p>
 */
public class QuestGenerator {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final Random RANDOM = new Random();

    private final QuestsConfig config;

    public QuestGenerator(@Nonnull QuestsConfig config) {
        this.config = config;
    }

    // ═════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ═════════════════════════════════════════════════════════════

    /**
     * Генерирует пул ежедневных квестов.
     *
     * @param playerLevel уровень игрока (0+ для скейлинга)
     * @return список квестов для выбора
     */
    @Nonnull
    public List<Quest> generateDailyPool(int playerLevel) {
        int poolSize = config.getQuestLimits().getDailyPoolSize();
        return generatePool(QuestPeriod.DAILY, poolSize, playerLevel);
    }

    /**
     * Генерирует пул еженедельных квестов.
     */
    @Nonnull
    public List<Quest> generateWeeklyPool(int playerLevel) {
        int poolSize = config.getQuestLimits().getWeeklyPoolSize();
        return generatePool(QuestPeriod.WEEKLY, poolSize, playerLevel);
    }

    // ═════════════════════════════════════════════════════════════
    //  POOL GENERATION
    // ═════════════════════════════════════════════════════════════

    @Nonnull
    private List<Quest> generatePool(QuestPeriod period, int poolSize, int playerLevel) {
        List<QuestCandidate> candidates = buildCandidates(period, playerLevel);

        if (candidates.isEmpty()) {
            LOGGER.warn("No quest candidates available for {} (level={})", period, playerLevel);
            return Collections.emptyList();
        }

        // Перемешиваем для разнообразия
        Collections.shuffle(candidates, RANDOM);

        // Выбираем до poolSize, стараясь не дублировать типы
        List<Quest> pool = new ArrayList<>();
        Set<QuestType> usedTypes = new HashSet<>();

        // Первый проход: по одному каждого типа
        for (QuestCandidate c : candidates) {
            if (pool.size() >= poolSize) break;
            if (!usedTypes.contains(c.type)) {
                pool.add(createQuest(c, period, playerLevel));
                usedTypes.add(c.type);
            }
        }

        // Второй проход: заполняем оставшиеся слоты
        for (QuestCandidate c : candidates) {
            if (pool.size() >= poolSize) break;
            // Проверяем что нет дубликата по target
            boolean duplicate = pool.stream().anyMatch(q ->
                    q.getObjective().getType() == c.type &&
                    Objects.equals(q.getObjective().getTarget(), c.target));
            if (!duplicate) {
                pool.add(createQuest(c, period, playerLevel));
            }
        }

        LOGGER.info("Generated {} {} quests for level {}", pool.size(), period, playerLevel);
        return pool;
    }

    // ═════════════════════════════════════════════════════════════
    //  CANDIDATE BUILDING
    // ═════════════════════════════════════════════════════════════

    /**
     * Собирает все возможные кандидаты квестов из конфига.
     */
    private List<QuestCandidate> buildCandidates(QuestPeriod period, int playerLevel) {
        List<QuestCandidate> candidates = new ArrayList<>();
        QuestsConfig.GenerationSection gen = config.getGeneration();

        // Kill Mobs
        for (Map.Entry<String, QuestsConfig.QuestTemplate> entry : gen.getKillMobs().entrySet()) {
            QuestsConfig.QuestTemplate tpl = entry.getValue();
            if (playerLevel >= tpl.getMinLevel()) {
                candidates.add(new QuestCandidate(QuestType.KILL_MOB, entry.getKey(), tpl));
            }
        }

        // Mine Ores
        for (Map.Entry<String, QuestsConfig.QuestTemplate> entry : gen.getMineOres().entrySet()) {
            QuestsConfig.QuestTemplate tpl = entry.getValue();
            if (playerLevel >= tpl.getMinLevel()) {
                candidates.add(new QuestCandidate(QuestType.MINE_ORE, entry.getKey(), tpl));
            }
        }

        // Chop Wood
        for (Map.Entry<String, QuestsConfig.QuestTemplate> entry : gen.getChopWood().entrySet()) {
            QuestsConfig.QuestTemplate tpl = entry.getValue();
            if (playerLevel >= tpl.getMinLevel()) {
                candidates.add(new QuestCandidate(QuestType.CHOP_WOOD, entry.getKey(), tpl));
            }
        }

        // Harvest Crops
        for (Map.Entry<String, QuestsConfig.QuestTemplate> entry : gen.getHarvestCrops().entrySet()) {
            QuestsConfig.QuestTemplate tpl = entry.getValue();
            if (playerLevel >= tpl.getMinLevel()) {
                candidates.add(new QuestCandidate(QuestType.HARVEST_CROP, entry.getKey(), tpl));
            }
        }

        // Earn Coins (always available)
        QuestsConfig.QuestTemplate earnTpl = gen.getEarnCoins();
        if (earnTpl != null && playerLevel >= earnTpl.getMinLevel()) {
            candidates.add(new QuestCandidate(QuestType.EARN_COINS, null, earnTpl));
        }

        // Gain XP (always available)
        QuestsConfig.QuestTemplate xpTpl = gen.getGainXP();
        if (xpTpl != null && playerLevel >= xpTpl.getMinLevel()) {
            candidates.add(new QuestCandidate(QuestType.GAIN_XP, null, xpTpl));
        }

        return candidates;
    }

    // ═════════════════════════════════════════════════════════════
    //  QUEST CREATION
    // ═════════════════════════════════════════════════════════════

    private Quest createQuest(QuestCandidate candidate, QuestPeriod period, int playerLevel) {
        UUID questId = UUID.randomUUID();
        QuestsConfig.QuestTemplate tpl = candidate.template;

        // Рассчитываем количество с учётом периода
        int amount;
        if (period == QuestPeriod.WEEKLY) {
            amount = randomRange(tpl.getWeeklyMin(), tpl.getWeeklyMax());
        } else {
            amount = randomRange(tpl.getDailyMin(), tpl.getDailyMax());
        }

        // Скейлинг по уровню: +5% за каждые 10 уровней
        double levelScale = 1.0 + (playerLevel / 10) * 0.05;
        amount = (int) Math.round(amount * levelScale);

        QuestObjective objective = new QuestObjective(candidate.type, candidate.target, amount);

        // Рассчитываем награду
        QuestsConfig.RewardsSection rewards = config.getRewards();
        double baseCoins = period == QuestPeriod.WEEKLY ? rewards.getBaseWeeklyCoins() : rewards.getBaseDailyCoins();
        int baseXp = period == QuestPeriod.WEEKLY ? rewards.getBaseWeeklyXP() : rewards.getBaseDailyXP();

        // Нормализуем amount к единой шкале по типу квеста,
        // чтобы "gain_xp: 10000" не давало diffMult=51, а считалось
        // эквивалентно "kill_mob: 20".
        double normalizedAmount = normalizeAmount(candidate.type, amount);

        // Множитель сложности от нормализованного количества (диапазон ~1.0–2.5)
        double diffMult = 1.0 + (normalizedAmount / 100.0) * 0.5;

        // Применяем DifficultyMultiplier по типу квеста из конфига
        String typeKey = candidate.type.name().toLowerCase();
        Map<String, Double> diffMultipliers = rewards.getDifficultyMultipliers();
        if (diffMultipliers != null && diffMultipliers.containsKey(typeKey)) {
            diffMult *= diffMultipliers.get(typeKey);
        }

        baseCoins *= diffMult;
        baseXp = (int) (baseXp * diffMult);

        QuestReward reward = new QuestReward(Math.round(baseCoins * 100.0) / 100.0, baseXp);

        // Время жизни
        long now = System.currentTimeMillis();
        long expiresAt = calculateExpiration(period, now);

        // Имя и описание
        String name = generateQuestName(candidate.type, candidate.target, period);
        String description = generateQuestDescription(candidate.type, candidate.target, amount);

        return new Quest(questId, name, description, period, objective, reward,
                tpl.getMinLevel(), now, expiresAt);
    }

    // ═════════════════════════════════════════════════════════════
    //  TIME HELPERS
    // ═════════════════════════════════════════════════════════════

    /**
     * Рассчитывает время истечения квеста.
     */
    private long calculateExpiration(QuestPeriod period, long now) {
        LocalDateTime nowLdt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(now), ZoneId.systemDefault());

        LocalDateTime expires;
        if (period == QuestPeriod.WEEKLY) {
            // Следующий понедельник в 00:00
            String resetDay = config.getQuestLimits().getWeeklyResetDay();
            DayOfWeek day;
            try {
                day = DayOfWeek.valueOf(resetDay.toUpperCase());
            } catch (Exception e) {
                day = DayOfWeek.MONDAY;
            }
            expires = nowLdt.with(TemporalAdjusters.next(day)).withHour(0).withMinute(0).withSecond(0);
        } else {
            // Следующая полночь
            expires = nowLdt.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        }

        return expires.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ═════════════════════════════════════════════════════════════
    //  NAME GENERATION
    // ═════════════════════════════════════════════════════════════

    private String generateQuestName(QuestType type, String target, QuestPeriod period) {
        String prefix = period == QuestPeriod.WEEKLY ? "weekly" : "daily";
        String targetName = target != null ? target : type.getCategory();
        return prefix + "_" + type.getId() + "_" + targetName.toLowerCase().replace(" ", "_");
    }

    private String generateQuestDescription(QuestType type, String target, int amount) {
        String targetDisplay = target != null ? capitalize(target) : "";
        return switch (type) {
            case KILL_MOB -> "Kill " + amount + " " + targetDisplay;
            case MINE_ORE -> "Mine " + amount + " " + targetDisplay + " ore";
            case CHOP_WOOD -> "Chop " + amount + " " + targetDisplay + " wood";
            case HARVEST_CROP -> "Harvest " + amount + " " + targetDisplay;
            case EARN_COINS -> "Earn " + amount + " coins";
            case GAIN_XP -> "Gain " + amount + " XP";
        };
    }

    // ═════════════════════════════════════════════════════════════
    //  UTIL
    // ═════════════════════════════════════════════════════════════

    /**
     * Normalizes quest amount to a unified scale (~10–100) regardless of quest type.
     * Without this, "gain_xp: 10000" would produce diffMult=51 while "kill_mob: 20"
     * gives diffMult=1.1 — a 46× difference in reward for comparable difficulty.
     *
     * Each type has a divisor representing what "1 unit of normalized difficulty" means.
     * kill_mob: 1 kill ≈ 1 unit    →  20 kills → normalized 20
     * mine_ore: 1 ore ≈ 1 unit     →  50 ores  → normalized 50
     * chop_wood: 1 log ≈ 1 unit    →  60 logs  → normalized 60
     * harvest_crop: 1 crop ≈ 1     →  40 crops → normalized 40
     * earn_coins: ~10 coins ≈ 1    → 500 coins → normalized 50
     * gain_xp: ~100 XP ≈ 1        → 5000 XP   → normalized 50
     */
    private static double normalizeAmount(QuestType type, int amount) {
        double divisor = switch (type) {
            case KILL_MOB -> 1.0;
            case MINE_ORE -> 1.0;
            case CHOP_WOOD -> 1.0;
            case HARVEST_CROP -> 1.0;
            case EARN_COINS -> 10.0;
            case GAIN_XP -> 100.0;
        };
        return amount / divisor;
    }

    private static int randomRange(int min, int max) {
        if (min >= max) return min;
        return min + RANDOM.nextInt(max - min + 1);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Внутренний контейнер для предварительного кандидата. */
    private record QuestCandidate(QuestType type, String target, QuestsConfig.QuestTemplate template) {}
}
