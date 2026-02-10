package com.crystalrealm.ecotalequests.gui;

import com.crystalrealm.ecotalequests.EcoTaleQuestsPlugin;
import com.crystalrealm.ecotalequests.config.ConfigManager;
import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.lang.LangManager;
import com.crystalrealm.ecotalequests.model.QuestPeriod;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Admin settings GUI for EcoTaleQuests.
 *
 * <p>Settings (S1–S12):</p>
 * <ul>
 *   <li>S1: Debug Mode (toggle)</li>
 *   <li>S2: Language (toggle en/ru)</li>
 *   <li>S3: Notify on Progress (toggle)</li>
 *   <li>S4: Notify on Complete (toggle)</li>
 *   <li>S5: Auto-Save Interval (increment)</li>
 *   <li>S6: Max Daily Active (increment)</li>
 *   <li>S7: Max Weekly Active (increment)</li>
 *   <li>S8: Daily Pool Size (increment)</li>
 *   <li>S9: Weekly Pool Size (increment)</li>
 *   <li>S10: Max Abandon / Day (increment)</li>
 *   <li>S11: Require Online (toggle)</li>
 *   <li>S12: Prevent Duplicate Types (toggle)</li>
 * </ul>
 *
 * <p>Actions (AB1–AB3): Reload Config, Refresh Pools, Save Data</p>
 *
 * @version 2.0.0
 */
public final class AdminQuestsGui extends InteractiveCustomUIPage<AdminQuestsGui.AdminEventData> {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private static final String PAGE_PATH = "Pages/CrystalRealm_EcoTaleQuests_AdminPanel.ui";

    // ── Event data keys ─────────────────────────────────────
    private static final String KEY_ACTION = "Action";
    private static final String KEY_SLOT   = "Slot";

    static final BuilderCodec<AdminEventData> CODEC = ReflectiveCodecBuilder
            .<AdminEventData>create(AdminEventData.class, AdminEventData::new)
            .addStringField(KEY_ACTION, (d, v) -> d.action = v, d -> d.action)
            .addStringField(KEY_SLOT,   (d, v) -> d.slot = v,   d -> d.slot)
            .build();

    // ── Instance fields ─────────────────────────────────────
    private final EcoTaleQuestsPlugin plugin;
    private final UUID playerUuid;
    @Nullable private final String errorMessage;
    @Nullable private final String successMessage;

    private Ref<EntityStore> savedRef;
    private Store<EntityStore> savedStore;

    public AdminQuestsGui(@Nonnull EcoTaleQuestsPlugin plugin,
                          @Nonnull PlayerRef playerRef,
                          @Nonnull UUID playerUuid) {
        this(plugin, playerRef, playerUuid, null, null);
    }

    public AdminQuestsGui(@Nonnull EcoTaleQuestsPlugin plugin,
                          @Nonnull PlayerRef playerRef,
                          @Nonnull UUID playerUuid,
                          @Nullable String errorMessage,
                          @Nullable String successMessage) {
        super(playerRef, CustomPageLifetime.CanDismiss, CODEC);
        this.plugin = plugin;
        this.playerUuid = playerUuid;
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
    }

    // ════════════════════════════════════════════════════════
    //  BUILD
    // ════════════════════════════════════════════════════════

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        this.savedRef = ref;
        this.savedStore = store;

        LangManager lang = plugin.getLangManager();

        cmd.append(PAGE_PATH);

        // Title
        cmd.set("#TitleLabel.Text", L(lang, "gui.admin.title"));

        // Section labels
        cmd.set("#SecGeneral.Text", L(lang, "gui.admin.sec_general"));
        cmd.set("#SecLimits.Text", L(lang, "gui.admin.sec_limits"));
        cmd.set("#SecProtection.Text", L(lang, "gui.admin.sec_protection"));
        cmd.set("#SecActions.Text", L(lang, "gui.admin.sec_actions"));
        cmd.set("#SecStats.Text", L(lang, "gui.admin.sec_stats"));

        // ── Bind events ─────────────────────────────────────

