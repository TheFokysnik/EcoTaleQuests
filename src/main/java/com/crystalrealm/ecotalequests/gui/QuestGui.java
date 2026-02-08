package com.crystalrealm.ecotalequests.gui;

import au.ellie.hyui.builders.PageBuilder;

import com.crystalrealm.ecotalequests.EcoTaleQuestsPlugin;
import com.crystalrealm.ecotalequests.lang.LangManager;
import com.crystalrealm.ecotalequests.model.*;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Quest GUI panel built with HyUI (HYUIML).
 * Uses {@link PageBuilder} to create a tabbed quest panel
 * with Daily / Weekly / Active tabs and interactive buttons.
 */
public final class QuestGui {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private QuestGui() {} // utility class

    /**
     * Build and open the quest panel for a player.
     *
     * @param plugin     plugin instance for accessing tracker, lang, etc.
     * @param playerRef  Hytale PlayerRef (ECS component)
     * @param store      entity store
     * @param playerUuid player UUID for quest data and localization
     */
    public static void open(@Nonnull EcoTaleQuestsPlugin plugin,
                            @Nonnull PlayerRef playerRef,
                            @Nonnull Store<EntityStore> store,
                            @Nonnull UUID playerUuid) {

        LangManager lang     = plugin.getLangManager();
        QuestTracker tracker  = plugin.getQuestTracker();

        // ── collect quest data ──────────────────────────────────
        List<Quest> dailyQuests  = tracker.getAvailableQuests(playerUuid, QuestPeriod.DAILY);
        List<Quest> weeklyQuests = tracker.getAvailableQuests(playerUuid, QuestPeriod.WEEKLY);
        List<PlayerQuestData> activeQuests = tracker.getActiveQuests(playerUuid);

        // maps: button-id -> quest UUID  (for event handlers)
        Map<String, UUID> acceptMap  = new LinkedHashMap<>();
        Map<String, UUID> abandonMap = new LinkedHashMap<>();

        // ── build HYUIML ────────────────────────────────────────
        StringBuilder html = new StringBuilder();
        html.append(CSS);
        html.append(headerHtml(lang, playerUuid));

        // Daily tab content
        html.append(tabOpen("daily"));
        if (dailyQuests.isEmpty()) {
            html.append(emptyLabel(lang, playerUuid, "cmd.available.none"));
        } else {
            for (Quest q : dailyQuests) {
                String btnId = "accept-" + q.getShortId();
                acceptMap.put(btnId, q.getQuestId());
                html.append(availableCard(lang, playerUuid, q, btnId));
            }
        }
        html.append(TAB_CLOSE);

        // Weekly tab content
        html.append(tabOpen("weekly"));
        if (weeklyQuests.isEmpty()) {
            html.append(emptyLabel(lang, playerUuid, "cmd.available.none"));
        } else {
            for (Quest q : weeklyQuests) {
                String btnId = "accept-" + q.getShortId();
                acceptMap.put(btnId, q.getQuestId());
                html.append(availableCard(lang, playerUuid, q, btnId));
            }
        }
        html.append(TAB_CLOSE);

        // Active tab content
        html.append(tabOpen("active"));
        if (activeQuests.isEmpty()) {
            html.append(emptyLabel(lang, playerUuid, "cmd.active.none"));
        } else {
            for (PlayerQuestData pqd : activeQuests) {
                Quest quest = tracker.getQuest(pqd.getQuestId());
                if (quest == null) continue;
                String btnId = "abandon-" + quest.getShortId();
                abandonMap.put(btnId, quest.getQuestId());
                html.append(activeCard(lang, playerUuid, quest, pqd, btnId));
            }
        }
        html.append(TAB_CLOSE);

        html.append(FOOTER_HTML);

        // ── create HyUI page ────────────────────────────────────
        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html.toString())
                .withLifetime(CustomPageLifetime.CanDismiss);

