package com.crystalrealm.ecotalequests.gui;

import com.crystalrealm.ecotalequests.EcoTaleQuestsPlugin;
import com.crystalrealm.ecotalequests.lang.LangManager;
import com.crystalrealm.ecotalequests.model.*;
import com.crystalrealm.ecotalequests.service.QuestAvailabilityManager;
import com.crystalrealm.ecotalequests.service.TimerService;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.MiniMessageParser;
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
import java.util.*;

/**
 * Native interactive quest GUI — no HyUI dependency.
 *
 * <p>Tabs:</p>
 * <ul>
 *   <li><b>Daily</b>  — available daily quests (up to 10 slots, D1–D10)</li>
 *   <li><b>Weekly</b> — available weekly quests (up to 5 slots, W1–W5)</li>
 *   <li><b>Active</b> — player's active quests with progress (up to 6 slots, A1–A6)</li>
 * </ul>
 *
 * <p>Slot-based event binding: events are bound once in {@link #build}, resolved
 * at runtime in {@link #handleDataEvent}. Uses {@link #sendUpdate(UICommandBuilder)}
 * to refresh data without Loading screens.</p>
 *
 * @version 2.0.0
 */
public final class PlayerQuestsGui extends InteractiveCustomUIPage<PlayerQuestsGui.QuestEventData> {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private static final String PAGE_PATH = "Pages/CrystalRealm_EcoTaleQuests_QuestPanel.ui";
    private static final int MAX_DAILY  = 10;
    private static final int MAX_WEEKLY = 5;
    private static final int MAX_ACTIVE = 6;

    // ── Event data keys ─────────────────────────────────────
    private static final String KEY_ACTION = "Action";
    private static final String KEY_SLOT   = "Slot";
    private static final String KEY_TAB    = "Tab";

    static final BuilderCodec<QuestEventData> CODEC = ReflectiveCodecBuilder
            .<QuestEventData>create(QuestEventData.class, QuestEventData::new)
            .addStringField(KEY_ACTION, (d, v) -> d.action = v, d -> d.action)
            .addStringField(KEY_SLOT,   (d, v) -> d.slot = v,   d -> d.slot)
            .addStringField(KEY_TAB,    (d, v) -> d.tab = v,    d -> d.tab)
            .build();

    // ── Instance fields ─────────────────────────────────────
    private final EcoTaleQuestsPlugin plugin;
    private final UUID playerUuid;
    private final String selectedTab;
    @Nullable private final String errorMessage;
    @Nullable private final String successMessage;

    // Saved for re-open
    private Ref<EntityStore> savedRef;
    private Store<EntityStore> savedStore;

    public PlayerQuestsGui(@Nonnull EcoTaleQuestsPlugin plugin,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull UUID playerUuid) {
        this(plugin, playerRef, playerUuid, null, null, "daily");
    }

    public PlayerQuestsGui(@Nonnull EcoTaleQuestsPlugin plugin,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull UUID playerUuid,
                           @Nullable String errorMessage,
                           @Nullable String successMessage,
                           @Nonnull String selectedTab) {
        super(playerRef, CustomPageLifetime.CanDismiss, CODEC);
        this.plugin = plugin;
        this.playerUuid = playerUuid;
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
        this.selectedTab = selectedTab;
    }

    // ════════════════════════════════════════════════════════
    //  BUILD — initial page construction + event binding
    // ════════════════════════════════════════════════════════

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        this.savedRef = ref;
        this.savedStore = store;

        LangManager lang = plugin.getLangManager();

        // Load root template
        cmd.append(PAGE_PATH);

        // Title
        cmd.set("#TitleLabel.Text", L(lang, "gui.title"));

        // Rank display
        QuestRank playerRank = plugin.getQuestTracker().getRankService().getPlayerRank(playerUuid);
        PlayerRankData rankData = plugin.getQuestTracker().getRankService().getRankData(playerUuid);
        cmd.set("#RankLabel.Text", L(lang, "gui.rank_display",
                "rank", playerRank.name(),
                "points", String.valueOf(rankData.getRankPoints())));
        cmd.set("#RankLabel.Visible", true);

        // Tab labels
        cmd.set("#TabDaily.Text", L(lang, "quest.period.daily"));
        cmd.set("#TabWeekly.Text", L(lang, "quest.period.weekly"));
        cmd.set("#TabActive.Text", L(lang, "gui.tab.active"));

