package com.crystalrealm.ecotalequests.gui;

import com.crystalrealm.ecotalequests.EcoTaleQuestsPlugin;
import com.crystalrealm.ecotalequests.config.ConfigManager;
import com.crystalrealm.ecotalequests.config.QuestsConfig;
import com.crystalrealm.ecotalequests.lang.LangManager;
import com.crystalrealm.ecotalequests.model.QuestPeriod;
import com.crystalrealm.ecotalequests.model.QuestRank;
import com.crystalrealm.ecotalequests.model.QuestType;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin settings GUI for EcoTaleQuests.
 *
 * <p>Settings (S1–S16):</p>
 * <ul>
 *   <li>S1: Debug Mode (toggle)</li>
 *   <li>S2: Language (cycle through supported languages)</li>
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
 *   <li>S15: Currency Symbol (text field + apply)</li>
 *   <li>S16: Round Currency (toggle)</li>
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
    private static final String KEY_ACTION   = "Action";
    private static final String KEY_SLOT     = "Slot";
    private static final String KEY_ED_TYPE  = "@EdType";
    private static final String KEY_ED_TARGET = "@EdTarget";
    private static final String KEY_SYMBOL   = "@Symbol";

    static final BuilderCodec<AdminEventData> CODEC = ReflectiveCodecBuilder
            .<AdminEventData>create(AdminEventData.class, AdminEventData::new)
            .addStringField(KEY_ACTION,    (d, v) -> d.action = v,    d -> d.action)
            .addStringField(KEY_SLOT,      (d, v) -> d.slot = v,      d -> d.slot)
            .addStringField(KEY_ED_TYPE,   (d, v) -> d.edType = v,    d -> d.edType)
            .addStringField(KEY_ED_TARGET, (d, v) -> d.edTarget = v,  d -> d.edTarget)
            .addStringField(KEY_SYMBOL,    (d, v) -> d.symbolValue = v, d -> d.symbolValue)
            .build();

    // ── Instance fields ─────────────────────────────────────
    private final EcoTaleQuestsPlugin plugin;
    private final UUID playerUuid;
    @Nullable private final String errorMessage;
    @Nullable private final String successMessage;

    private Ref<EntityStore> savedRef;
    private Store<EntityStore> savedStore;

    // ── Quest Editor state (persists across refreshes) ──────
    private static final QuestType[] EDITOR_TYPES = QuestType.values();
    private static final QuestRank[] EDITOR_RANKS = QuestRank.values();
    private static final String[] ACCESS_TYPES = {"individual", "global_unique", "limited_slots"};

    /** Custom target names set by admins via /quests settarget <name>. */
    private static final Map<UUID, String> customTargets = new ConcurrentHashMap<>();

    /** Sets a custom target for the quest editor. Called from /quests settarget command. */
    public static void setCustomTarget(@Nonnull UUID playerUuid, @Nonnull String target) {
        if (target.isEmpty()) {
            customTargets.remove(playerUuid);
        } else {
            customTargets.put(playerUuid, target);
        }
    }

    /** Clears the custom target for a player. */
    public static void clearCustomTarget(@Nonnull UUID playerUuid) {
        customTargets.remove(playerUuid);
    }

    private int edTypeIdx = 0;
    private int edTargetIdx = 0;
    private String edTypeName = "";    // free-text type (overrides edTypeIdx when non-empty)
    private String edTargetName = "";  // free-text target (overrides edTargetIdx when non-empty)
    private int edAmount = 10;
    private boolean edWeekly = false; // false = daily
    private int edRankIdx = 0;
    private int edCoins = 100;
    private int edXp = 50;
    private int edRankPoints = 10;
    private int edDuration = 60;
    private int edAccessIdx = 0;
    private int edMaxSlots = 0;
    private int edMinLevel = 0;

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
        cmd.set("#SecBoards.Text", L(lang, "gui.admin.sec_boards"));
        cmd.set("#SecActions.Text", L(lang, "gui.admin.sec_actions"));
        cmd.set("#SecStats.Text", L(lang, "gui.admin.sec_stats"));

        // ── Bind events ─────────────────────────────────────

        // Toggle buttons (S1–S4, S11, S12, S13, S14, S16)
        for (int s : new int[]{1, 2, 3, 4, 11, 12, 13, 14, 16}) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#S" + s + "Toggle",
                    new EventData().append(KEY_ACTION, "toggle").append(KEY_SLOT, String.valueOf(s)));
        }

        // S15: Currency Symbol apply button (reads TextField value)
        events.addEventBinding(CustomUIEventBindingType.Activating, "#S15Apply",
                new EventData().append(KEY_ACTION, "apply_symbol")
                        .append(KEY_SYMBOL, "#S15Input.Value"),
                false);

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

        // Quest Type toggles (QT1–QT7)
        for (int qt = 1; qt <= 7; qt++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#QT" + qt + "Toggle",
                    new EventData().append(KEY_ACTION, "qt_toggle").append(KEY_SLOT, String.valueOf(qt)));
        }

        // ── Editor events ───────────────────────────────────
        // E1 (type), E2 (target), E4 (period), E5 (rank), E10 (access) — cycle toggles
        for (int e : new int[]{1, 2, 4, 5, 10}) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#E" + e + "Toggle",
                    new EventData().append(KEY_ACTION, "ed_toggle").append(KEY_SLOT, String.valueOf(e)));
        }
        // E3 (amount), E6 (coins), E7 (xp), E8 (rp), E9 (duration), E11 (slots), E12 (level) — 4 buttons: --/-/+/++
        for (int e : new int[]{3, 6, 7, 8, 9, 11, 12}) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#E" + e + "DownBig",
                    new EventData().append(KEY_ACTION, "ed_dec_big").append(KEY_SLOT, String.valueOf(e)));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#E" + e + "Down",
                    new EventData().append(KEY_ACTION, "ed_dec").append(KEY_SLOT, String.valueOf(e)));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#E" + e + "Up",
                    new EventData().append(KEY_ACTION, "ed_inc").append(KEY_SLOT, String.valueOf(e)));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#E" + e + "UpBig",
                    new EventData().append(KEY_ACTION, "ed_inc_big").append(KEY_SLOT, String.valueOf(e)));
        }
        // Create / Clear / Delete buttons
        // Create button passes TextField values via @-prefixed references
        events.addEventBinding(CustomUIEventBindingType.Activating, "#EBtnCreate",
                new EventData().append(KEY_ACTION, "ed_create")
                        .append(KEY_ED_TYPE, "#E1Input.Value")
                        .append(KEY_ED_TARGET, "#E2Input.Value"),
                false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#EBtnClear",
                new EventData().append(KEY_ACTION, "ed_clear"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#EBtnDelete",
                new EventData().append(KEY_ACTION, "ed_delete"));

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
        updateQuestTypesData(cmd, lang);
        updateEditorData(cmd, lang);

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

            case "apply_symbol" -> {
                String sym = data.symbolValue != null ? data.symbolValue.trim() : "";
                if (!sym.isEmpty()) {
                    config.getGeneral().setCurrencySymbol(sym);
                    saveConfig(configMgr);
                    refreshPage(null, L(lang, "gui.admin.symbol_applied"));
                } else {
                    refreshPage(L(lang, "gui.admin.symbol_empty"), null);
                }
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

            // ── Quest Type toggle events ──────────────────
            case "qt_toggle" -> {
                int slot = parseSlot(data.slot);
                QuestType[] allTypes = QuestType.values();
                if (slot >= 1 && slot <= allTypes.length) {
                    config.getGeneration().toggleQuestType(allTypes[slot - 1].getId());
                    saveConfig(configMgr);
                }
                refreshPage(null, null);
            }

            // ── Quest Editor events ─────────────────────────
            case "ed_toggle" -> {
                int slot = parseSlot(data.slot);
                handleEditorToggle(slot);
                refreshPage(null, null);
            }
            case "ed_inc" -> {
                int slot = parseSlot(data.slot);
                handleEditorIncrement(slot, 1, false);
                refreshPage(null, null);
            }
            case "ed_dec" -> {
                int slot = parseSlot(data.slot);
                handleEditorIncrement(slot, -1, false);
                refreshPage(null, null);
            }
            case "ed_inc_big" -> {
                int slot = parseSlot(data.slot);
                handleEditorIncrement(slot, 1, true);
                refreshPage(null, null);
            }
            case "ed_dec_big" -> {
                int slot = parseSlot(data.slot);
                handleEditorIncrement(slot, -1, true);
                refreshPage(null, null);
            }
            case "ed_create" -> {
                // Read type and target from text fields
                String typedType = data.edType != null ? data.edType.trim() : "";
                String typedTarget = data.edTarget != null ? data.edTarget.trim() : "";
                // Update editor state from text fields so cycle buttons stay in sync
                if (!typedType.isEmpty()) {
                    edTypeName = typedType;
                }
                if (!typedTarget.isEmpty()) {
                    edTargetName = typedTarget;
                }
                String result = createCustomQuest(config);
                if (result == null) {
                    saveConfig(configMgr);
                    // Auto-refresh pools so the new quest appears immediately
                    try {
                        plugin.getQuestTracker().refreshPools(1);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to auto-refresh pools after quest creation: {}", e.getMessage());
                    }
                    refreshPage(null, L(lang, "gui.admin.quest_created"));
                } else {
                    refreshPage(result, null);
                }
            }
            case "ed_clear" -> {
                resetEditorState();
                refreshPage(null, null);
            }
            case "ed_delete" -> {
                String delResult = deleteLastCustomQuest(config);
                if (delResult == null) {
                    saveConfig(configMgr);
                    try {
                        plugin.getQuestTracker().refreshPools(1);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to auto-refresh pools after quest deletion: {}", e.getMessage());
                    }
                    refreshPage(null, L(lang, "gui.admin.quest_deleted"));
                } else {
                    refreshPage(delResult, null);
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
        QuestsConfig.BoardsSection boards = config.getBoards();

        switch (slot) {
            case 1  -> gen.setDebugMode(!gen.isDebugMode());
            case 2  -> {
                java.util.List<String> langs = LangManager.SUPPORTED_LANGS;
                int idx = langs.indexOf(gen.getLanguage().toLowerCase());
                gen.setLanguage(langs.get((idx + 1) % langs.size()));
            }
            case 3  -> gen.setNotifyOnProgress(!gen.isNotifyOnProgress());
            case 4  -> gen.setNotifyOnComplete(!gen.isNotifyOnComplete());
            case 11 -> prot.setRequireOnline(!prot.isRequireOnline());
            case 12 -> prot.setPreventDuplicateTypes(!prot.isPreventDuplicateTypes());
            case 13 -> {
                // Cycle: both → board_only → gui_only → both
                String current = boards.getQuestAccessMode();
                String next = switch (current.toLowerCase()) {
                    case "both"       -> "board_only";
                    case "board_only" -> "gui_only";
                    default           -> "both";
                };
                boards.setQuestAccessMode(next);
            }
            case 14 -> boards.setEnabled(!boards.isEnabled());
            case 16 -> gen.setRoundCurrency(!gen.isRoundCurrency());
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
    //  QUEST EDITOR HANDLERS
    // ════════════════════════════════════════════════════════

    private void handleEditorToggle(int slot) {
        switch (slot) {
            case 1 -> {
                edTypeIdx = (edTypeIdx + 1) % EDITOR_TYPES.length;
                edTypeName = EDITOR_TYPES[edTypeIdx].getId();
                edTargetIdx = 0; // reset target when type changes
                java.util.List<String> targets = getTargetsForType(EDITOR_TYPES[edTypeIdx]);
                edTargetName = targets.isEmpty() ? EDITOR_TYPES[edTypeIdx].getCategory() : targets.get(0);
            }
            case 2 -> {
                QuestType type = resolveEditorType();
                java.util.List<String> targets = getTargetsForType(type);
                if (!targets.isEmpty()) {
                    edTargetIdx = (edTargetIdx + 1) % targets.size();
                    edTargetName = targets.get(edTargetIdx);
                }
            }
            case 4 -> edWeekly = !edWeekly;
            case 5 -> edRankIdx = (edRankIdx + 1) % EDITOR_RANKS.length;
            case 10 -> edAccessIdx = (edAccessIdx + 1) % ACCESS_TYPES.length;
        }
    }

    // Step sizes: [small, big] for each editor field
    private static final int[][] EDITOR_STEPS = {
        /* E3  Amount */     {1, 10},
        /* E6  Coins  */     {10, 100},
        /* E7  XP     */     {5, 50},
        /* E8  RP     */     {1, 10},
        /* E9  Duration */   {5, 30},
        /* E11 Slots  */     {1, 5},
        /* E12 Level  */     {1, 5}
    };

    private int getStepSmall(int slot) {
        return switch (slot) {
            case 3  -> EDITOR_STEPS[0][0];
            case 6  -> EDITOR_STEPS[1][0];
            case 7  -> EDITOR_STEPS[2][0];
            case 8  -> EDITOR_STEPS[3][0];
            case 9  -> EDITOR_STEPS[4][0];
            case 11 -> EDITOR_STEPS[5][0];
            case 12 -> EDITOR_STEPS[6][0];
            default -> 1;
        };
    }

    private int getStepBig(int slot) {
        return switch (slot) {
            case 3  -> EDITOR_STEPS[0][1];
            case 6  -> EDITOR_STEPS[1][1];
            case 7  -> EDITOR_STEPS[2][1];
            case 8  -> EDITOR_STEPS[3][1];
            case 9  -> EDITOR_STEPS[4][1];
            case 11 -> EDITOR_STEPS[5][1];
            case 12 -> EDITOR_STEPS[6][1];
            default -> 1;
        };
    }

    private void handleEditorIncrement(int slot, int dir, boolean big) {
        int step = big ? getStepBig(slot) : getStepSmall(slot);
        switch (slot) {
            case 3  -> edAmount = Math.max(1, edAmount + dir * step);
            case 6  -> edCoins = Math.max(0, edCoins + dir * step);
            case 7  -> edXp = Math.max(0, edXp + dir * step);
            case 8  -> edRankPoints = Math.max(0, edRankPoints + dir * step);
            case 9  -> edDuration = Math.max(5, edDuration + dir * step);
            case 11 -> edMaxSlots = Math.max(0, edMaxSlots + dir * step);
            case 12 -> edMinLevel = Math.max(0, edMinLevel + dir * step);
        }
    }

    private void resetEditorState() {
        edTypeIdx = 0;
        edTargetIdx = 0;
        edTypeName = EDITOR_TYPES[0].getId();
        edTargetName = "";
        edAmount = 10;
        edWeekly = false;
        edRankIdx = 0;
        edCoins = 100;
        edXp = 50;
        edRankPoints = 10;
        edDuration = 60;
        edAccessIdx = 0;
        edMaxSlots = 0;
        edMinLevel = 0;
    }

    /**
     * Resolves the current quest type from free-text name or fallback to cycle index.
     */
    @Nonnull
    private QuestType resolveEditorType() {
        if (edTypeName != null && !edTypeName.isEmpty()) {
            for (QuestType qt : EDITOR_TYPES) {
                if (qt.getId().equalsIgnoreCase(edTypeName)) return qt;
            }
        }
        return EDITOR_TYPES[edTypeIdx];
    }

    /**
     * Creates a custom quest entry from the current editor state and appends it to config.
     * Uses edTypeName and edTargetName for free-text input compatibility.
     * @return null on success, or an error message.
     */
    @Nullable
    private String createCustomQuest(QuestsConfig config) {
        // Use free-text names if available, otherwise fall back to cycle index
        String typeId = (edTypeName != null && !edTypeName.isEmpty())
                ? edTypeName : EDITOR_TYPES[edTypeIdx].getId();
        String target = (edTargetName != null && !edTargetName.isEmpty())
                ? edTargetName : getTargetsForType(resolveEditorType()).isEmpty()
                    ? typeId : getTargetsForType(resolveEditorType()).get(edTargetIdx);
        QuestRank rank = EDITOR_RANKS[edRankIdx];

        // Generate unique ID
        String id = typeId + "_" + target + "_" + System.currentTimeMillis();

        QuestsConfig.CustomQuestEntry entry = new QuestsConfig.CustomQuestEntry();
        entry.setId(id);
        entry.setType(typeId);
        entry.setTarget(target);
        entry.setAmount(edAmount);
        entry.setPeriod(edWeekly ? "weekly" : "daily");
        entry.setRank(rank.getId());
        entry.setCoins(edCoins);
        entry.setXp(edXp);
        entry.setRankPoints(edRankPoints);
        entry.setDurationMinutes(edDuration);
        entry.setAccessType(ACCESS_TYPES[edAccessIdx]);
        entry.setMaxSlots(edMaxSlots);
        entry.setMinLevel(edMinLevel);

        // Add to config list
        if (config.getCustomQuests() == null) {
            // Initialize list if null (first custom quest)
            try {
                java.lang.reflect.Field f = config.getClass().getDeclaredField("CustomQuests");
                f.setAccessible(true);
                f.set(config, new java.util.ArrayList<QuestsConfig.CustomQuestEntry>());
            } catch (Exception e) {
                return "Failed to initialize custom quests list";
            }
        }
        config.getCustomQuests().add(entry);

        LOGGER.info("Custom quest created: {} (type={}, target={}, rank={})",
                id, typeId, target, rank.getId());
        return null;
    }

    /**
     * Deletes the last custom quest entry from the config.
     * @return null on success, or error message.
     */
    @Nullable
    private String deleteLastCustomQuest(QuestsConfig config) {
        java.util.List<QuestsConfig.CustomQuestEntry> list = config.getCustomQuests();
        if (list == null || list.isEmpty()) {
            return L(plugin.getLangManager(), "gui.admin.no_custom_quests");
        }
        QuestsConfig.CustomQuestEntry removed = list.remove(list.size() - 1);
        LOGGER.info("Deleted custom quest: {}", removed.getId());
        return null;
    }

    /**
     * Returns available target keys for a given quest type from the generation config.
     */
    @Nonnull
    private java.util.List<String> getTargetsForType(QuestType type) {
        QuestsConfig.GenerationSection gen = plugin.getConfigManager().getConfig().getGeneration();
        java.util.Map<String, ?> map = switch (type) {
            case KILL_MOB     -> gen.getKillMobs();
            case MINE_ORE     -> gen.getMineOres();
            case CHOP_WOOD    -> gen.getChopWood();
            case HARVEST_CROP -> gen.getHarvestCrops();
            case KILL_BOSS    -> gen.getKillBosses();
            default           -> java.util.Collections.emptyMap();
        };
        java.util.List<String> result = new java.util.ArrayList<>(map.keySet());
        // Append custom target if set by admin
        String custom = customTargets.get(playerUuid);
        if (custom != null && !custom.isEmpty() && !result.contains(custom)) {
            result.add(custom);
        }
        return result;
    }

    // ════════════════════════════════════════════════════════
    //  QUEST TYPES DATA (enable/disable from pool)
    // ════════════════════════════════════════════════════════

    private void updateQuestTypesData(UICommandBuilder cmd, LangManager lang) {
        QuestsConfig config = plugin.getConfigManager().getConfig();
        QuestsConfig.GenerationSection gen = config.getGeneration();
        String toggleText = L(lang, "gui.admin.toggle");

        cmd.set("#SecQuestTypes.Text", L(lang, "gui.admin.sec_quest_types"));

        QuestType[] allTypes = QuestType.values();
        for (int i = 0; i < allTypes.length && i < 7; i++) {
            int n = i + 1;
            QuestType qt = allTypes[i];
            boolean disabled = gen.isQuestTypeDisabled(qt.getId());
            cmd.set("#QT" + n + "Label.Text", L(lang, "gui.admin.qt_" + qt.getId()));
            cmd.set("#QT" + n + "Value.Text", disabled ? "OFF" : "ON");
            cmd.set("#QT" + n + "Toggle.Text", toggleText);
        }
    }

    // ════════════════════════════════════════════════════════
    //  EDITOR DATA UPDATE
    // ════════════════════════════════════════════════════════

    private void updateEditorData(UICommandBuilder cmd, LangManager lang) {
        QuestsConfig config = plugin.getConfigManager().getConfig();

        cmd.set("#SecEditor.Text", L(lang, "gui.admin.sec_editor"));
        cmd.set("#EditorHint.Text", L(lang, "gui.admin.editor_hint"));

        QuestType currentType = resolveEditorType();
        java.util.List<String> targets = getTargetsForType(currentType);
        String currentTarget = (edTargetName != null && !edTargetName.isEmpty())
                ? edTargetName
                : (targets.isEmpty() ? currentType.getCategory() : targets.get(edTargetIdx));
        QuestRank currentRank = EDITOR_RANKS[edRankIdx];

        // E1: Type (TextField)
        cmd.set("#E1Label.Text", L(lang, "gui.admin.ed_type"));
        cmd.set("#E1Input.Value", edTypeName != null ? edTypeName : currentType.getId());
        cmd.set("#E1Toggle.Text", L(lang, "gui.admin.ed_next_type"));

        // E2: Target (TextField)
        cmd.set("#E2Label.Text", L(lang, "gui.admin.ed_target"));
        cmd.set("#E2Input.Value", currentTarget);
        cmd.set("#E2Toggle.Text", L(lang, "gui.admin.ed_next_target"));

        // E3: Amount
        cmd.set("#E3Label.Text", L(lang, "gui.admin.ed_amount"));
        cmd.set("#E3Value.Text", String.valueOf(edAmount));
        setNumericButtons(cmd, 3);

        // E4: Period
        cmd.set("#E4Label.Text", L(lang, "gui.admin.ed_period"));
        cmd.set("#E4Value.Text", edWeekly ? "weekly" : "daily");
        cmd.set("#E4Toggle.Text", L(lang, "gui.admin.toggle"));

        // E5: Rank
        cmd.set("#E5Label.Text", L(lang, "gui.admin.ed_rank"));
        cmd.set("#E5Value.Text", currentRank.name());
        cmd.set("#E5Toggle.Text", L(lang, "gui.admin.ed_next_rank"));

        // E6: Coins
        cmd.set("#E6Label.Text", L(lang, "gui.admin.ed_coins"));
        cmd.set("#E6Value.Text", String.valueOf(edCoins));
        setNumericButtons(cmd, 6);

        // E7: XP
        cmd.set("#E7Label.Text", L(lang, "gui.admin.ed_xp"));
        cmd.set("#E7Value.Text", String.valueOf(edXp));
        setNumericButtons(cmd, 7);

        // E8: Rank Points
        cmd.set("#E8Label.Text", L(lang, "gui.admin.ed_rank_points"));
        cmd.set("#E8Value.Text", String.valueOf(edRankPoints));
        setNumericButtons(cmd, 8);

        // E9: Duration
        cmd.set("#E9Label.Text", L(lang, "gui.admin.ed_duration"));
        cmd.set("#E9Value.Text", edDuration + " min");
        setNumericButtons(cmd, 9);

        // E10: Access Type
        cmd.set("#E10Label.Text", L(lang, "gui.admin.ed_access"));
        cmd.set("#E10Value.Text", ACCESS_TYPES[edAccessIdx]);
        cmd.set("#E10Toggle.Text", L(lang, "gui.admin.ed_next"));

        // E11: Max Slots
        cmd.set("#E11Label.Text", L(lang, "gui.admin.ed_max_slots"));
        cmd.set("#E11Value.Text", String.valueOf(edMaxSlots));
        setNumericButtons(cmd, 11);

        // E12: Min Level
        cmd.set("#E12Label.Text", L(lang, "gui.admin.ed_min_level"));
        cmd.set("#E12Value.Text", String.valueOf(edMinLevel));
        setNumericButtons(cmd, 12);

        // Buttons
        cmd.set("#EBtnCreate.Text", L(lang, "gui.admin.btn_create"));
        cmd.set("#EBtnClear.Text", L(lang, "gui.admin.btn_clear"));
        cmd.set("#EBtnDelete.Text", L(lang, "gui.admin.btn_delete"));

        // Custom quests count
        int customCount = config.getCustomQuests() != null ? config.getCustomQuests().size() : 0;
        cmd.set("#StatCustomLabel.Text", L(lang, "gui.admin.stat_custom"));
        cmd.set("#StatCustomValue.Text", String.valueOf(customCount));
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
            updateQuestTypesData(cmd, lang);
            updateEditorData(cmd, lang);
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
        QuestsConfig.BoardsSection boards = config.getBoards();

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

        // S15: Currency Symbol
        cmd.set("#S15Label.Text", L(lang, "gui.admin.currency_symbol"));
        cmd.set("#S15Input.Value", gen.getCurrencySymbol());
        cmd.set("#S15Apply.Text", L(lang, "gui.admin.apply"));

        // S16: Round Currency
        cmd.set("#S16Label.Text", L(lang, "gui.admin.round_currency"));
        cmd.set("#S16Value.Text", boolText(gen.isRoundCurrency()));
        cmd.set("#S16Toggle.Text", toggleText);

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

        // ── Boards section ──────────────────────────────────
        cmd.set("#SecBoards.Text", L(lang, "gui.admin.sec_boards"));

        // S13: Quest Access Mode
        cmd.set("#S13Label.Text", L(lang, "gui.admin.access_mode"));
        String modeDisplay = switch (boards.getQuestAccessMode().toLowerCase()) {
            case "board_only" -> L(lang, "gui.admin.mode_board");
            case "gui_only"   -> L(lang, "gui.admin.mode_gui");
            default           -> L(lang, "gui.admin.mode_both");
        };
        cmd.set("#S13Value.Text", modeDisplay);
        cmd.set("#S13Toggle.Text", toggleText);

        // S14: Boards Enabled
        cmd.set("#S14Label.Text", L(lang, "gui.admin.boards_enabled"));
        cmd.set("#S14Value.Text", boolText(boards.isEnabled()));
        cmd.set("#S14Toggle.Text", toggleText);

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

    /**
     * Sets the text labels for 4 numeric buttons: -big, -small, +small, +big.
     */
    private void setNumericButtons(UICommandBuilder cmd, int slot) {
        int small = getStepSmall(slot);
        int big = getStepBig(slot);
        cmd.set("#E" + slot + "DownBig.Text",  "-" + big);
        cmd.set("#E" + slot + "Down.Text",     "-" + small);
        cmd.set("#E" + slot + "Up.Text",       "+" + small);
        cmd.set("#E" + slot + "UpBig.Text",    "+" + big);
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
        public String edType = "";
        public String edTarget = "";
        public String symbolValue = "";
    }
}
