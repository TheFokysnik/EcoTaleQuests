package com.crystalrealm.ecotalequests;

import com.crystalrealm.ecotalequests.commands.QuestsCommandCollection;
import com.crystalrealm.ecotalequests.config.ConfigManager;
import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.generator.QuestGenerator;
import com.crystalrealm.ecotalequests.lang.LangManager;
import com.crystalrealm.ecotalequests.listeners.BlockQuestListener;
import com.crystalrealm.ecotalequests.listeners.CoinQuestListener;
import com.crystalrealm.ecotalequests.listeners.MobDeathQuestSystem;
import com.crystalrealm.ecotalequests.listeners.MobKillQuestListener;
import com.crystalrealm.ecotalequests.listeners.QuestBoardInteractionListener;
import com.crystalrealm.ecotalequests.model.QuestPeriod;
import com.crystalrealm.ecotalequests.protection.QuestAbuseGuard;
import com.crystalrealm.ecotalequests.reward.QuestRewardCalculator;
import com.crystalrealm.ecotalequests.service.QuestAvailabilityManager;
import com.crystalrealm.ecotalequests.service.QuestBoardManager;
import com.crystalrealm.ecotalequests.service.QuestRankService;
import com.crystalrealm.ecotalequests.service.TimerService;
import com.crystalrealm.ecotalequests.storage.JsonQuestStorage;
import com.crystalrealm.ecotalequests.storage.QuestStorage;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.provider.economy.EconomyBridge;
import com.crystalrealm.ecotalequests.provider.economy.GenericEconomyProvider;
import com.crystalrealm.ecotalequests.provider.leveling.GenericLevelProvider;
import com.crystalrealm.ecotalequests.provider.leveling.LevelBridge;
import com.crystalrealm.ecotalequests.provider.leveling.MMOSkillTreeProvider;
import com.crystalrealm.ecotalequests.provider.leveling.RPGLevelingProvider;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PermissionHelper;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * EcoTaleQuests — система ежедневных и еженедельных квестов
 * для Hytale серверов с интеграцией EcoTale API.
 *
 * <h3>Возможности</h3>
 * <ul>
 *   <li>Генерация daily/weekly квестов на основе конфига</li>
 *   <li>6 типов квестов: Kill, Mine, Chop, Harvest, Earn, XP</li>
 *   <li>Скейлинг наград по уровню через RPG Leveling</li>
 *   <li>JSON-хранилище прогресса игроков</li>
 *   <li>Полная система команд с RU/EN локализацией</li>
 *   <li>Anti-abuse: cooldown, лимиты отмен, дедупликация</li>
 * </ul>
 *
 * @version 1.4.0
 */
public class EcoTaleQuestsPlugin extends JavaPlugin {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final String VERSION = "1.4.0";

    // ── Services ────────────────────────────────────────────────
    private ConfigManager configManager;
    private LangManager langManager;
    private QuestStorage storage;
    private QuestGenerator questGenerator;
    private QuestRewardCalculator rewardCalculator;
    private QuestTracker questTracker;
    private QuestAbuseGuard abuseGuard;
    private QuestRankService rankService;
    private QuestAvailabilityManager availabilityManager;
    private TimerService timerService;
    private QuestBoardManager boardManager;

    // ── Listeners ───────────────────────────────────────────────
    private MobKillQuestListener mobKillListener;
    private MobDeathQuestSystem mobDeathQuestSystem;
    private BlockQuestListener blockQuestListener;
    private CoinQuestListener coinQuestListener;
    private QuestBoardInteractionListener boardInteractionListener;

    // ── Provider Bridges ────────────────────────────────────────
    private EconomyBridge economyBridge;
    private LevelBridge levelBridge;

    // ── Scheduled tasks ─────────────────────────────────────────
    private ScheduledFuture<?> autoSaveTask;
    private ScheduledFuture<?> poolRefreshTask;
    private ScheduledFuture<?> timerTickTask;

    public EcoTaleQuestsPlugin(JavaPluginInit init) {
        super(init);
    }

    // ═════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════════════