        // Tab visibility
        cmd.set("#DailyContent.Visible", "daily".equals(selectedTab));
        cmd.set("#WeeklyContent.Visible", "weekly".equals(selectedTab));
        cmd.set("#ActiveContent.Visible", "active".equals(selectedTab));

        // ── Bind ALL events (once, slot-based) ──────────────

        // Tab switching
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabDaily",
                new EventData().append(KEY_ACTION, "tab").append(KEY_TAB, "daily"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabWeekly",
                new EventData().append(KEY_ACTION, "tab").append(KEY_TAB, "weekly"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabActive",
                new EventData().append(KEY_ACTION, "tab").append(KEY_TAB, "active"));

        // Daily accept buttons (D1–D10)
        for (int i = 1; i <= MAX_DAILY; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#D" + i + "Btn",
                    new EventData().append(KEY_ACTION, "accept_daily")
                            .append(KEY_SLOT, String.valueOf(i)));
        }

        // Weekly accept buttons (W1–W5)
        for (int i = 1; i <= MAX_WEEKLY; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#W" + i + "Btn",
                    new EventData().append(KEY_ACTION, "accept_weekly")
                            .append(KEY_SLOT, String.valueOf(i)));
        }

        // Active abandon buttons (A1–A6)
        for (int i = 1; i <= MAX_ACTIVE; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#A" + i + "Btn",
                    new EventData().append(KEY_ACTION, "abandon")
                            .append(KEY_SLOT, String.valueOf(i)));
        }

        // ── Banners ─────────────────────────────────────────
        if (errorMessage != null && !errorMessage.isEmpty()) {
            cmd.set("#ErrorBanner.Visible", true);
            cmd.set("#ErrorText.Text", stripForUI(errorMessage));
        }
        if (successMessage != null && !successMessage.isEmpty()) {
            cmd.set("#SuccessBanner.Visible", true);
            cmd.set("#SuccessText.Text", stripForUI(successMessage));
        }

        // ── Build all tab data ──────────────────────────────
        updateDailyData(cmd, lang);
        updateWeeklyData(cmd, lang);
        updateActiveData(cmd, lang);

        LOGGER.info("Quest GUI built for {} (tab={})", playerUuid, selectedTab);
    }

    // ════════════════════════════════════════════════════════
    //  HANDLE EVENTS
    // ════════════════════════════════════════════════════════

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull QuestEventData data) {
        LangManager lang = plugin.getLangManager();
        QuestTracker tracker = plugin.getQuestTracker();

        switch (data.action) {
            case "tab" -> {
                try {
                    UICommandBuilder tabCmd = new UICommandBuilder();
                    tabCmd.set("#DailyContent.Visible", "daily".equals(data.tab));
                    tabCmd.set("#WeeklyContent.Visible", "weekly".equals(data.tab));
                    tabCmd.set("#ActiveContent.Visible", "active".equals(data.tab));
                    tabCmd.set("#ErrorBanner.Visible", false);
                    tabCmd.set("#SuccessBanner.Visible", false);
                    // Refresh data for the selected tab
                    updateDailyData(tabCmd, lang);
                    updateWeeklyData(tabCmd, lang);
                    updateActiveData(tabCmd, lang);
                    sendUpdate(tabCmd);
                } catch (Exception e) {
                    LOGGER.warn("[tab] sendUpdate failed: {}", e.getMessage());
                    reopen(null, null, data.tab);
                }
            }

            case "accept_daily" -> {
                int slot = parseSlot(data.slot);
                List<Quest> quests = tracker.getAvailableQuests(playerUuid, QuestPeriod.DAILY);
                if (slot < 1 || slot > quests.size()) return;
                Quest quest = quests.get(slot - 1);
                doAccept(tracker, lang, quest);
            }

            case "accept_weekly" -> {
                int slot = parseSlot(data.slot);
                List<Quest> quests = tracker.getAvailableQuests(playerUuid, QuestPeriod.WEEKLY);
                if (slot < 1 || slot > quests.size()) return;
                Quest quest = quests.get(slot - 1);
                doAccept(tracker, lang, quest);
            }

            case "abandon" -> {
                int slot = parseSlot(data.slot);
                List<PlayerQuestData> active = tracker.getActiveQuests(playerUuid);
                if (slot < 1 || slot > active.size()) return;
                PlayerQuestData pqd = active.get(slot - 1);

                QuestTracker.AbandonResult result = tracker.abandonQuest(playerUuid, pqd.getQuestId());
                LOGGER.info("GUI abandon slot {} -> {}", slot, result);

                String msg = switch (result) {
                    case SUCCESS      -> L(lang, "cmd.abandon.success");
                    case NOT_FOUND    -> L(lang, "cmd.abandon.not_found",
                            "id", pqd.getQuestId().toString().substring(0, 8));
                    case NOT_ACTIVE   -> L(lang, "cmd.abandon.not_active");
                    case LIMIT_REACHED -> L(lang, "cmd.abandon.limit");
                };

                refreshPage(result == QuestTracker.AbandonResult.SUCCESS ? null : msg,
                        result == QuestTracker.AbandonResult.SUCCESS ? msg : null,
                        "active");
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  ACCEPT QUEST LOGIC
    // ════════════════════════════════════════════════════════

    private void doAccept(QuestTracker tracker, LangManager lang, Quest quest) {
        UUID questId = quest.getQuestId();

        if (!plugin.getAbuseGuard().canAcceptQuest(playerUuid)) {
            refreshPage(L(lang, "cmd.accept.cooldown"), null, "daily");
            return;
        }

        QuestTracker.AcceptResult result = tracker.acceptQuest(playerUuid, questId);
        LOGGER.info("GUI accept {} -> {}", questId, result);

        String msg = switch (result) {
            case SUCCESS -> {
                plugin.getAbuseGuard().recordAccept(playerUuid);
                String name = localizedDesc(lang, quest);
                yield L(lang, "cmd.accept.success", "name", name);
            }
            case ALREADY_ACTIVE  -> L(lang, "cmd.accept.already_active");
            case LIMIT_REACHED   -> L(lang, "cmd.accept.limit");
            case DUPLICATE_TYPE  -> L(lang, "cmd.accept.duplicate");
            case QUEST_EXPIRED   -> L(lang, "cmd.accept.expired");
            case QUEST_NOT_FOUND -> L(lang, "cmd.accept.not_found",
                    "id", questId.toString().substring(0, 8));
            case RANK_TOO_LOW    -> L(lang, "cmd.accept.rank_low");
            case SLOTS_FULL      -> L(lang, "cmd.accept.slots_full");
        };

        // On success, switch to active tab so player sees their new quest
        String tab = result == QuestTracker.AcceptResult.SUCCESS
                ? "active"
                : (quest.getPeriod() == QuestPeriod.WEEKLY ? "weekly" : "daily");
        refreshPage(result == QuestTracker.AcceptResult.SUCCESS ? null : msg,
                result == QuestTracker.AcceptResult.SUCCESS ? msg : null,
                tab);
    }

    // ════════════════════════════════════════════════════════
    //  REFRESH PAGE (sendUpdate — no Loading!)
    // ════════════════════════════════════════════════════════

    private void refreshPage(@Nullable String error, @Nullable String success, @Nonnull String tab) {
        try {
            LangManager lang = plugin.getLangManager();

            UICommandBuilder cmd = new UICommandBuilder();

            // Banners
            cmd.set("#ErrorBanner.Visible", error != null && !error.isEmpty());
            if (error != null && !error.isEmpty()) cmd.set("#ErrorText.Text", stripForUI(error));
            cmd.set("#SuccessBanner.Visible", success != null && !success.isEmpty());
            if (success != null && !success.isEmpty()) cmd.set("#SuccessText.Text", stripForUI(success));

            // Tab visibility
            cmd.set("#DailyContent.Visible", "daily".equals(tab));
            cmd.set("#WeeklyContent.Visible", "weekly".equals(tab));
            cmd.set("#ActiveContent.Visible", "active".equals(tab));

            // Refresh all data
            updateDailyData(cmd, lang);
            updateWeeklyData(cmd, lang);
            updateActiveData(cmd, lang);

            sendUpdate(cmd);
        } catch (Exception e) {
            LOGGER.warn("[refreshPage] sendUpdate failed, falling back to reopen: {}", e.getMessage());
            reopen(error, success, tab);
        }
    }

    // ════════════════════════════════════════════════════════
    //  TAB DATA BUILDERS
    // ════════════════════════════════════════════════════════

    private void updateDailyData(UICommandBuilder cmd, LangManager lang) {
        List<Quest> quests = plugin.getQuestTracker().getAvailableQuests(playerUuid, QuestPeriod.DAILY);
        QuestAvailabilityManager avail = plugin.getQuestTracker().getAvailabilityManager();
        QuestRank playerRank = plugin.getQuestTracker().getRankService().getPlayerRank(playerUuid);
        String acceptText = L(lang, "gui.btn.accept");

        boolean empty = quests.isEmpty();
        cmd.set("#NoDailyMsg.Visible", empty);
        if (empty) cmd.set("#NoDailyMsg.Text", stripForUI(L(lang, "cmd.available.none")));

        for (int i = 0; i < MAX_DAILY; i++) {
            int n = i + 1;
            if (i < quests.size()) {
                Quest q = quests.get(i);
                cmd.set("#D" + n + ".Visible", true);

                // Quest name with rank badge
                String nameText = formatQuestName(lang, q);
                cmd.set("#D" + n + "Name.Text", stripForUI(nameText));
                cmd.set("#D" + n + "Reward.Text", "+" + MessageUtil.formatCoins(q.getReward().getBaseCoins()) + "$");

                // Availability & rank indicators on button
                boolean canAccept = true;
                String btnText = acceptText;

                // Rank check
                if (q.getRequiredRank() != null && !playerRank.canAccess(q.getRequiredRank())) {
                    btnText = L(lang, "gui.btn.locked", "rank", q.getRequiredRank().name());
                    canAccept = false;
                }

                // Slot availability
                if (canAccept && !avail.isAvailable(q, playerUuid)) {
                    int occupied = avail.getOccupiedSlots(q.getQuestId());
                    int maxSlots = q.getMaxSlots();
                    btnText = L(lang, "gui.btn.occupied",
                            "current", String.valueOf(occupied),
                            "max", String.valueOf(maxSlots));
                    canAccept = false;
                }

                cmd.set("#D" + n + "Btn.Text", btnText);
            } else {
                cmd.set("#D" + n + ".Visible", false);
            }
        }
    }

    private void updateWeeklyData(UICommandBuilder cmd, LangManager lang) {
        List<Quest> quests = plugin.getQuestTracker().getAvailableQuests(playerUuid, QuestPeriod.WEEKLY);
        QuestAvailabilityManager avail = plugin.getQuestTracker().getAvailabilityManager();
        QuestRank playerRank = plugin.getQuestTracker().getRankService().getPlayerRank(playerUuid);
        String acceptText = L(lang, "gui.btn.accept");

        boolean empty = quests.isEmpty();
        cmd.set("#NoWeeklyMsg.Visible", empty);
        if (empty) cmd.set("#NoWeeklyMsg.Text", stripForUI(L(lang, "cmd.available.none")));

        for (int i = 0; i < MAX_WEEKLY; i++) {
            int n = i + 1;
            if (i < quests.size()) {
                Quest q = quests.get(i);
                cmd.set("#W" + n + ".Visible", true);

                String nameText = formatQuestName(lang, q);
                cmd.set("#W" + n + "Name.Text", stripForUI(nameText));
                cmd.set("#W" + n + "Reward.Text", "+" + MessageUtil.formatCoins(q.getReward().getBaseCoins()) + "$");

                boolean canAccept = true;
                String btnText = acceptText;

                if (q.getRequiredRank() != null && !playerRank.canAccess(q.getRequiredRank())) {
                    btnText = L(lang, "gui.btn.locked");
                    canAccept = false;
                }

                if (canAccept && !avail.isAvailable(q, playerUuid)) {
                    btnText = L(lang, "gui.btn.occupied");
                    canAccept = false;
                }

                cmd.set("#W" + n + "Btn.Text", btnText);
            } else {
                cmd.set("#W" + n + ".Visible", false);
            }
        }
    }

    private void updateActiveData(UICommandBuilder cmd, LangManager lang) {
        List<PlayerQuestData> active = plugin.getQuestTracker().getActiveQuests(playerUuid);
        TimerService timerService = plugin.getQuestTracker().getTimerService();
        String abandonText = L(lang, "gui.btn.abandon");

        LOGGER.info("[updateActiveData] player={} active quests count={}", playerUuid, active.size());

        boolean empty = active.isEmpty();
        cmd.set("#NoActiveMsg.Visible", empty);
        if (empty) cmd.set("#NoActiveMsg.Text", stripForUI(L(lang, "cmd.active.none")));

        for (int i = 0; i < MAX_ACTIVE; i++) {
            int n = i + 1;
            if (i < active.size()) {
                PlayerQuestData pqd = active.get(i);
                Quest quest = plugin.getQuestTracker().getQuest(pqd.getQuestId());
                if (quest == null) {
                    LOGGER.warn("[updateActiveData] Quest definition MISSING for questId={} (slot A{})", pqd.getQuestId(), n);
                    cmd.set("#A" + n + ".Visible", false);
                    continue;
                }

                double required = quest.getObjective().getRequiredAmount();
                double current = pqd.getCurrentProgress();
                int pct = required > 0 ? (int) ((current / required) * 100) : 100;
                String periodIcon = quest.getPeriod() == QuestPeriod.WEEKLY ? "[W]" : "[D]";

                // Добавляем таймер если есть
                String timerText = "";
                if (quest.hasTimer()) {
                    String timeLeft = timerService.formatRemainingTime(quest.getQuestId(), playerUuid);
                    timerText = " [" + timeLeft + "]";
                }

                String progressText = periodIcon + " " + (int) current + "/" + (int) required
                        + " (" + pct + "%)" + timerText;

                cmd.set("#A" + n + ".Visible", true);
                cmd.set("#A" + n + "Name.Text", stripForUI(formatQuestName(lang, quest)));
                cmd.set("#A" + n + "Reward.Text", "+" + MessageUtil.formatCoins(quest.getReward().getBaseCoins()) + "$");
                cmd.set("#A" + n + "Progress.Text", progressText);
                cmd.set("#A" + n + "Btn.Text", abandonText);
            } else {
                cmd.set("#A" + n + ".Visible", false);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  RE-OPEN (with new state)
    // ════════════════════════════════════════════════════════

    private void reopen(@Nullable String error, @Nullable String success, @Nonnull String tab) {
        close();
        PlayerQuestsGui newPage = new PlayerQuestsGui(plugin, playerRef, playerUuid, error, success, tab);
        PageOpenHelper.openPage(savedRef, savedStore, newPage);
    }

    // ════════════════════════════════════════════════════════
    //  STATIC OPEN (entry point from commands)
    // ════════════════════════════════════════════════════════

    public static void open(@Nonnull EcoTaleQuestsPlugin plugin,
                            @Nonnull PlayerRef playerRef,
                            @Nonnull Ref<EntityStore> ref,
                            @Nonnull Store<EntityStore> store,
                            @Nonnull UUID playerUuid) {
        PlayerQuestsGui page = new PlayerQuestsGui(plugin, playerRef, playerUuid);
        PageOpenHelper.openPage(ref, store, page);
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    private String L(LangManager lang, String key, String... args) {
        return lang.getForPlayer(playerUuid, key, args);
    }

    private String localizedDesc(LangManager lang, Quest quest) {
        QuestObjective obj = quest.getObjective();
        String typeKey = "quest.desc." + obj.getType().getId();
        String target = obj.getTarget();
        String display = (target != null && !target.isEmpty())
                ? L(lang, "target." + target.toLowerCase()) : "";
        return L(lang, typeKey,
                "amount", String.valueOf((int) obj.getRequiredAmount()),
                "target", display);
    }

    /**
     * Формирует имя квеста с бейджами ранга и типа доступа.
     */
    private String formatQuestName(LangManager lang, Quest quest) {
        StringBuilder sb = new StringBuilder();

        // Ранг бейдж (показываем только для квестов выше E-ранга — E доступен всем)
        if (quest.getRequiredRank() != null && quest.getRequiredRank() != QuestRank.E) {
            sb.append("[").append(quest.getRequiredRank().name()).append("] ");
        }

        // Тип доступа бейдж
        if (quest.getAccessType() == QuestAccessType.GLOBAL_UNIQUE) {
            sb.append("(!) ");
        } else if (quest.getAccessType() == QuestAccessType.LIMITED_SLOTS) {
            QuestAvailabilityManager avail = plugin.getQuestTracker().getAvailabilityManager();
            int occupied = avail.getOccupiedSlots(quest.getQuestId());
            sb.append("(").append(occupied).append("/").append(quest.getMaxSlots()).append(") ");
        }

        sb.append(localizedDesc(lang, quest));

        // Таймер маркер (показываем общую длительность квеста)
        if (quest.hasTimer()) {
            sb.append(L(lang, "gui.timer_suffix", "time", String.valueOf(quest.getDurationMinutes())));
        }

        return sb.toString();
    }

    /** Strip non-renderable chars and MiniMessage tags for Hytale UI labels. */
    private static String stripForUI(String text) {
        if (text == null) return "";
        return text.replace("\u2714 ", "").replace("\u2714", "")
                   .replaceAll("<[^>]+>", "").trim();
    }

    private static int parseSlot(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    // ════════════════════════════════════════════════════════
    //  EVENT DATA CLASS
    // ════════════════════════════════════════════════════════

    public static class QuestEventData {
        public String action = "";
        public String slot = "";
        public String tab = "";
    }
}
