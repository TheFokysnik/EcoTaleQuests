package com.hypixel.hytale.server.core.command.system;

import com.hypixel.hytale.server.core.Message;

import java.util.UUID;

/**
 * Stub — Command sender interface.
 * Real class: com.hypixel.hytale.server.core.command.system.CommandSender
 *
 * <p>Extends IMessageReceiver and PermissionHolder in the real API.</p>
 */
public interface CommandSender {

    /** Display name of the sender. */
    String getDisplayName();

    /** UUID of the sender. */
    UUID getUuid();

    /** Check if the sender has a specific permission. */
    boolean hasPermission(String permission);

    /** Send a Message to this sender. */
    void sendMessage(Message message);
}
