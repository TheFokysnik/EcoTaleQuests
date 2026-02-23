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
 *   <li><b>Top</b>    — leaderboard of top players by rank points (up to 10 rows, T1–T10)</li>
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
    private static final int MAX_TOP    = 10;

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
    private final PlayerRef playerRef;
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
        this.playerRef = playerRef;
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

        // ── Rank Bar ────────────────────────────────────────
        QuestRank playerRank = plugin.getQuestTracker().getRankService().getPlayerRank(playerUuid);
        PlayerRankData rankData = plugin.getQuestTracker().getRankService().getRankData(playerUuid);

        // Update last known name for leaderboard
        String username = playerRef.getUsername();
        if (username != null && !username.isEmpty()) {
            rankData.setLastKnownName(username);
        }

        cmd.set("#RankBadgeLabel.Text", "[" + playerRank.name() + "]");
        cmd.set("#RankNameLabel.Text", L(lang, "gui.rank_name." + playerRank.name().toLowerCase()));
        cmd.set("#RankPointsLabel.Text", rankData.getRankPoints() + " pts");

        // Progress to next rank
        QuestRank nextRank = playerRank.next();
        if (nextRank != null) {
            int current = rankData.getRankPoints();
            int need = nextRank.getRequiredPoints();
            int prev = playerRank.getRequiredPoints();
            int pct = need > prev ? (int) ((double)(current - prev) / (need - prev) * 100) : 100;
            cmd.set("#RankProgressLabel.Text", buildProgressBar(pct) + " -> " + nextRank.name());
        } else {
            cmd.set("#RankProgressLabel.Text", L(lang, "gui.rank_max"));
        }

        // Tab labels
        cmd.set("#TabDaily.Text", L(lang, "quest.period.daily"));
        cmd.set("#TabWeekly.Text", L(lang, "quest.period.weekly"));
        cmd.set("#TabActive.Text", L(lang, "gui.tab.active"));
        cmd.set("#TabTop.Text", L(lang, "gui.tab.top"));

        // Tab visibility
        cmd.set("#DailyContent.Visible", "daily".equals(selectedTab));
        cmd.set("#WeeklyContent.Visible", "weekly".equals(selectedTab));
        cmd.set("#ActiveContent.Visible", "active".equals(selectedTab));
        cmd.set("#TopContent.Visible", "top".equals(selectedTab));

        // ── Bind ALL events (once, slot-based) ──────────────

        // Tab switching
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabDaily",
                new EventData().append(KEY_ACTION, "tab").append(KEY_TAB, "daily"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabWeekly",
                new EventData().append(KEY_ACTION, "tab").append(KEY_TAB, "weekly"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabActive",
                new EventData().append(KEY_ACTION, "tab").append(KEY_TAB, "active"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabTop",
                new EventData().append(KEY_ACTION, "tab").append(KEY_TAB, "top"));

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
        updateTopData(cmd, lang);

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
                    tabCmd.set("#TopContent.Visible", "top".equals(data.tab));
                    tabCmd.set("#ErrorBanner.Visible", false);
                    tabCmd.set("#SuccessBanner.Visible", false);
                    // Refresh data for the selected tab
                    updateDailyData(tabCmd, lang);
                    updateWeeklyData(tabCmd, lang);
                    updateActiveData(tabCmd, lang);
                    updateTopData(tabCmd, lang);
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

        // Update tab stats
        int maxDailyActive = plugin.getConfigManager().getConfig().getQuestLimits().getMaxDailyActive();
        long activeDailyCount = plugin.getQuestTracker().getActiveQuests(playerUuid).stream()
                .filter(pqd -> {
                    Quest q = plugin.getQuestTracker().getQuest(pqd.getQuestId());
                    return q != null && q.getPeriod() == QuestPeriod.DAILY;
                }).count();
        cmd.set("#TabStatsLabel.Text", L(lang, "gui.stats.active_daily",
                "current", String.valueOf(activeDailyCount),
                "max", String.valueOf(maxDailyActive)));

        for (int i = 0; i < MAX_DAILY; i++) {
            int n = i + 1;
            String p = "#D" + n;
            if (i < quests.size()) {
                Quest q = quests.get(i);
                cmd.set(p + ".Visible", true);

                // Category icon
                cmd.set(p + "Cat.Text", getCategoryIcon(q.getObjective().getType()));

                // Rank badge
                QuestRank rank = q.getRequiredRank();
                if (rank != null && rank != QuestRank.E) {
                    cmd.set(p + "Rank.Text", "[" + rank.name() + "]");
                } else {
                    cmd.set(p + "Rank.Text", "");
                }

                // Quest name (clean, without badges)
                cmd.set(p + "Name.Text", stripForUI(localizedDesc(lang, q)));

                // XP reward
                int xpReward = q.getReward().getBonusXp();
                cmd.set(p + "XP.Text", xpReward > 0 ? "+" + xpReward + " XP" : "");

                // Coins reward
                cmd.set(p + "Reward.Text", formatRewardCoins(q.getReward().getBaseCoins()));

                // Description line (access type + timer info)
                cmd.set(p + "Desc.Text", formatQuestSubline(lang, q));

                // Button
                boolean canAccept = true;
                String btnText = acceptText;

                if (rank != null && !playerRank.canAccess(rank)) {
                    btnText = L(lang, "gui.btn.locked", "rank", rank.name())
                            .replace("{rank}", rank.name());
                    canAccept = false;
                }
                if (canAccept && !avail.isAvailable(q, playerUuid)) {
                    int occupied = avail.getOccupiedSlots(q.getQuestId());
                    int maxSlots = q.getMaxSlots();
                    btnText = L(lang, "gui.btn.occupied",
                            "current", String.valueOf(occupied),
                            "max", String.valueOf(maxSlots))
                            .replace("{current}", String.valueOf(occupied))
                            .replace("{max}", String.valueOf(maxSlots));
                    canAccept = false;
                }

                cmd.set(p + "Btn.Text", btnText);
            } else {
                cmd.set(p + ".Visible", false);
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
            String p = "#W" + n;
            if (i < quests.size()) {
                Quest q = quests.get(i);
                cmd.set(p + ".Visible", true);

                cmd.set(p + "Cat.Text", getCategoryIcon(q.getObjective().getType()));

                QuestRank rank = q.getRequiredRank();
                if (rank != null && rank != QuestRank.E) {
                    cmd.set(p + "Rank.Text", "[" + rank.name() + "]");
                } else {
                    cmd.set(p + "Rank.Text", "");
                }

                cmd.set(p + "Name.Text", stripForUI(localizedDesc(lang, q)));

                int xpReward = q.getReward().getBonusXp();
                cmd.set(p + "XP.Text", xpReward > 0 ? "+" + xpReward + " XP" : "");
                cmd.set(p + "Reward.Text", formatRewardCoins(q.getReward().getBaseCoins()));

                cmd.set(p + "Desc.Text", formatQuestSubline(lang, q));

                boolean canAccept = true;
                String btnText = acceptText;

                if (rank != null && !playerRank.canAccess(rank)) {
                    btnText = L(lang, "gui.btn.locked", "rank", rank.name())
                            .replace("{rank}", rank.name());
                    canAccept = false;
                }
                if (canAccept && !avail.isAvailable(q, playerUuid)) {
                    int occupied = avail.getOccupiedSlots(q.getQuestId());
                    int maxSlots = q.getMaxSlots();
                    btnText = L(lang, "gui.btn.occupied",
                            "current", String.valueOf(occupied),
                            "max", String.valueOf(maxSlots))
                            .replace("{current}", String.valueOf(occupied))
                            .replace("{max}", String.valueOf(maxSlots));
                    canAccept = false;
                }

                cmd.set(p + "Btn.Text", btnText);
            } else {
                cmd.set(p + ".Visible", false);
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
            String p = "#A" + n;
            if (i < active.size()) {
                PlayerQuestData pqd = active.get(i);
                Quest quest = plugin.getQuestTracker().getQuest(pqd.getQuestId());
                if (quest == null) {
                    LOGGER.warn("[updateActiveData] Quest definition MISSING for questId={} (slot A{})", pqd.getQuestId(), n);
                    cmd.set(p + ".Visible", false);
                    continue;
                }

                double required = quest.getObjective().getRequiredAmount();
                double current = pqd.getCurrentProgress();
                int pct = required > 0 ? (int) ((current / required) * 100) : 100;

                cmd.set(p + ".Visible", true);

                // Category icon
                cmd.set(p + "Cat.Text", getCategoryIcon(quest.getObjective().getType()));

                // Period badge
                cmd.set(p + "Period.Text", quest.getPeriod() == QuestPeriod.WEEKLY ? "W" : "D");

                // Quest name
                cmd.set(p + "Name.Text", stripForUI(localizedDesc(lang, quest)));

                // XP reward
                int xpReward = quest.getReward().getBonusXp();
                cmd.set(p + "XP.Text", xpReward > 0 ? "+" + xpReward + " XP" : "");

                // Reward
                cmd.set(p + "Reward.Text", formatRewardCoins(quest.getReward().getBaseCoins()));

                // Visual progress bar
                cmd.set(p + "ProgBar.Text", buildProgressBar(pct) + " " + pct + "%");

                // Timer
                if (quest.hasTimer()) {
                    String timeLeft = timerService.formatRemainingTime(quest.getQuestId(), playerUuid);
                    cmd.set(p + "Timer.Text", timeLeft);
                } else {
                    cmd.set(p + "Timer.Text", "");
                }

                // Detailed progress text
                cmd.set(p + "Progress.Text", (int) current + " / " + (int) required);

                cmd.set(p + "Btn.Text", abandonText);
            } else {
                cmd.set(p + ".Visible", false);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  TOP (LEADERBOARD) TAB
    // ════════════════════════════════════════════════════════

    private void updateTopData(UICommandBuilder cmd, LangManager lang) {
        Collection<PlayerRankData> allData = plugin.getStorage().getAllRankData();

        // Sort by rank points descending, then by totalCompleted descending
        List<PlayerRankData> sorted = allData.stream()
                .filter(d -> d.getRankPoints() > 0 || d.getTotalCompleted() > 0)
                .sorted(Comparator.comparingInt(PlayerRankData::getRankPoints).reversed()
                        .thenComparingInt(PlayerRankData::getTotalCompleted).reversed())
                .limit(MAX_TOP)
                .toList();

        // Header columns
        cmd.set("#TopHPos.Text", L(lang, "gui.top.h_pos"));
        cmd.set("#TopHName.Text", L(lang, "gui.top.h_name"));
        cmd.set("#TopHRank.Text", L(lang, "gui.top.h_rank"));
        cmd.set("#TopHPts.Text", L(lang, "gui.top.h_pts"));
        cmd.set("#TopHDone.Text", L(lang, "gui.top.h_done"));

        boolean empty = sorted.isEmpty();
        cmd.set("#NoTopMsg.Visible", empty);
        if (empty) cmd.set("#NoTopMsg.Text", stripForUI(L(lang, "gui.top.empty")));

        for (int i = 0; i < MAX_TOP; i++) {
            int n = i + 1;
            String p = "#T" + n;
            if (i < sorted.size()) {
                PlayerRankData rd = sorted.get(i);
                QuestRank rank = rd.getRank();
                String name = rd.getLastKnownName();
                if (name == null || name.isEmpty()) {
                    name = rd.getPlayerUuid().toString().substring(0, 8);
                }

                cmd.set(p + ".Visible", true);
                cmd.set(p + "Pos.Text", "#" + n);
                cmd.set(p + "Name.Text", name);
                cmd.set(p + "Rank.Text", "[" + rank.name() + "]");
                cmd.set(p + "Pts.Text", rd.getRankPoints() + " pts");
                cmd.set(p + "Done.Text", rd.getTotalCompleted() + " done");
            } else {
                cmd.set(p + ".Visible", false);
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

        if (quest.getRequiredRank() != null && quest.getRequiredRank() != QuestRank.E) {
            sb.append("[").append(quest.getRequiredRank().name()).append("] ");
        }

        if (quest.getAccessType() == QuestAccessType.GLOBAL_UNIQUE) {
            sb.append("(!) ");
        } else if (quest.getAccessType() == QuestAccessType.LIMITED_SLOTS) {
            QuestAvailabilityManager avail = plugin.getQuestTracker().getAvailabilityManager();
            int occupied = avail.getOccupiedSlots(quest.getQuestId());
            sb.append("(").append(occupied).append("/").append(quest.getMaxSlots()).append(") ");
        }

        sb.append(localizedDesc(lang, quest));

        if (quest.hasTimer()) {
            sb.append(L(lang, "gui.timer_suffix", "time", String.valueOf(quest.getDurationMinutes())));
        }

        return sb.toString();
    }

    /**
     * Формирует подстроку квеста: тип доступа + таймер.
     */
    private String formatQuestSubline(LangManager lang, Quest quest) {
        StringBuilder sb = new StringBuilder();

        if (quest.getAccessType() == QuestAccessType.GLOBAL_UNIQUE) {
            sb.append(L(lang, "gui.access.global_unique"));
        } else if (quest.getAccessType() == QuestAccessType.LIMITED_SLOTS) {
            QuestAvailabilityManager avail = plugin.getQuestTracker().getAvailabilityManager();
            int occupied = avail.getOccupiedSlots(quest.getQuestId());
            sb.append(L(lang, "gui.access.limited",
                    "current", String.valueOf(occupied),
                    "max", String.valueOf(quest.getMaxSlots())));
        }

        if (quest.hasTimer()) {
            if (!sb.isEmpty()) sb.append("  |  ");
            sb.append(L(lang, "gui.timer_info", "minutes", String.valueOf(quest.getDurationMinutes())));
        }

        if (quest.getRankPoints() > 0) {
            if (!sb.isEmpty()) sb.append("  |  ");
            sb.append("+" + quest.getRankPoints() + " RP");
        }

        return sb.toString();
    }

    /**
     * Возвращает текстовую иконку для категории квеста.
     */
    private String getCategoryIcon(QuestType type) {
        return switch (type) {
            case KILL_MOB     -> "[M]";
            case MINE_ORE     -> "[O]";
            case CHOP_WOOD    -> "[W]";
            case HARVEST_CROP -> "[H]";
            case EARN_COINS   -> "[" + plugin.getConfigManager().getConfig().getGeneral().getCurrencySymbol() + "]";
            case GAIN_XP      -> "[X]";
            case KILL_BOSS    -> "[B]";
            default           -> "[?]";
        };
    }

    /**
     * Строит текстовый прогресс-бар из символов.
     * Пример: [||||||||------] 80%
     */
    private static String buildProgressBar(int pct) {
        int total = 16;
        int filled = Math.max(0, Math.min(total, (int) Math.round(pct / 100.0 * total)));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < total; i++) {
            sb.append(i < filled ? "|" : "-");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Strip non-renderable chars and MiniMessage tags for Hytale UI labels. */
    private static String stripForUI(String text) {
        if (text == null) return "";
        return text.replace("\u2714 ", "").replace("\u2714", "")
                   .replaceAll("<[^>]+>", "").trim();
    }

    /** Format coin reward for display using config symbol & rounding. */
    private String formatRewardCoins(double coins) {
        var general = plugin.getConfigManager().getConfig().getGeneral();
        String formatted = MessageUtil.formatCoins(coins, general.isRoundCurrency());
        return "+" + formatted + general.getCurrencySymbol();
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
