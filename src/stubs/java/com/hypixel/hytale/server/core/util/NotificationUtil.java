package com.hypixel.hytale.server.core.util;

import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.PacketHandler;

/**
 * Stub — Hytale NotificationUtil for native HUD notifications.
 * Real class: com.hypixel.hytale.server.core.util.NotificationUtil
 *
 * <p>Displays a pop-up notification on the right side of the player's screen,
 * similar to the "New Item Found!" popup from the NewItemIndicator plugin.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   NotificationUtil.sendNotification(
 *       playerRef.getPacketHandler(),
 *       Message.raw("Title").color("#FFD700"),
 *       Message.raw("Description").color("#E0E0E0"),
 *       "Particles/Textures/Basic/Star.png",
 *       null,
 *       NotificationStyle.Default
 *   );
 * </pre>
 */
public class NotificationUtil {

    /**
     * Send a HUD notification to a player.
     *
     * @param packetHandler player's packet handler (from PlayerRef.getPacketHandler())
     * @param title         notification title (colored Message)
     * @param description   notification description/subtitle (colored Message)
     * @param iconPath      path to an icon texture, or null
     * @param item          optional item to display, or null
     * @param style         notification visual style
     */
    public static void sendNotification(PacketHandler packetHandler,
                                        Message title,
                                        Message description,
                                        String iconPath,
                                        ItemWithAllMetadata item,
                                        NotificationStyle style) {
        // Stub — real implementation sends a packet to the client
    }
}