        // Toggle buttons (S1–S4, S11, S12)
        for (int s : new int[]{1, 2, 3, 4, 11, 12}) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#S" + s + "Toggle",
                    new EventData().append(KEY_ACTION, "toggle").append(KEY_SLOT, String.valueOf(s)));
        }

        // Increment buttons (S5–S10)
        for (int s = 5; s <= 10; s++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#S" + s + "Down",
                    new EventData().append(KEY_ACTION, "dec").append(KEY_SLOT, String.valueOf(s)));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#S" + s + "Up",
                    new EventData().append(KEY_ACTION, "inc").append(KEY_SLOT, String.valueOf(s)));
        }

        // Action buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AB1",
                new EventData().append(KEY_ACTION, "reload"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AB2",
                new EventData().append(KEY_ACTION, "refresh_pools"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AB3",
                new EventData().append(KEY_ACTION, "save"));

        // ── Banners ─────────────────────────────────────────
        if (errorMessage != null && !errorMessage.isEmpty()) {
            cmd.set("#ErrorBanner.Visible", true);
            cmd.set("#ErrorText.Text", stripForUI(errorMessage));
        }
        if (successMessage != null && !successMessage.isEmpty()) {
            cmd.set("#SuccessBanner.Visible", true);
            cmd.set("#SuccessText.Text", stripForUI(successMessage));
        }

        // ── Fill settings data ──────────────────────────────
        updateSettingsData(cmd, lang);

        LOGGER.info("Admin quest GUI built for {}", playerUuid);
    }

    // ════════════════════════════════════════════════════════
    //  HANDLE EVENTS
    // ════════════════════════════════════════════════════════

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull AdminEventData data) {
        LangManager lang = plugin.getLangManager();
        ConfigManager configMgr = plugin.getConfigManager();
        QuestsConfig config = configMgr.getConfig();

        switch (data.action) {
            case "toggle" -> {
                int slot = parseSlot(data.slot);
                handleToggle(slot, config, lang);
                saveConfig(configMgr);
                refreshPage(null, null);
            }

            case "inc" -> {
                int slot = parseSlot(data.slot);
                handleIncrement(slot, config, 1);
                saveConfig(configMgr);
                refreshPage(null, null);
            }

            case "dec" -> {
                int slot = parseSlot(data.slot);
                handleIncrement(slot, config, -1);
                saveConfig(configMgr);
                refreshPage(null, null);
            }

            case "reload" -> {
                boolean success = configMgr.reload();
                if (success) {
                    String newLang = configMgr.getConfig().getGeneral().getLanguage();
                    plugin.getLangManager().reload(newLang);
                    refreshPage(null, L(lang, "cmd.reload.success"));
                } else {
                    refreshPage(L(lang, "cmd.reload.fail"), null);
                }
            }

            case "refresh_pools" -> {
                try {
                    plugin.getQuestTracker().refreshPools(1);
                    refreshPage(null, L(lang, "gui.admin.pools_refreshed"));
                } catch (Exception e) {
                    refreshPage(L(lang, "gui.admin.pools_refresh_fail"), null);
                }
            }

            case "save" -> {
                try {
                    plugin.getStorage().save();
                    refreshPage(null, L(lang, "gui.admin.data_saved"));
                } catch (Exception e) {
                    refreshPage(L(lang, "gui.admin.data_save_fail"), null);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  TOGGLE / INCREMENT HANDLERS
    // ════════════════════════════════════════════════════════

    private void handleToggle(int slot, QuestsConfig config, LangManager lang) {
        QuestsConfig.GeneralSection gen = config.getGeneral();
        QuestsConfig.ProtectionSection prot = config.getProtection();

        switch (slot) {
            case 1  -> gen.setDebugMode(!gen.isDebugMode());
            case 2  -> gen.setLanguage("en".equalsIgnoreCase(gen.getLanguage()) ? "ru" : "en");
            case 3  -> gen.setNotifyOnProgress(!gen.isNotifyOnProgress());
            case 4  -> gen.setNotifyOnComplete(!gen.isNotifyOnComplete());
            case 11 -> prot.setRequireOnline(!prot.isRequireOnline());
            case 12 -> prot.setPreventDuplicateTypes(!prot.isPreventDuplicateTypes());
        }
    }

    private void handleIncrement(int slot, QuestsConfig config, int delta) {
        QuestsConfig.GeneralSection gen = config.getGeneral();
        QuestsConfig.QuestLimitsSection limits = config.getQuestLimits();

        switch (slot) {
            case 5  -> gen.setAutoSaveIntervalMinutes(
                    Math.max(1, gen.getAutoSaveIntervalMinutes() + delta));
            case 6  -> limits.setMaxDailyActive(
                    Math.max(1, limits.getMaxDailyActive() + delta));
            case 7  -> limits.setMaxWeeklyActive(
                    Math.max(1, limits.getMaxWeeklyActive() + delta));
            case 8  -> limits.setDailyPoolSize(
                    Math.max(1, limits.getDailyPoolSize() + delta));
            case 9  -> limits.setWeeklyPoolSize(
                    Math.max(1, limits.getWeeklyPoolSize() + delta));
            case 10 -> limits.setMaxAbandonPerDay(
                    Math.max(0, limits.getMaxAbandonPerDay() + delta));
        }
    }

    // ════════════════════════════════════════════════════════
    //  REFRESH PAGE
    // ════════════════════════════════════════════════════════

    private void refreshPage(@Nullable String error, @Nullable String success) {
        try {
            LangManager lang = plugin.getLangManager();
            UICommandBuilder cmd = new UICommandBuilder();

            cmd.set("#ErrorBanner.Visible", error != null && !error.isEmpty());
            if (error != null && !error.isEmpty()) cmd.set("#ErrorText.Text", stripForUI(error));
            cmd.set("#SuccessBanner.Visible", success != null && !success.isEmpty());
            if (success != null && !success.isEmpty()) cmd.set("#SuccessText.Text", stripForUI(success));

            updateSettingsData(cmd, lang);
            sendUpdate(cmd);
        } catch (Exception e) {
            LOGGER.warn("[refreshPage] sendUpdate failed: {}", e.getMessage());
            reopen(error, success);
        }
    }

    // ════════════════════════════════════════════════════════
    //  SETTINGS DATA
    // ════════════════════════════════════════════════════════

    private void updateSettingsData(UICommandBuilder cmd, LangManager lang) {
        QuestsConfig config = plugin.getConfigManager().getConfig();
        QuestsConfig.GeneralSection gen = config.getGeneral();
        QuestsConfig.QuestLimitsSection limits = config.getQuestLimits();
        QuestsConfig.ProtectionSection prot = config.getProtection();

        String toggleText = L(lang, "gui.admin.toggle");

        // S1: Debug Mode
        cmd.set("#S1Label.Text", L(lang, "gui.admin.debug_mode"));
        cmd.set("#S1Value.Text", boolText(gen.isDebugMode()));
        cmd.set("#S1Toggle.Text", toggleText);

        // S2: Language
        cmd.set("#S2Label.Text", L(lang, "gui.admin.language"));
        cmd.set("#S2Value.Text", gen.getLanguage().toUpperCase());
        cmd.set("#S2Toggle.Text", toggleText);

        // S3: Notify on Progress
        cmd.set("#S3Label.Text", L(lang, "gui.admin.notify_progress"));
        cmd.set("#S3Value.Text", boolText(gen.isNotifyOnProgress()));
        cmd.set("#S3Toggle.Text", toggleText);

        // S4: Notify on Complete
        cmd.set("#S4Label.Text", L(lang, "gui.admin.notify_complete"));
        cmd.set("#S4Value.Text", boolText(gen.isNotifyOnComplete()));
        cmd.set("#S4Toggle.Text", toggleText);

        // S5: Auto-Save Interval
        cmd.set("#S5Label.Text", L(lang, "gui.admin.autosave_interval"));
        cmd.set("#S5Value.Text", gen.getAutoSaveIntervalMinutes() + " min");

        // S6: Max Daily Active
        cmd.set("#S6Label.Text", L(lang, "gui.admin.max_daily_active"));
        cmd.set("#S6Value.Text", String.valueOf(limits.getMaxDailyActive()));

        // S7: Max Weekly Active
        cmd.set("#S7Label.Text", L(lang, "gui.admin.max_weekly_active"));
        cmd.set("#S7Value.Text", String.valueOf(limits.getMaxWeeklyActive()));

        // S8: Daily Pool Size
        cmd.set("#S8Label.Text", L(lang, "gui.admin.daily_pool_size"));
        cmd.set("#S8Value.Text", String.valueOf(limits.getDailyPoolSize()));

        // S9: Weekly Pool Size
        cmd.set("#S9Label.Text", L(lang, "gui.admin.weekly_pool_size"));
        cmd.set("#S9Value.Text", String.valueOf(limits.getWeeklyPoolSize()));

        // S10: Max Abandon / Day
        cmd.set("#S10Label.Text", L(lang, "gui.admin.max_abandon"));
        cmd.set("#S10Value.Text", String.valueOf(limits.getMaxAbandonPerDay()));

        // S11: Require Online
        cmd.set("#S11Label.Text", L(lang, "gui.admin.require_online"));
        cmd.set("#S11Value.Text", boolText(prot.isRequireOnline()));
        cmd.set("#S11Toggle.Text", toggleText);

        // S12: Prevent Duplicate Types
        cmd.set("#S12Label.Text", L(lang, "gui.admin.prevent_duplicates"));
        cmd.set("#S12Value.Text", boolText(prot.isPreventDuplicateTypes()));
        cmd.set("#S12Toggle.Text", toggleText);

        // Action button labels
        cmd.set("#AB1.Text", L(lang, "gui.admin.btn_reload"));
        cmd.set("#AB2.Text", L(lang, "gui.admin.btn_refresh"));
        cmd.set("#AB3.Text", L(lang, "gui.admin.btn_save"));

        // Stats
        int dailyPool = plugin.getStorage().loadQuestPool(QuestPeriod.DAILY).size();
        int weeklyPool = plugin.getStorage().loadQuestPool(QuestPeriod.WEEKLY).size();

        cmd.set("#StatDailyLabel.Text", L(lang, "gui.admin.stat_daily_pool"));
        cmd.set("#StatDailyValue.Text", String.valueOf(dailyPool));
        cmd.set("#StatWeeklyLabel.Text", L(lang, "gui.admin.stat_weekly_pool"));
        cmd.set("#StatWeeklyValue.Text", String.valueOf(weeklyPool));
        cmd.set("#StatVersionLabel.Text", L(lang, "gui.admin.stat_version"));
        cmd.set("#StatVersionValue.Text", "v" + plugin.getVersion());
    }

    // ════════════════════════════════════════════════════════
    //  RE-OPEN + STATIC OPEN
    // ════════════════════════════════════════════════════════

    private void reopen(@Nullable String error, @Nullable String success) {
        close();
        AdminQuestsGui newPage = new AdminQuestsGui(plugin, playerRef, playerUuid, error, success);
        PageOpenHelper.openPage(savedRef, savedStore, newPage);
    }

    public static void open(@Nonnull EcoTaleQuestsPlugin plugin,
                            @Nonnull PlayerRef playerRef,
                            @Nonnull Ref<EntityStore> ref,
                            @Nonnull Store<EntityStore> store,
                            @Nonnull UUID playerUuid) {
        AdminQuestsGui page = new AdminQuestsGui(plugin, playerRef, playerUuid);
        PageOpenHelper.openPage(ref, store, page);
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    private String L(LangManager lang, String key, String... args) {
        return lang.getForPlayer(playerUuid, key, args);
    }

    private static String stripForUI(String text) {
        if (text == null) return "";
        return text.replace("\u2714 ", "").replace("\u2714", "")
                   .replaceAll("<[^>]+>", "").trim();
    }

    private static String boolText(boolean val) {
        return val ? "ON" : "OFF";
    }

    private static int parseSlot(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private void saveConfig(ConfigManager configMgr) {
        try {
            // Write updated config back to file
            java.lang.reflect.Method saveMethod = configMgr.getClass().getMethod("saveConfig");
            saveMethod.invoke(configMgr);
        } catch (NoSuchMethodException e) {
            // If saveConfig() doesn't exist, try writing via reflection
            try {
                java.nio.file.Path configPath = configMgr.getConfigPath();
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                        .setPrettyPrinting().disableHtmlEscaping().create();
                try (java.io.Writer w = new java.io.OutputStreamWriter(
                        java.nio.file.Files.newOutputStream(configPath),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    gson.toJson(configMgr.getConfig(), w);
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to save config: {}", ex.getMessage());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to save config: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    //  EVENT DATA CLASS
    // ════════════════════════════════════════════════════════

    public static class AdminEventData {
        public String action = "";
        public String slot = "";
    }
}
