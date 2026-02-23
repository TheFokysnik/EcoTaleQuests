package com.crystalrealm.ecotalequests.util;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Утилита для форматирования и отправки сообщений/уведомлений игрокам.
 *
 * <p>Цепочка доставки квестовых уведомлений:</p>
 * <ol>
 *   <li>{@code NotificationUtil.sendNotification()} — нативный HUD popup (приоритет)</li>
 *   <li>{@code Player.sendMessage(Message)} — fallback в чат</li>
 * </ol>
 */
public final class MessageUtil {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private static final DecimalFormat COIN_FORMAT;
    private static final DecimalFormat COIN_FORMAT_ROUNDED;
    private static final Map<UUID, Object> PLAYER_REF_CACHE = new ConcurrentHashMap<>();

    // ── Reflection cache for NotificationUtil ───────────────────
    private static volatile boolean notificationApiChecked = false;
    private static volatile boolean notificationApiAvailable = false;
    private static Method notificationSendMethod;
    private static Method messageRawMethod;
    private static Method messageColorMethod;
    private static Class<?> notificationStyleClass;
    private static Object notificationStyleDefault;
    private static Method getPacketHandlerMethod;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        COIN_FORMAT = new DecimalFormat("#,##0.##", symbols);
        COIN_FORMAT_ROUNDED = new DecimalFormat("#,##0", symbols);
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

    // ── NotificationUtil API Detection ──────────────────────────

    private static synchronized void checkNotificationApi() {
        if (notificationApiChecked) return;
        notificationApiChecked = true;

        try {
            Class<?> notifUtilClass = Class.forName("com.hypixel.hytale.server.core.util.NotificationUtil");
            Class<?> messageClass = Class.forName("com.hypixel.hytale.server.core.Message");
            Class<?> packetHandlerClass = Class.forName("com.hypixel.hytale.server.core.io.PacketHandler");
            notificationStyleClass = Class.forName("com.hypixel.hytale.protocol.packets.interface_.NotificationStyle");
            Class<?> itemMetaClass = Class.forName("com.hypixel.hytale.protocol.ItemWithAllMetadata");

            notificationStyleDefault = notificationStyleClass.getField("Default").get(null);

            notificationSendMethod = notifUtilClass.getMethod("sendNotification",
                    packetHandlerClass, messageClass, messageClass,
                    String.class, itemMetaClass, notificationStyleClass);

            messageRawMethod = messageClass.getMethod("raw", String.class);
            messageColorMethod = messageClass.getMethod("color", String.class);

            Class<?> playerRefClass = Class.forName("com.hypixel.hytale.server.core.universe.PlayerRef");
            getPacketHandlerMethod = playerRefClass.getMethod("getPacketHandler");

            notificationApiAvailable = true;
            LOGGER.info("Native HUD notification API (NotificationUtil) detected and ready.");

        } catch (ClassNotFoundException e) {
            LOGGER.info("NotificationUtil not available (class not found: {}). Using chat fallback.", e.getMessage());
        } catch (NoSuchMethodException e) {
            LOGGER.info("NotificationUtil API signature mismatch: {}. Using chat fallback.", e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize NotificationUtil API: {}. Using chat fallback.", e.getMessage());
        }
    }

    // ── Quest HUD Notifications ─────────────────────────────────

    /**
     * Отправляет квестовое HUD-уведомление (popup справа на экране).
     * При недоступности NotificationUtil — fallback в чат через chatFallback.
     *
     * @param playerUuid   UUID игрока
     * @param title        заголовок (plain text, без MiniMessage)
     * @param description  описание (plain text, без MiniMessage)
     * @param titleColor   HEX-цвет заголовка (напр. "#55FF88")
     * @param descColor    HEX-цвет описания (напр. "#B0B0B0")
     * @param iconPath     путь к иконке текстуры, или null
     * @param chatFallback MiniMessage-строка для чат-fallback (может быть null)
     */
    public static void sendQuestNotification(UUID playerUuid,
                                             String title,
                                             String description,
                                             String titleColor,
                                             String descColor,
                                             String iconPath,
                                             String chatFallback) {
        checkNotificationApi();

        if (notificationApiAvailable) {
            boolean sent = trySendNativeNotification(playerUuid, title, description,
                    titleColor, descColor, iconPath);
            if (sent) return;
        }

        // Fallback — в чат
        if (chatFallback != null && !chatFallback.isEmpty()) {
            sendMessage(playerUuid, chatFallback);
        } else {
            String miniMsg = colorToMini(titleColor) + title + " <dark_gray>| "
                    + colorToMini(descColor) + description;
            sendMessage(playerUuid, miniMsg);
        }
    }

    private static boolean trySendNativeNotification(UUID playerUuid,
                                                     String title,
                                                     String description,
                                                     String titleColor,
                                                     String descColor,
                                                     String iconPath) {
        try {
            Object playerRef = PLAYER_REF_CACHE.get(playerUuid);
            if (playerRef == null) return false;

            Object packetHandler = getPacketHandlerMethod.invoke(playerRef);
            if (packetHandler == null) return false;

            Object titleMsg = messageRawMethod.invoke(null, title);
            titleMsg = messageColorMethod.invoke(titleMsg, titleColor);

            Object descMsg = messageRawMethod.invoke(null, description);
            descMsg = messageColorMethod.invoke(descMsg, descColor);

            if (iconPath == null) iconPath = "Particles/Textures/Basic/Star.png";

            notificationSendMethod.invoke(null,
                    packetHandler, titleMsg, descMsg,
                    iconPath, null, notificationStyleDefault);

            LOGGER.debug("HUD quest notification sent to {}: {} | {}", playerUuid, title, description);
            return true;

        } catch (Throwable e) {
            LOGGER.debug("Native notification failed for {}: {}", playerUuid, e.getMessage());
            return false;
        }
    }

    /**
     * Конвертирует HEX-цвет в MiniMessage-тег для fallback.
     */
    private static String colorToMini(String hex) {
        if (hex == null || hex.isEmpty()) return "<white>";
        return "<" + hex + ">";
    }

    /**
     * Возвращает путь к иконке для типа квеста.
     */
    public static String getQuestTypeIcon(String category) {
        if (category == null) return "Particles/Textures/Basic/Star.png";
        return switch (category.toLowerCase()) {
            case "mob"   -> "Icons/ItemsGenerated/Weapon_Sword_Iron.png";
            case "ore"   -> "Icons/ItemsGenerated/Tool_Pickaxe_Iron.png";
            case "wood"  -> "Icons/ItemsGenerated/Tool_Hatchet_Iron.png";
            case "crop"  -> "Icons/ItemsGenerated/Plant_Fruit_Apple.png";
            case "coins" -> "Icons/ItemsGenerated/Ingredient_Bar_Iron.png";
            case "xp"    -> "Particles/Textures/Basic/Star.png";
            case "boss"  -> "Icons/ItemsGenerated/Weapon_Sword_Iron.png";
            default      -> "Particles/Textures/Basic/Star.png";
        };
    }

    // ── Formatting ──────────────────────────────────────────────

    public static String formatCoins(double amount) {
        return COIN_FORMAT.format(amount);
    }

    public static String formatCoins(double amount, boolean round) {
        return round ? COIN_FORMAT_ROUNDED.format(Math.ceil(amount)) : COIN_FORMAT.format(amount);
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