        // register accept-button event handlers
        for (var entry : acceptMap.entrySet()) {
            String btnId   = entry.getKey();
            UUID   questId = entry.getValue();
            builder.addEventListener(btnId, CustomUIEventBindingType.Activating, (data, ctx) -> {
                if (!plugin.getAbuseGuard().canAcceptQuest(playerUuid)) {
                    playerRef.sendMessage(Message.raw(L(lang, playerUuid, "cmd.accept.cooldown")));
                    return;
                }
                QuestTracker.AcceptResult result = tracker.acceptQuest(playerUuid, questId);
                LOGGER.info("GUI accept {} -> {}", questId, result);
                String msg = switch (result) {
                    case SUCCESS -> {
                        plugin.getAbuseGuard().recordAccept(playerUuid);
                        Quest q = tracker.getQuest(questId);
                        String name = q != null ? localizedDesc(lang, playerUuid, q)
                                                : questId.toString().substring(0, 8);
                        yield L(lang, playerUuid, "cmd.accept.success", "name", name);
                    }
                    case ALREADY_ACTIVE  -> L(lang, playerUuid, "cmd.accept.already_active");
                    case LIMIT_REACHED   -> L(lang, playerUuid, "cmd.accept.limit");
                    case DUPLICATE_TYPE  -> L(lang, playerUuid, "cmd.accept.duplicate");
                    case QUEST_EXPIRED   -> L(lang, playerUuid, "cmd.accept.expired");
                    case QUEST_NOT_FOUND -> L(lang, playerUuid, "cmd.accept.not_found",
                                              "id", questId.toString().substring(0, 8));
                };
                playerRef.sendMessage(Message.raw(msg));
            });
        }

        // register abandon-button event handlers
        for (var entry : abandonMap.entrySet()) {
            String btnId   = entry.getKey();
            UUID   questId = entry.getValue();
            builder.addEventListener(btnId, CustomUIEventBindingType.Activating, (data, ctx) -> {
                QuestTracker.AbandonResult result = tracker.abandonQuest(playerUuid, questId);
                LOGGER.info("GUI abandon {} -> {}", questId, result);
                String msg = switch (result) {
                    case SUCCESS       -> L(lang, playerUuid, "cmd.abandon.success");
                    case NOT_ACTIVE    -> L(lang, playerUuid, "cmd.abandon.not_active");
                    case NOT_FOUND     -> L(lang, playerUuid, "cmd.abandon.not_found",
                                            "id", questId.toString().substring(0, 8));
                    case LIMIT_REACHED -> L(lang, playerUuid, "cmd.abandon.limit");
                };
                playerRef.sendMessage(Message.raw(msg));
            });
        }

