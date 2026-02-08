package com.hypixel.hytale.server.core.command.system.basecommands;

import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.concurrent.CompletableFuture;

/**
 * Stub — A command collection that groups sub-commands.
 * Real class: com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
 *
 * <p>Extends AbstractAsyncCommand; at runtime the real server handles
 * sub-command dispatch via addSubCommand(AbstractCommand).</p>
 */
public abstract class AbstractCommandCollection extends AbstractAsyncCommand {

    public AbstractCommandCollection(String name, String description) {
        super(name, description);
    }

    /**
     * Final — sub-command dispatch is handled by the server at runtime.
     * Cannot be overridden by plugins.
     */
    protected final CompletableFuture<Void> executeAsync(CommandContext context) {
        return CompletableFuture.completedFuture(null);
    }
}
