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
            String jsonText = MiniMessageParser.toJson(text);
            Class<?> msgClass = Class.forName("com.hypixel.hytale.server.core.Message");
            Method parseMethod = msgClass.getMethod("parse", String.class);
            Object parsedMsg = parseMethod.invoke(null, jsonText);

            // Попытка: PlayerRef → getPlayer() → sendMessage(Message)
            Object player = null;
            try {
                Method getPlayer = playerRef.getClass().getMethod("getPlayer");
                player = getPlayer.invoke(playerRef);
            } catch (NoSuchMethodException ignored) {}

            if (player != null) {
                try {
                    Method sendMsg = player.getClass().getMethod("sendMessage", msgClass);
                    sendMsg.invoke(player, parsedMsg);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }

            // Fallback: PlayerRef.sendMessage(Object)
            try {
                Method sendMsg = playerRef.getClass().getMethod("sendMessage", Object.class);
                sendMsg.invoke(playerRef, parsedMsg);
            } catch (NoSuchMethodException ignored) {}

        } catch (Throwable e) {
            LOGGER.debug("trySendViaPlayerRef failed: {}", e.getMessage());
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
