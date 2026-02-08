package com.crystalrealm.ecotalequests.commands;

import com.crystalrealm.ecotalequests.EcoTaleQuestsPlugin;
import com.crystalrealm.ecotalequests.gui.QuestGui;
import com.crystalrealm.ecotalequests.lang.LangManager;
import com.crystalrealm.ecotalequests.model.*;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.MiniMessageParser;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Команда {@code /quests} — коллекция суб-команд.
 * Использует {@link AbstractCommandCollection} с диспатчем через {@code addSubCommand()}.
 * Аргументы парсятся из {@code getInputString()} через {@link #parseTrailingArg(CommandContext)}.
 */
public class QuestsCommandCollection extends AbstractCommandCollection {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private final EcoTaleQuestsPlugin plugin;

    /** Keywords that are command/subcommand names (not real arguments). */
    private static final Set<String> COMMAND_KEYWORDS = Set.of(
            "quests", "active", "available", "accept", "abandon",
            "info", "stats", "reload", "lang", "help", "gui"
    );

    private static Message msg(String miniMessage) {
        return Message.parse(MiniMessageParser.toJson(miniMessage));
    }

    public QuestsCommandCollection(EcoTaleQuestsPlugin plugin) {
        super("quests", "EcoTaleQuests — daily and weekly quest system");
        this.plugin = plugin;

        addSubCommand(new ActiveSubCommand());
        addSubCommand(new AvailableSubCommand());
        addSubCommand(new AcceptSubCommand());
        addSubCommand(new AbandonSubCommand());
        addSubCommand(new InfoSubCommand());
        addSubCommand(new StatsSubCommand());
        addSubCommand(new GuiSubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new LangSubCommand());
        addSubCommand(new HelpSubCommand());
    }

    // ═════════════════════════════════════════════════════════════
    //  SUB-COMMANDS
    // ═════════════════════════════════════════════════════════════

    // ── /quests active (default) ────────────────────────────────

    private class ActiveSubCommand extends AbstractAsyncCommand {
        ActiveSubCommand() { super("active", "Show active quests"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.use")) return done();

            LOGGER.info("[quests active] input='{}'", context.getInputString());

            UUID uuid = sender.getUuid();
            List<PlayerQuestData> active = plugin.getQuestTracker().getActiveQuests(uuid);

            context.sendMessage(msg(L(sender, "cmd.active.header")));

            if (active.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.active.none")));
            } else {
                for (PlayerQuestData pqd : active) {
                    Quest quest = plugin.getQuestTracker().getQuest(pqd.getQuestId());
                    if (quest == null) continue;

                    double required = quest.getObjective().getRequiredAmount();
                    String bar = MessageUtil.progressBar(pqd.getCurrentProgress(), required, 10);
                    String periodIcon = quest.getPeriod() == QuestPeriod.WEEKLY ? "[W]" : "[D]";

                    context.sendMessage(msg(L(sender, "cmd.active.entry",
                            "icon", periodIcon,
                            "id", quest.getShortId(),
                            "name", localizedDesc(sender, quest),
                            "current", String.valueOf((int) pqd.getCurrentProgress()),
                            "required", String.valueOf((int) required),
                            "bar", bar)));
                }
            }

            context.sendMessage(msg(L(sender, "cmd.active.footer")));
            return done();
        }
    }

    // ── /quests available ───────────────────────────────────────

    private class AvailableSubCommand extends AbstractAsyncCommand {
        AvailableSubCommand() { super("available", "Show available quests"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.use")) return done();

            UUID uuid = sender.getUuid();
            QuestTracker tracker = plugin.getQuestTracker();

            context.sendMessage(msg(L(sender, "cmd.available.header")));

            List<Quest> dailyAvail = tracker.getAvailableQuests(uuid, QuestPeriod.DAILY);
            if (!dailyAvail.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.available.daily_header")));
                for (Quest q : dailyAvail) {
                    context.sendMessage(msg(L(sender, "cmd.available.entry",
                            "id", q.getShortId(),
                            "desc", localizedDesc(sender, q),
                            "reward", MessageUtil.formatCoins(q.getReward().getBaseCoins()))));
                }
            }

            List<Quest> weeklyAvail = tracker.getAvailableQuests(uuid, QuestPeriod.WEEKLY);
            if (!weeklyAvail.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.available.weekly_header")));
                for (Quest q : weeklyAvail) {
                    context.sendMessage(msg(L(sender, "cmd.available.entry",
                            "id", q.getShortId(),
                            "desc", localizedDesc(sender, q),
                            "reward", MessageUtil.formatCoins(q.getReward().getBaseCoins()))));
                }
            }

            if (dailyAvail.isEmpty() && weeklyAvail.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.available.none")));
            }

            context.sendMessage(msg(L(sender, "cmd.available.footer")));
            return done();
        }
    }

    // ── /quests accept <id> ─────────────────────────────────────

    private class AcceptSubCommand extends AbstractAsyncCommand {
        AcceptSubCommand() { super("accept", "Accept a quest"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.use")) return done();

            String questIdStr = parseTrailingArg(context);
            LOGGER.info("[quests accept] input='{}' parsed_arg='{}'",
                    context.getInputString(), questIdStr);

            if (questIdStr == null || questIdStr.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.accept.usage")));
                return done();
            }

            UUID questId = resolveQuestId(questIdStr);
            if (questId == null) {
                context.sendMessage(msg(L(sender, "cmd.accept.not_found", "id", questIdStr)));
                return done();
            }

            if (!plugin.getAbuseGuard().canAcceptQuest(sender.getUuid())) {
                context.sendMessage(msg(L(sender, "cmd.accept.cooldown")));
                return done();
            }

            QuestTracker.AcceptResult result = plugin.getQuestTracker()
                    .acceptQuest(sender.getUuid(), questId);

            switch (result) {
                case SUCCESS -> {
                    plugin.getAbuseGuard().recordAccept(sender.getUuid());
                    Quest quest = plugin.getQuestTracker().getQuest(questId);
                    String name = quest != null ? localizedDesc(sender, quest) : questIdStr;
                    context.sendMessage(msg(L(sender, "cmd.accept.success", "name", name)));
                }
                case QUEST_NOT_FOUND -> context.sendMessage(msg(L(sender, "cmd.accept.not_found", "id", questIdStr)));
                case QUEST_EXPIRED   -> context.sendMessage(msg(L(sender, "cmd.accept.expired")));
                case LIMIT_REACHED   -> context.sendMessage(msg(L(sender, "cmd.accept.limit")));
                case DUPLICATE_TYPE  -> context.sendMessage(msg(L(sender, "cmd.accept.duplicate")));
                case ALREADY_ACTIVE  -> context.sendMessage(msg(L(sender, "cmd.accept.already_active")));
            }

            return done();
        }
    }

    // ── /quests abandon <id> ────────────────────────────────────

    private class AbandonSubCommand extends AbstractAsyncCommand {
        AbandonSubCommand() { super("abandon", "Abandon a quest"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.use")) return done();

            String questIdStr = parseTrailingArg(context);
            LOGGER.info("[quests abandon] input='{}' parsed_arg='{}'",
                    context.getInputString(), questIdStr);

            if (questIdStr == null || questIdStr.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.abandon.usage")));
                return done();
            }

            UUID questId = resolveQuestId(questIdStr);
            if (questId == null) {
                context.sendMessage(msg(L(sender, "cmd.abandon.not_found", "id", questIdStr)));
                return done();
            }

            QuestTracker.AbandonResult result = plugin.getQuestTracker()
                    .abandonQuest(sender.getUuid(), questId);

            switch (result) {
                case SUCCESS     -> context.sendMessage(msg(L(sender, "cmd.abandon.success")));
                case NOT_FOUND   -> context.sendMessage(msg(L(sender, "cmd.abandon.not_found", "id", questIdStr)));
                case NOT_ACTIVE  -> context.sendMessage(msg(L(sender, "cmd.abandon.not_active")));
                case LIMIT_REACHED -> context.sendMessage(msg(L(sender, "cmd.abandon.limit")));
            }

            return done();
        }
    }

    // ── /quests info <id> ───────────────────────────────────────

    private class InfoSubCommand extends AbstractAsyncCommand {
        InfoSubCommand() { super("info", "Show quest info"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.use")) return done();

            String questIdStr = parseTrailingArg(context);
            LOGGER.info("[quests info] input='{}' parsed_arg='{}'",
                    context.getInputString(), questIdStr);

            if (questIdStr == null || questIdStr.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.info.usage")));
                return done();
            }

            UUID questId = resolveQuestId(questIdStr);
            Quest quest = questId != null ? plugin.getQuestTracker().getQuest(questId) : null;
            if (quest == null) {
                context.sendMessage(msg(L(sender, "cmd.info.not_found", "id", questIdStr)));
                return done();
            }

            QuestObjective obj = quest.getObjective();
            PlayerQuestData pqd = null;
            List<PlayerQuestData> playerQuests = plugin.getQuestTracker().getPlayerQuests(sender.getUuid());
            for (PlayerQuestData d : playerQuests) {
                if (d.getQuestId().equals(questId)) { pqd = d; break; }
            }

            context.sendMessage(msg(L(sender, "cmd.info.header")));
            context.sendMessage(msg(L(sender, "cmd.info.name", "name", localizedDesc(sender, quest))));
            context.sendMessage(msg(L(sender, "cmd.info.id", "id", quest.getShortId())));
            context.sendMessage(msg(L(sender, "cmd.info.period",
                    "value", L(sender, quest.getPeriod() == QuestPeriod.WEEKLY
                            ? "quest.period.weekly" : "quest.period.daily"))));
            context.sendMessage(msg(L(sender, "cmd.info.desc", "desc", localizedDesc(sender, quest))));
            context.sendMessage(msg(L(sender, "cmd.info.objective",
                    "type", obj.getType().getId(),
                    "target", obj.getTarget() != null
                            ? L(sender, "target." + obj.getTarget().toLowerCase())
                            : "*",
                    "amount", String.valueOf((int) obj.getRequiredAmount()))));
            context.sendMessage(msg(L(sender, "cmd.info.reward",
                    "coins", MessageUtil.formatCoins(quest.getReward().getBaseCoins()),
                    "xp", String.valueOf(quest.getReward().getBonusXp()))));

            if (pqd != null) {
                double req = obj.getRequiredAmount();
                String bar = MessageUtil.progressBar(pqd.getCurrentProgress(), req, 15);
                context.sendMessage(msg(L(sender, "cmd.info.progress",
                        "current", String.valueOf((int) pqd.getCurrentProgress()),
                        "required", String.valueOf((int) req),
                        "bar", bar)));
                context.sendMessage(msg(L(sender, "cmd.info.status", "status", pqd.getStatus().getId())));
            }

            context.sendMessage(msg(L(sender, "cmd.info.footer")));
            return done();
        }
    }

    // ── /quests stats ───────────────────────────────────────────

    private class StatsSubCommand extends AbstractAsyncCommand {
        StatsSubCommand() { super("stats", "Show quest statistics"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.use")) return done();

            UUID uuid = sender.getUuid();
            QuestTracker tracker = plugin.getQuestTracker();

            int activeCount = tracker.getActiveQuests(uuid).size();
            int completedCount = tracker.getCompletedCount(uuid);

            context.sendMessage(msg(L(sender, "cmd.stats.header")));
            context.sendMessage(msg(L(sender, "cmd.stats.active", "count", String.valueOf(activeCount))));
            context.sendMessage(msg(L(sender, "cmd.stats.completed", "count", String.valueOf(completedCount))));
            context.sendMessage(msg(L(sender, "cmd.stats.footer")));

            return done();
        }
    }

    // ── /quests reload ──────────────────────────────────────────

    private class ReloadSubCommand extends AbstractAsyncCommand {
        ReloadSubCommand() { super("reload", "Reload configuration"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.admin.reload")) return done();

            boolean success = plugin.getConfigManager().reload();
            if (success) {
                String newLang = plugin.getConfigManager().getConfig().getGeneral().getLanguage();
                plugin.getLangManager().reload(newLang);
                context.sendMessage(msg(L(sender, "cmd.reload.success")));
                LOGGER.info("Configuration reloaded by {}", sender.getDisplayName());
            } else {
                context.sendMessage(msg(L(sender, "cmd.reload.fail")));
            }
            return done();
        }
    }

    // ── /quests lang <en|ru> ────────────────────────────────────

    private class LangSubCommand extends AbstractAsyncCommand {
        LangSubCommand() { super("lang", "Change language"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();

            String langCode = parseTrailingArg(context);
            LOGGER.info("[quests lang] input='{}' parsed_arg='{}'",
                    context.getInputString(), langCode);

            if (langCode == null || langCode.isEmpty()) {
                context.sendMessage(msg(L(sender, "cmd.lang.usage")));
                return done();
            }

            if (plugin.getLangManager().setPlayerLang(sender.getUuid(), langCode.toLowerCase())) {
                context.sendMessage(msg(L(sender, "cmd.lang.changed")));
            } else {
                context.sendMessage(msg(L(sender, "cmd.lang.invalid")));
            }
            return done();
        }
    }

    // ── /quests gui ─────────────────────────────────────────────

    private class GuiSubCommand extends AbstractAsyncCommand {
        GuiSubCommand() { super("gui", "Open quest GUI panel"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();
            if (!checkPerm(sender, context, "ecotalequests.use")) return done();

            LOGGER.info("[quests gui] sender={}", sender.getDisplayName());

            if (sender instanceof Player player) {
                Ref<EntityStore> ref = player.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> store = ref.getStore();
                    try {
                        // Store.getComponent must run on WorldThread, not ForkJoinPool.
                        // Use reflection for getExternalData().getWorld() to get the
                        // World (Executor) since stub return types differ from real API.
                        java.lang.reflect.Method getExt = store.getClass()
                                .getMethod("getExternalData");
                        Object extData = getExt.invoke(store);
                        java.lang.reflect.Method getWorld = extData.getClass()
                                .getMethod("getWorld");
                        Object worldObj = getWorld.invoke(extData);

                        if (worldObj instanceof java.util.concurrent.Executor worldExec) {
                            return CompletableFuture.runAsync(() -> {
                                try {
                                    java.lang.reflect.Method getComp = store.getClass()
                                            .getMethod("getComponent", Ref.class, ComponentType.class);
                                    Object result = getComp.invoke(store, ref,
                                            PlayerRef.getComponentType());
                                    if (result instanceof PlayerRef playerRef) {
                                        QuestGui.open(plugin, playerRef, store, sender.getUuid());
                                    }
                                } catch (NoClassDefFoundError e) {
                                    LOGGER.warn("HyUI not available: {}", e.getMessage());
                                } catch (Exception e) {
                                    LOGGER.error("[quests gui] failed on WorldThread", e);
                                }
                            }, worldExec);
                        } else {
                            LOGGER.warn("[quests gui] World is not an Executor");
                        }
                    } catch (ReflectiveOperationException e) {
                        LOGGER.error("[quests gui] reflection failed", e);
                        context.sendMessage(msg("<red>Failed to open GUI.</red>"));
                    }
                }
            }
            return done();
        }
    }

    // ── /quests help ────────────────────────────────────────────

    private class HelpSubCommand extends AbstractAsyncCommand {
        HelpSubCommand() { super("help", "Show help"); }

        @Override
        public CompletableFuture<Void> executeAsync(CommandContext context) {
            if (!context.isPlayer()) return done();
            CommandSender sender = context.sender();

            context.sendMessage(msg(L(sender, "cmd.help.header")));
            context.sendMessage(msg(L(sender, "cmd.help.active")));
            context.sendMessage(msg(L(sender, "cmd.help.available")));
            context.sendMessage(msg(L(sender, "cmd.help.accept")));
            context.sendMessage(msg(L(sender, "cmd.help.abandon")));
            context.sendMessage(msg(L(sender, "cmd.help.info")));
            context.sendMessage(msg(L(sender, "cmd.help.stats")));
            context.sendMessage(msg("<gray>/quests gui</gray> <dark_gray>— Open quest panel (requires HyUI)</dark_gray>"));
            context.sendMessage(msg(L(sender, "cmd.help.reload")));
            context.sendMessage(msg(L(sender, "cmd.help.lang")));
            context.sendMessage(msg(L(sender, "cmd.help.help")));
            context.sendMessage(msg(L(sender, "cmd.help.footer")));
            return done();
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════

    /**
     * Parses trailing argument from getInputString(), filtering out known keywords.
     * {@code "/quests accept abc123"} → {@code "abc123"}
     * {@code "/quests lang ru"} → {@code "ru"}
     * {@code "/quests accept"} → {@code null}
     */
    private String parseTrailingArg(CommandContext context) {
        try {
            String input = context.getInputString();
            LOGGER.info("[parseTrailingArg] raw getInputString()='{}'", input);
            if (input == null || input.isBlank()) return null;

            String[] parts = input.trim().split("\\s+");
            List<String> args = new ArrayList<>();
            for (String part : parts) {
                String lower = part.toLowerCase();
                // strip leading slash
                if (lower.startsWith("/")) lower = lower.substring(1);
                if (!COMMAND_KEYWORDS.contains(lower)) {
                    args.add(part);
                }
            }
            return args.isEmpty() ? null : args.get(args.size() - 1);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse trailing arg: {}", e.getMessage());
        }
        return null;
    }

    private LangManager lang() { return plugin.getLangManager(); }

    private String L(CommandSender sender, String key, String... args) {
        return lang().getForPlayer(sender.getUuid(), key, args);
    }

    private String localizedDesc(CommandSender sender, Quest quest) {
        QuestObjective obj = quest.getObjective();
        String typeKey = "quest.desc." + obj.getType().getId();
        String target = obj.getTarget();
        String targetDisplay = (target != null && !target.isEmpty())
                ? L(sender, "target." + target.toLowerCase())
                : "";
        return L(sender, typeKey,
                "amount", String.valueOf((int) obj.getRequiredAmount()),
                "target", targetDisplay);
    }

    private boolean checkPerm(CommandSender sender, CommandContext ctx, String perm) {
        if (!sender.hasPermission(perm)) {
            ctx.sendMessage(msg(L(sender, "cmd.no_permission")));
            return false;
        }
        return true;
    }

    private static CompletableFuture<Void> done() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Resolves quest by short ID (first 8 chars of UUID).
     */
    private UUID resolveQuestId(String shortId) {
        if (shortId == null) return null;

        try {
            return UUID.fromString(shortId);
        } catch (IllegalArgumentException ignored) {}

        String lower = shortId.toLowerCase();

        for (Quest q : plugin.getQuestTracker().getAvailableQuests(UUID.randomUUID(), QuestPeriod.DAILY)) {
            if (q.getQuestId().toString().toLowerCase().startsWith(lower)) return q.getQuestId();
        }
        for (Quest q : plugin.getQuestTracker().getAvailableQuests(UUID.randomUUID(), QuestPeriod.WEEKLY)) {
            if (q.getQuestId().toString().toLowerCase().startsWith(lower)) return q.getQuestId();
        }

        List<Quest> allDaily = plugin.getStorage().loadQuestPool(QuestPeriod.DAILY);
        for (Quest q : allDaily) {
            if (q.getQuestId().toString().toLowerCase().startsWith(lower)) return q.getQuestId();
        }
        List<Quest> allWeekly = plugin.getStorage().loadQuestPool(QuestPeriod.WEEKLY);
        for (Quest q : allWeekly) {
            if (q.getQuestId().toString().toLowerCase().startsWith(lower)) return q.getQuestId();
        }

        return null;
    }
}
