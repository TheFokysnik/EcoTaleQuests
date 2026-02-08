package com.hypixel.hytale.server.core.command.system.basecommands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.concurrent.CompletableFuture;

/**
 * Stub — Async command base.
 * Real class: com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand
 */
public abstract class AbstractAsyncCommand extends AbstractCommand {

    public AbstractAsyncCommand(String name, String description) {
        super(name, description);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return executeAsync(context);
    }

    /** Subclasses implement async execution logic. */
    protected abstract CompletableFuture<Void> executeAsync(CommandContext context);
}
