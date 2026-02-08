package com.crystalrealm.ecotalequests.util;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Утилита для форматирования и отправки сообщений игрокам.
 */
public final class MessageUtil {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private static final DecimalFormat COIN_FORMAT;
    private static final Map<UUID, Object> PLAYER_REF_CACHE = new ConcurrentHashMap<>();

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        COIN_FORMAT = new DecimalFormat("#,##0.##", symbols);
    }

    private MessageUtil() {}

    // ── PlayerRef Cache ─────────────────────────────────────────

    public static void cachePlayerRef(UUID uuid, Object playerRef) {
        if (uuid != null && playerRef != null) {
            PLAYER_REF_CACHE.put(uuid, playerRef);
        }
    }

    public static void clearCache() {
        PLAYER_REF_CACHE.clear();
    }

    /**
     * Возвращает множество UUID всех кешированных игроков.
     */
    public static java.util.Set<UUID> getCachedPlayerUuids() {
        return PLAYER_REF_CACHE.keySet();
    }

    // ── Message Sending ─────────────────────────────────────────

    /**
     * Отправляет MiniMessage-сообщение игроку через кеш PlayerRef.
     */
    public static void sendMessage(UUID playerUuid, String miniMessage) {
        try {
            Object playerRef = PLAYER_REF_CACHE.get(playerUuid);
            if (playerRef != null) {
                trySendViaPlayerRef(playerRef, miniMessage);
            }
        } catch (Throwable e) {
            LOGGER.debug("sendMessage failed for {}: {}", playerUuid, e.getMessage());
        }
    }

    private static void trySendViaPlayerRef(Object playerRef, String text) {
        try {
            // Convert MiniMessage to Hytale JSON
            String jsonText = MiniMessageParser.toJson(text);

            // Parse JSON into Message object
            Class<?> msgClass = Class.forName("com.hypixel.hytale.server.core.Message");
            Method parseMethod = msgClass.getMethod("parse", String.class);
            Object parsedMsg = parseMethod.invoke(null, jsonText);

            // Same pattern as QuestGui.sendMsg — direct playerRef.sendMessage(Message)
            Method sendMethod = playerRef.getClass().getMethod("sendMessage", msgClass);
            sendMethod.invoke(playerRef, parsedMsg);
        } catch (Throwable e) {
            LOGGER.warn("[sendMsg] failed for {}: {}", playerRef.getClass().getSimpleName(), e.getMessage());
        }
    }

    // ── Formatting ──────────────────────────────────────────────

    public static String formatCoins(double amount) {
        return COIN_FORMAT.format(amount);
    }

    public static String formatPercent(double ratio) {
        return String.format("%.0f%%", ratio * 100);
    }

    public static String formatProgress(double current, double required) {
        if (required <= 0) return "100%";
        int pct = (int) Math.min(current / required * 100, 100);
        return pct + "%";
    }

    /**
     * Генерирует текстовый прогресс-бар: [####------] 40%
     */
    public static String progressBar(double current, double required, int length) {
        double ratio = required > 0 ? Math.min(current / required, 1.0) : 1.0;
        int filled = (int) (ratio * length);
        int empty = length - filled;
        StringBuilder sb = new StringBuilder("<gray>[");
        sb.append("<green>");
        sb.append("#".repeat(filled));
        sb.append("<dark_gray>");
        sb.append("-".repeat(empty));
        sb.append("<gray>] <white>");
        sb.append(String.format("%.0f%%", ratio * 100));
        return sb.toString();
    }
}