    @Override
    protected void setup() {
        LOGGER.info("═══════════════════════════════════════");
        LOGGER.info("  EcoTaleQuests v{} — setup", VERSION);
        LOGGER.info("═══════════════════════════════════════");

        // 1. Config
        configManager = new ConfigManager(getDataDirectory());
        configManager.loadOrCreate();

        // 1b. Permission resolver (reads permissions.json for group-based checks)
        PermissionHelper.getInstance().init(getDataDirectory());
        QuestsConfig config = configManager.getConfig();

        // 2. Lang
        langManager = new LangManager(getDataDirectory());
        langManager.load(config.getGeneral().getLanguage());

        // 3. Storage
        storage = new JsonQuestStorage(getDataDirectory());
        storage.initialize();

        // 4. Generator
        questGenerator = new QuestGenerator(config);

        // 5. Reward calculator (EconomyBridge injected in start())
        rewardCalculator = new QuestRewardCalculator(config);

        // 6. Anti-abuse
        abuseGuard = new QuestAbuseGuard(config);

        // 6a. Rank service
        rankService = new QuestRankService(storage, config, langManager);

        // 6b. Availability manager
        availabilityManager = new QuestAvailabilityManager(storage);
        availabilityManager.initialize();

        // 6c. Timer service
        timerService = new TimerService();

        // 6d. Board manager
        boardManager = new QuestBoardManager(storage);
        boardManager.initialize();

        // 7. Quest tracker
        questTracker = new QuestTracker(config, storage, questGenerator, rewardCalculator, langManager,
                rankService, availabilityManager, timerService);

        // 8. Commands
        getCommandRegistry().registerCommand(new QuestsCommandCollection(this));
        LOGGER.info("Registered /quests command.");

        // 9. Register ECS systems (MUST be in setup(), before world starts ticking)
        // LevelBridge injected in start() after providers are activated
        mobDeathQuestSystem = new MobDeathQuestSystem(questTracker);
        getEntityStoreRegistry().registerSystem(mobDeathQuestSystem);
        LOGGER.info("MobDeathQuestSystem registered (native ECS death tracking).");

        blockQuestListener = new BlockQuestListener(questTracker);
        blockQuestListener.register(getEntityStoreRegistry());

        // 10. Register quest board interaction listener (physical boards)
        boardInteractionListener = new QuestBoardInteractionListener(this);
        boardInteractionListener.register(getEntityStoreRegistry());
    }

