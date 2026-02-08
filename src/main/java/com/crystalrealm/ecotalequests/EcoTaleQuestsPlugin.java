package com.crystalrealm.ecotalequests;

import com.crystalrealm.ecotalequests.commands.QuestsCommandCollection;
import com.crystalrealm.ecotalequests.config.ConfigManager;
import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.generator.QuestGenerator;
import com.crystalrealm.ecotalequests.lang.LangManager;
import com.crystalrealm.ecotalequests.listeners.BlockQuestListener;
import com.crystalrealm.ecotalequests.listeners.CoinQuestListener;
import com.crystalrealm.ecotalequests.listeners.MobKillQuestListener;
import com.crystalrealm.ecotalequests.model.QuestPeriod;
import com.crystalrealm.ecotalequests.protection.QuestAbuseGuard;
import com.crystalrealm.ecotalequests.reward.QuestRewardCalculator;
import com.crystalrealm.ecotalequests.storage.JsonQuestStorage;
import com.crystalrealm.ecotalequests.storage.QuestStorage;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import org.zuxaw.plugin.api.RPGLevelingAPI;

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
 * @version 1.0.1
 */
public class EcoTaleQuestsPlugin extends JavaPlugin {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final String VERSION = "1.0.1";

    // ── Services ────────────────────────────────────────────────
    private ConfigManager configManager;
    private LangManager langManager;
    private QuestStorage storage;
    private QuestGenerator questGenerator;
    private QuestRewardCalculator rewardCalculator;
    private QuestTracker questTracker;
    private QuestAbuseGuard abuseGuard;

    // ── Listeners ───────────────────────────────────────────────
    private MobKillQuestListener mobKillListener;
    private BlockQuestListener blockQuestListener;
    private CoinQuestListener coinQuestListener;

    // ── RPG Integration ─────────────────────────────────────────
    private RPGLevelingAPI rpgApi;

    // ── Scheduled tasks ─────────────────────────────────────────
    private ScheduledFuture<?> autoSaveTask;
    private ScheduledFuture<?> poolRefreshTask;

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
        QuestsConfig config = configManager.getConfig();

        // 2. Lang
        langManager = new LangManager(getDataDirectory());
        langManager.load(config.getGeneral().getLanguage());

        // 3. Storage
        storage = new JsonQuestStorage(getDataDirectory());
        storage.initialize();

        // 4. Generator
        questGenerator = new QuestGenerator(config);

        // 5. Reward calculator
        rewardCalculator = new QuestRewardCalculator(config);

        // 6. Anti-abuse
        abuseGuard = new QuestAbuseGuard(config);

        // 7. Quest tracker
        questTracker = new QuestTracker(config, storage, questGenerator, rewardCalculator, langManager);

        // 8. Commands
        getCommandRegistry().registerCommand(new QuestsCommandCollection(this));
        LOGGER.info("Registered /quests command.");
    }

    @Override
    protected void start() {
        LOGGER.info("EcoTaleQuests starting...");

        // ── RPG Leveling detection ──
        rpgApi = detectRPGLeveling();

        // ── Register listeners ──
        mobKillListener = new MobKillQuestListener(questTracker);
        mobKillListener.register(rpgApi);

        blockQuestListener = new BlockQuestListener(questTracker);
        blockQuestListener.register(getEntityStoreRegistry());

        coinQuestListener = new CoinQuestListener(questTracker);
        coinQuestListener.register();

        // ── Generate initial quest pools ──
        int avgLevel = rpgApi != null ? 5 : 1; // стартовый уровень
        questTracker.refreshPools(avgLevel);

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
        LOGGER.info("  Mob kill tracking: {}", mobKillListener.isRegistered() ? "ACTIVE" : "DISABLED");
        LOGGER.info("  Block tracking:    ACTIVE");
        LOGGER.info("  Coin tracking:     {}", coinQuestListener.isRegistered() ? "ACTIVE" : "DISABLED");
        LOGGER.info("  Daily pool:        {} quests", storage.loadQuestPool(QuestPeriod.DAILY).size());
        LOGGER.info("  Weekly pool:       {} quests", storage.loadQuestPool(QuestPeriod.WEEKLY).size());
        LOGGER.info("═══════════════════════════════════════");
    }

    @Override
    protected void shutdown() {
        LOGGER.info("EcoTaleQuests shutting down...");

        // Cancel scheduled tasks
        if (autoSaveTask != null) autoSaveTask.cancel(false);
        if (poolRefreshTask != null) poolRefreshTask.cancel(false);
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
    //  RPG LEVELING DETECTION
    // ═════════════════════════════════════════════════════════════

    @Nullable
    private RPGLevelingAPI detectRPGLeveling() {
        try {
            Class<?> clazz = Class.forName("org.zuxaw.plugin.api.RPGLevelingAPI");
            // Try get() first (beta-5+ API), then getInstance() as fallback
            Object instance = null;
            for (String methodName : new String[]{"get", "getInstance", "getAPI"}) {
                try {
                    java.lang.reflect.Method m = clazz.getMethod(methodName);
                    instance = m.invoke(null);
                    if (instance != null) {
                        LOGGER.info("RPG Leveling API found via {}()", methodName);
                        break;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            if (instance instanceof RPGLevelingAPI api) {
                LOGGER.info("RPG Leveling API detected and connected.");
                return api;
            }
            if (instance != null) {
                LOGGER.warn("RPG Leveling API instance found but type mismatch: {}", instance.getClass().getName());
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("RPG Leveling not found — mob kill and XP quests will be limited.");
        } catch (Exception e) {
            LOGGER.warn("Failed to connect to RPG Leveling API: {}", e.getMessage());
        }
        return null;
    }

    // ═════════════════════════════════════════════════════════════
    //  GETTERS
    // ═════════════════════════════════════════════════════════════

    @Nonnull public ConfigManager getConfigManager() { return configManager; }
    @Nonnull public LangManager getLangManager() { return langManager; }
    @Nonnull public QuestStorage getStorage() { return storage; }
    @Nonnull public QuestTracker getQuestTracker() { return questTracker; }
    @Nonnull public QuestAbuseGuard getAbuseGuard() { return abuseGuard; }
    @Nonnull public QuestRewardCalculator getRewardCalculator() { return rewardCalculator; }
    @Nonnull public String getVersion() { return VERSION; }
}
