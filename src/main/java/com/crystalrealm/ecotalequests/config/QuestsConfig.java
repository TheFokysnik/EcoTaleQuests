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
    private VipTiersSection VipTiers = new VipTiersSection();
    private ProtectionSection Protection = new ProtectionSection();
    private RanksSection Ranks = new RanksSection();
    private BoardsSection Boards = new BoardsSection();
    private TimerSection Timers = new TimerSection();

    // ─── Getters ──────────────────────────────────────────────────

    public GeneralSection getGeneral() { return General; }
    public QuestLimitsSection getQuestLimits() { return QuestLimits; }
    public GenerationSection getGeneration() { return Generation; }
    public RewardsSection getRewards() { return Rewards; }
    public VipTiersSection getVipTiers() { return VipTiers; }
    public ProtectionSection getProtection() { return Protection; }
    public RanksSection getRanks() { return Ranks; }
    public BoardsSection getBoards() { return Boards; }
    public TimerSection getTimers() { return Timers; }

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
        public void setNotifyOnProgress(boolean v) { this.NotifyOnProgress = v; }
        public boolean isNotifyOnComplete() { return NotifyOnComplete; }
        public void setNotifyOnComplete(boolean v) { this.NotifyOnComplete = v; }
        public int getAutoSaveIntervalMinutes() { return AutoSaveIntervalMinutes; }
        public void setAutoSaveIntervalMinutes(int v) { this.AutoSaveIntervalMinutes = v; }
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
        public void setMaxDailyActive(int v) { this.MaxDailyActive = v; }
        public int getMaxWeeklyActive() { return MaxWeeklyActive; }
        public void setMaxWeeklyActive(int v) { this.MaxWeeklyActive = v; }
        public int getDailyPoolSize() { return DailyPoolSize; }
        public void setDailyPoolSize(int v) { this.DailyPoolSize = v; }
        public int getWeeklyPoolSize() { return WeeklyPoolSize; }
        public void setWeeklyPoolSize(int v) { this.WeeklyPoolSize = v; }
        public int getMaxAbandonPerDay() { return MaxAbandonPerDay; }
        public void setMaxAbandonPerDay(int v) { this.MaxAbandonPerDay = v; }
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

    /**
     * VIP-тиры: список из Permission → Multiplier.
     * Проверяются сверху вниз, применяется первый подходящий (наивысший приоритет).
     */
    public static class VipTiersSection {
        private List<VipTier> Tiers = new ArrayList<>();

        public VipTiersSection() {
            // Defaults: MVP_Plus > MVP > VIP > base (1.0)
            Tiers.add(new VipTier("ecotalequests.multiplier.mvp_plus", 2.0, "MVP+"));
            Tiers.add(new VipTier("ecotalequests.multiplier.mvp", 1.5, "MVP"));
            Tiers.add(new VipTier("ecotalequests.multiplier.vip", 1.25, "VIP"));
        }

        public List<VipTier> getTiers() { return Tiers; }
    }

    /** Один VIP-тир: permission, множитель наград, отображаемое имя. */
    public static class VipTier {
        private String Permission;
        private double Multiplier;
        private String DisplayName;

        public VipTier() {}

        public VipTier(String permission, double multiplier, String displayName) {
            this.Permission = permission;
            this.Multiplier = multiplier;
            this.DisplayName = displayName;
        }

        public String getPermission() { return Permission; }
        public double getMultiplier() { return Multiplier; }
        public String getDisplayName() { return DisplayName; }
    }

    /** Защита от абьюза. */
    public static class ProtectionSection {
        private boolean RequireOnline = true;
        private int MinPlaytimeMinutes = 5;
        private long QuestAcceptCooldownMs = 1000;
        private boolean PreventDuplicateTypes = true;
        private List<String> AllowedWorlds = List.of();

        public boolean isRequireOnline() { return RequireOnline; }
        public void setRequireOnline(boolean v) { this.RequireOnline = v; }
        public int getMinPlaytimeMinutes() { return MinPlaytimeMinutes; }
        public long getQuestAcceptCooldownMs() { return QuestAcceptCooldownMs; }
        public boolean isPreventDuplicateTypes() { return PreventDuplicateTypes; }
        public void setPreventDuplicateTypes(boolean v) { this.PreventDuplicateTypes = v; }
        public List<String> getAllowedWorlds() { return AllowedWorlds; }
    }

    /** Система рангов квестовой гильдии. */
    public static class RanksSection {
        private boolean Enabled = true;
        private boolean PenalizeOnFail = true;
        private int DefaultFailPenalty = 15;
        private int BaseRankPoints = 10;
        private Map<String, Integer> RankThresholds = new LinkedHashMap<>();

        public RanksSection() {
            RankThresholds.put("E", 0);
            RankThresholds.put("D", 100);
            RankThresholds.put("C", 300);
            RankThresholds.put("B", 700);
            RankThresholds.put("A", 1500);
            RankThresholds.put("S", 3000);
        }

        public boolean isEnabled() { return Enabled; }
        public void setEnabled(boolean v) { this.Enabled = v; }
        public boolean isPenalizeOnFail() { return PenalizeOnFail; }
        public void setPenalizeOnFail(boolean v) { this.PenalizeOnFail = v; }
        public int getDefaultFailPenalty() { return DefaultFailPenalty; }
        public void setDefaultFailPenalty(int v) { this.DefaultFailPenalty = v; }
        public int getBaseRankPoints() { return BaseRankPoints; }
        public Map<String, Integer> getRankThresholds() { return RankThresholds; }
    }

    /** Настройки досок квестов. */
    public static class BoardsSection {
        private boolean Enabled = true;
        /**
         * Quest access mode:
         * <ul>
         *   <li><b>both</b> — quests available via GUI commands AND physical board (default)</li>
         *   <li><b>board_only</b> — only physical board interaction opens quest panel</li>
         *   <li><b>gui_only</b> — only /quests gui command opens quest panel</li>
         * </ul>
         */
        private String QuestAccessMode = "both";

        public boolean isEnabled() { return Enabled; }
        public void setEnabled(boolean v) { this.Enabled = v; }
        public String getQuestAccessMode() { return QuestAccessMode != null ? QuestAccessMode : "both"; }
        public void setQuestAccessMode(String v) { this.QuestAccessMode = v; }
        public boolean isBoardAllowed() { String m = getQuestAccessMode(); return "both".equalsIgnoreCase(m) || "board_only".equalsIgnoreCase(m); }
        public boolean isGuiCommandAllowed() { String m = getQuestAccessMode(); return "both".equalsIgnoreCase(m) || "gui_only".equalsIgnoreCase(m); }
    }

    /** Настройки таймеров квестов. */
    public static class TimerSection {
        private int DefaultDurationMinutes = 0;
        private long RelogGracePeriodMs = 60000;
        private int TimerCheckIntervalSeconds = 10;
        private boolean NotifyTimerWarnings = true;
        private List<Integer> WarningMinutes = List.of(10, 5, 1);

        public int getDefaultDurationMinutes() { return DefaultDurationMinutes; }
        public long getRelogGracePeriodMs() { return RelogGracePeriodMs; }
        public int getTimerCheckIntervalSeconds() { return TimerCheckIntervalSeconds; }
        public boolean isNotifyTimerWarnings() { return NotifyTimerWarnings; }
        public List<Integer> getWarningMinutes() { return WarningMinutes; }
    }
}