    @Override
    protected void start() {
        LOGGER.info("EcoTaleQuests starting...");
        QuestsConfig config = configManager.getConfig();

        // ── Economy Bridge ──
        economyBridge = new EconomyBridge();
        var genericEco = config.getGenericEconomy();
        if (genericEco.isConfigured()) {
            economyBridge.registerProvider("generic", new GenericEconomyProvider(
                    genericEco.getClassName(), genericEco.getInstanceMethod(),
                    genericEco.getDepositMethod(), genericEco.getBalanceMethod(),
                    genericEco.isDepositHasReason()));
        }
        economyBridge.activate(config.getGeneral().getEconomyProvider());
        LOGGER.info("Economy provider: {}", economyBridge.getProviderName());

        // ── Level Bridge ──
        levelBridge = new LevelBridge();
        levelBridge.registerProvider("mmoskilltree", new MMOSkillTreeProvider(
                config.getMMOSkillTree().getDefaultSkillType()));
        var genericLvl = config.getGenericLeveling();
        if (genericLvl.isConfigured()) {
            levelBridge.registerProvider("generic", new GenericLevelProvider(
                    genericLvl.getClassName(), genericLvl.getInstanceMethod(),
                    genericLvl.getGetLevelMethod(), genericLvl.getGrantXPMethod()));
        }
        levelBridge.activate(config.getGeneral().getLevelProvider());
        LOGGER.info("Level provider: {}", levelBridge.getProviderName());

        // ── Inject bridges into components ──
        rewardCalculator.setEconomyBridge(economyBridge);
        rewardCalculator.setLevelBridge(levelBridge);
        blockQuestListener.setLevelBridge(levelBridge);
        mobDeathQuestSystem.setLevelBridge(levelBridge);

        // ── Register RPG Leveling listener (XP quests via event subscription) ──
        mobKillListener = new MobKillQuestListener(questTracker);
        Object rawRpgApi = null;
        for (var entry : java.util.List.of("rpgleveling")) {
            if (levelBridge.isAvailable()) {
                // Try to get raw API for event subscription
                try {
                    var providers = LevelBridge.class.getDeclaredField("providers");
                    providers.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    var map = (java.util.Map<String, ?>) providers.get(levelBridge);
                    var rpgProvider = map.get("rpgleveling");
                    if (rpgProvider instanceof RPGLevelingProvider rp) {
                        rawRpgApi = rp.getRawApi();
                    }
                } catch (Exception ignored) {}
            }
        }
        mobKillListener.registerWithRawApi(rawRpgApi);

        // ── Register coin listener ──
        coinQuestListener = new CoinQuestListener(questTracker, economyBridge, levelBridge);
        coinQuestListener.register();

        // ── Generate initial quest pools ──
        int avgLevel = levelBridge.isAvailable() ? 5 : 1;
        questTracker.refreshPools(avgLevel);

        // ── Restore quest timers from persistent assignments ──
        int restored = timerService.restoreTimers(storage.loadActiveAssignments());
        LOGGER.info("Restored {} quest timers from disk.", restored);

        // ── Schedule timer tick ──
        QuestsConfig cfg = configManager.getConfig();
        int timerInterval = cfg.getTimers().getTimerCheckIntervalSeconds();
        timerTickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    try {
                        timerService.tick();
                    } catch (Exception e) {
                        LOGGER.error("Timer tick failed", e);
                    }
                },
                timerInterval, timerInterval, TimeUnit.SECONDS
        );

        // ── Schedule auto-save ──
        int saveInterval = configManager.getConfig().getGeneral().getAutoSaveIntervalMinutes();
        autoSaveTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    try {
                        storage.save();
                    } catch (Exception e) {
                        LOGGER.error("Auto-save failed", e);
                    }
                },
                saveInterval, saveInterval, TimeUnit.MINUTES
        );

        // ── Schedule pool refresh + expiry check every minute ──
        poolRefreshTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    try {
                        questTracker.checkExpiredQuests();
                        questTracker.refreshPools(avgLevel);
                    } catch (Exception e) {
                        LOGGER.error("Pool refresh failed", e);
                    }
                },
                1, 1, TimeUnit.MINUTES
        );

        LOGGER.info("═══════════════════════════════════════");
        LOGGER.info("  EcoTaleQuests v{} — STARTED", VERSION);
        LOGGER.info("  Economy:           {} ({})", config.getGeneral().getEconomyProvider(), economyBridge.getProviderName());
        LOGGER.info("  Leveling:          {} ({})", config.getGeneral().getLevelProvider(), levelBridge.getProviderName());
        LOGGER.info("  Mob kill tracking: ACTIVE (native ECS DeathSystem)");
        LOGGER.info("  XP tracking:       {}", mobKillListener.isRegistered() ? "ACTIVE" : "DISABLED");
        LOGGER.info("  Block tracking:    ACTIVE");
        LOGGER.info("  Coin tracking:     {}", coinQuestListener.isRegistered() ? "ACTIVE" : "DISABLED");
        LOGGER.info("  Daily pool:        {} quests", storage.loadQuestPool(QuestPeriod.DAILY).size());
        LOGGER.info("  Weekly pool:       {} quests", storage.loadQuestPool(QuestPeriod.WEEKLY).size());
        LOGGER.info("  Ranks:             {}", config.getRanks().isEnabled() ? "ENABLED" : "DISABLED");
        LOGGER.info("  Boards:            {} placed", boardManager.getAllBoards().size());
        LOGGER.info("  Timer tick:        every {}s", timerInterval);
        LOGGER.info("═══════════════════════════════════════");
    }

    @Override
    protected void shutdown() {
        LOGGER.info("EcoTaleQuests shutting down...");

        // Cancel scheduled tasks
        if (autoSaveTask != null) autoSaveTask.cancel(false);
        if (poolRefreshTask != null) poolRefreshTask.cancel(false);
        if (timerTickTask != null) timerTickTask.cancel(false);
        if (coinQuestListener != null) coinQuestListener.shutdown();

        // Save all data
        if (storage != null) storage.shutdown();

        // Cleanup
        if (abuseGuard != null) abuseGuard.cleanup();
        MessageUtil.clearCache();
        if (langManager != null) langManager.clearPlayerData();

        LOGGER.info("EcoTaleQuests v{} — shutdown complete.", VERSION);
    }

    // ═════════════════════════════════════════════════════════════
    //  GETTERS
    // ═════════════════════════════════════════════════════════════

    @Nonnull public EconomyBridge getEconomyBridge() { return economyBridge; }
    @Nonnull public LevelBridge getLevelBridge() { return levelBridge; }

    @Nonnull public ConfigManager getConfigManager() { return configManager; }
    @Nonnull public LangManager getLangManager() { return langManager; }
    @Nonnull public QuestStorage getStorage() { return storage; }
    @Nonnull public QuestTracker getQuestTracker() { return questTracker; }
    @Nonnull public QuestAbuseGuard getAbuseGuard() { return abuseGuard; }
    @Nonnull public QuestRewardCalculator getRewardCalculator() { return rewardCalculator; }
    @Nonnull public QuestRankService getRankService() { return rankService; }
    @Nonnull public QuestAvailabilityManager getAvailabilityManager() { return availabilityManager; }
    @Nonnull public TimerService getTimerService() { return timerService; }
    @Nonnull public QuestBoardManager getBoardManager() { return boardManager; }
    @Nonnull public String getVersion() { return VERSION; }
}