        builder.open(store);
        LOGGER.info("Quest GUI opened for {}", playerUuid);
    }

    // ═══════════════════════════════════════════════════════════
    //  HTML FRAGMENT BUILDERS
    // ═══════════════════════════════════════════════════════════

    private static String headerHtml(LangManager lang, UUID uuid) {
        String title  = esc(L(lang, uuid, "cmd.available.header"));
        String daily  = esc("[D] " + L(lang, uuid, "quest.period.daily"));
        String weekly = esc("[W] " + L(lang, uuid, "quest.period.weekly"));
        String active = esc(L(lang, uuid, "gui.tab.active"));

        return """
            <div class="page-overlay">
              <div class="decorated-container" data-hyui-title="%s"
                   style="anchor-width: 720; anchor-height: 550;">
                <div class="container-contents" style="layout-mode: Top; padding: 6;">
                  <nav id="quest-tabs" class="tabs"
                       data-tabs="daily:%s:daily-content,weekly:%s:weekly-content,active:%s:active-content"
                       data-selected="daily">
                  </nav>
            """.formatted(title, daily, weekly, active);
    }

    private static String tabOpen(String tabId) {
        return """
            <div id="%s-content" class="tab-content" data-hyui-tab-id="%s"
                 style="layout-mode: Top; padding: 4;">
            """.formatted(tabId, tabId);
    }

    private static final String TAB_CLOSE = "</div>\n";

    private static final String FOOTER_HTML = """
                </div>
              </div>
            </div>
            """;

    private static String emptyLabel(LangManager lang, UUID uuid, String key) {
        return "<p style=\"color: #888888; font-size: 14; padding: 20;\">"
                + esc(L(lang, uuid, key)) + "</p>\n";
    }

    /** Card for an available (not yet accepted) quest. */
    private static String availableCard(LangManager lang, UUID uuid,
                                        Quest quest, String btnId) {
        String name      = esc(localizedDesc(lang, uuid, quest));
        String objective = esc(localizedObjective(lang, uuid, quest));
        String reward    = esc("+" + MessageUtil.formatCoins(quest.getReward().getBaseCoins()) + "$");
        String btnText   = esc(L(lang, uuid, "gui.btn.accept"));

        return """
            <div style="background-color: #1a1a2e(0.85); padding: 8; layout-mode: Top;">
              <div style="layout-mode: Left;">
                <p style="color: #4CAF50; font-size: 16; font-weight: bold; flex-weight: 1;">%s</p>
                <p style="color: #FFD700; font-size: 14; font-weight: bold;">%s</p>
              </div>
              <p style="color: #cccccc; font-size: 13;">%s</p>
              <div style="layout-mode: Left; padding-top: 4;">
                <button id="%s" class="small-secondary-button">%s</button>
              </div>
            </div>
            """.formatted(name, reward, objective, btnId, btnText);
    }

    /** Card for an active quest with progress bar. */
    private static String activeCard(LangManager lang, UUID uuid,
                                     Quest quest, PlayerQuestData pqd, String btnId) {
        String name        = esc(localizedDesc(lang, uuid, quest));
        double required    = quest.getObjective().getRequiredAmount();
        double current     = pqd.getCurrentProgress();
        String progressTxt = esc((int) current + " / " + (int) required);
        int    progressPct = required > 0 ? (int) ((current / required) * 100) : 100;
        String reward      = esc("+" + MessageUtil.formatCoins(quest.getReward().getBaseCoins()) + "$");
        String btnText     = esc(L(lang, uuid, "gui.btn.abandon"));

        return """
            <div style="background-color: #1a1a2e(0.85); padding: 8; layout-mode: Top;">
              <div style="layout-mode: Left;">
                <p style="color: #4CAF50; font-size: 16; font-weight: bold; flex-weight: 1;">%s</p>
                <p style="color: #FFD700; font-size: 14; font-weight: bold;">%s</p>
              </div>
              <p style="color: #cccccc; font-size: 13;">%s</p>
              <div style="layout-mode: Left; padding-top: 4;">
                <progress value="%d" max="100" style="flex-weight: 1; anchor-height: 14;"></progress>
                <button id="%s" class="small-tertiary-button">%s</button>
              </div>
            </div>
            """.formatted(name, reward, progressTxt, progressPct, btnId, btnText);
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    private static String L(LangManager lang, UUID uuid, String key, String... args) {
        return lang.getForPlayer(uuid, key, args);
    }

    private static String localizedDesc(LangManager lang, UUID uuid, Quest quest) {
        QuestObjective obj = quest.getObjective();
        String typeKey = "quest.desc." + obj.getType().getId();
        String target  = obj.getTarget();
        String display = (target != null && !target.isEmpty())
                ? L(lang, uuid, "target." + target.toLowerCase()) : "";
        return L(lang, uuid, typeKey,
                "amount", String.valueOf((int) obj.getRequiredAmount()),
                "target", display);
    }

    private static String localizedObjective(LangManager lang, UUID uuid, Quest quest) {
        QuestObjective obj = quest.getObjective();
        String target  = obj.getTarget();
        String display = (target != null && !target.isEmpty())
                ? L(lang, uuid, "target." + target.toLowerCase()) : "";
        return obj.getType().getId() + " -> " + display + " x" + (int) obj.getRequiredAmount();
    }

    /** Minimal HTML entity escaping. */
    private static String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private static final String CSS = """
        <style>
            .empty-msg {
                color: #888888;
                font-size: 14;
                padding: 20;
            }
        </style>
        """;
}
