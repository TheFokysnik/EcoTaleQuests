package com.crystalrealm.ecotalequests.config;

import java.util.*;

/**
 * POJO-модель конфигурации плагина EcoTaleQuests.
 * Десериализуется из JSON через Gson.
 */
public class QuestsConfig {

    private GeneralSection General = new GeneralSection();
    private QuestLimitsSection QuestLimits = new QuestLimitsSection();
    private GenerationSection Generation = new GenerationSection();
    private RewardsSection Rewards = new RewardsSection();
    private ProtectionSection Protection = new ProtectionSection();

    // ─── Getters ──────────────────────────────────────────────────

    public GeneralSection getGeneral() { return General; }
    public QuestLimitsSection getQuestLimits() { return QuestLimits; }
    public GenerationSection getGeneration() { return Generation; }
    public RewardsSection getRewards() { return Rewards; }
    public ProtectionSection getProtection() { return Protection; }

    // ═════════════════════════════════════════════════════════════
    //  Секции
    // ═════════════════════════════════════════════════════════════

    /** Общие настройки. */
    public static class GeneralSection {
        private boolean DebugMode = false;
        private String Language = "ru";
        private String MessagePrefix = "<dark_gray>[<gold>⚔<dark_gray>]";
        private boolean NotifyOnProgress = true;
        private boolean NotifyOnComplete = true;
        private int AutoSaveIntervalMinutes = 5;

        public boolean isDebugMode() { return DebugMode; }
        public void setDebugMode(boolean v) { this.DebugMode = v; }
        public String getLanguage() { return Language; }
        public void setLanguage(String l) { this.Language = l; }
        public String getMessagePrefix() { return MessagePrefix; }
        public boolean isNotifyOnProgress() { return NotifyOnProgress; }
        public boolean isNotifyOnComplete() { return NotifyOnComplete; }
        public int getAutoSaveIntervalMinutes() { return AutoSaveIntervalMinutes; }
    }

    /** Лимиты квестов. */
    public static class QuestLimitsSection {
        private int MaxDailyActive = 3;
        private int MaxWeeklyActive = 1;
        private int DailyPoolSize = 6;
        private int WeeklyPoolSize = 3;
        private int MaxAbandonPerDay = 2;
        private String DailyResetTime = "00:00";
        private String WeeklyResetDay = "MONDAY";

        public int getMaxDailyActive() { return MaxDailyActive; }
        public int getMaxWeeklyActive() { return MaxWeeklyActive; }
        public int getDailyPoolSize() { return DailyPoolSize; }
        public int getWeeklyPoolSize() { return WeeklyPoolSize; }
        public int getMaxAbandonPerDay() { return MaxAbandonPerDay; }
        public String getDailyResetTime() { return DailyResetTime; }
        public String getWeeklyResetDay() { return WeeklyResetDay; }
    }

    /** Настройки генерации квестов. */
    public static class GenerationSection {
        private Map<String, QuestTemplate> KillMobs = new LinkedHashMap<>();
        private Map<String, QuestTemplate> MineOres = new LinkedHashMap<>();
        private Map<String, QuestTemplate> ChopWood = new LinkedHashMap<>();
        private Map<String, QuestTemplate> HarvestCrops = new LinkedHashMap<>();
        private QuestTemplate EarnCoins = new QuestTemplate(100, 500, 50, 250, 0);
        private QuestTemplate GainXP = new QuestTemplate(50, 200, 100, 500, 0);

        public Map<String, QuestTemplate> getKillMobs() { return KillMobs; }
        public Map<String, QuestTemplate> getMineOres() { return MineOres; }
        public Map<String, QuestTemplate> getChopWood() { return ChopWood; }
        public Map<String, QuestTemplate> getHarvestCrops() { return HarvestCrops; }
        public QuestTemplate getEarnCoins() { return EarnCoins; }
        public QuestTemplate getGainXP() { return GainXP; }
    }

    /**
     * Шаблон для генерации квеста — диапазоны количества (daily/weekly)
     * и минимальный уровень.
     */
    public static class QuestTemplate {
        private int DailyMin = 5;
        private int DailyMax = 20;
        private int WeeklyMin = 20;
        private int WeeklyMax = 100;
        private int MinLevel = 0;

        public QuestTemplate() {}

        public QuestTemplate(int dailyMin, int dailyMax, int weeklyMin, int weeklyMax, int minLevel) {
            this.DailyMin = dailyMin;
            this.DailyMax = dailyMax;
            this.WeeklyMin = weeklyMin;
            this.WeeklyMax = weeklyMax;
            this.MinLevel = minLevel;
        }

        public int getDailyMin() { return DailyMin; }
        public int getDailyMax() { return DailyMax; }
        public int getWeeklyMin() { return WeeklyMin; }
        public int getWeeklyMax() { return WeeklyMax; }
        public int getMinLevel() { return MinLevel; }
    }

    /** Настройки наград. */
    public static class RewardsSection {
        private double BaseDailyCoins = 50.0;
        private double BaseWeeklyCoins = 200.0;
        private int BaseDailyXP = 25;
        private int BaseWeeklyXP = 100;
        private double LevelScalingFactor = 0.02;
        private double MaxLevelMultiplier = 3.0;
        private Map<String, Double> DifficultyMultipliers = new LinkedHashMap<>();

        public double getBaseDailyCoins() { return BaseDailyCoins; }
        public double getBaseWeeklyCoins() { return BaseWeeklyCoins; }
        public int getBaseDailyXP() { return BaseDailyXP; }
        public int getBaseWeeklyXP() { return BaseWeeklyXP; }
        public double getLevelScalingFactor() { return LevelScalingFactor; }
        public double getMaxLevelMultiplier() { return MaxLevelMultiplier; }
        public Map<String, Double> getDifficultyMultipliers() { return DifficultyMultipliers; }

        /**
         * Рассчитывает множитель уровня: 1.0 + level * factor, capped.
         */
        public double getLevelMultiplier(int playerLevel) {
            double mult = 1.0 + playerLevel * LevelScalingFactor;
            return Math.min(mult, MaxLevelMultiplier);
        }
    }

    /** Защита от абьюза. */
    public static class ProtectionSection {
        private boolean RequireOnline = true;
        private int MinPlaytimeMinutes = 5;
        private long QuestAcceptCooldownMs = 1000;
        private boolean PreventDuplicateTypes = true;
        private List<String> AllowedWorlds = List.of();

        public boolean isRequireOnline() { return RequireOnline; }
        public int getMinPlaytimeMinutes() { return MinPlaytimeMinutes; }
        public long getQuestAcceptCooldownMs() { return QuestAcceptCooldownMs; }
        public boolean isPreventDuplicateTypes() { return PreventDuplicateTypes; }
        public List<String> getAllowedWorlds() { return AllowedWorlds; }
    }
}
