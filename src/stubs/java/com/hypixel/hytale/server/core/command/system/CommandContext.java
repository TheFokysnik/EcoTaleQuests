package com.hypixel.hytale.server.core.command.system;

import com.hypixel.hytale.server.core.Message;

/**
 * Stub — Command execution context.
 * Real class: com.hypixel.hytale.server.core.command.system.CommandContext
 *
 * <p>Real API methods: sender(), isPlayer(), sendMessage(Message), get(Arg), provided(Arg).</p>
 * <p>Does NOT have: senderAs(Class), getArgument(name, class).</p>
 */
public class CommandContext {

    /** Returns the command sender. */
    public CommandSender sender() {
        throw new UnsupportedOperationException("Stub");
    }

    /** Whether the sender is a player. */
    public boolean isPlayer() {
        return false;
    }

    /** Send a Message to the command sender. */
    public void sendMessage(Message message) {}

    /** Get the raw input string of the command. */
    public String getInputString() {
        return "";
    }

    /**
     * Get typed argument value.
     * Real API: ctx.get(argObject)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Object arg) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Check if optional argument was provided.
     */
    public boolean provided(Object arg) {
        throw new UnsupportedOperationException("Stub");
    }
}
