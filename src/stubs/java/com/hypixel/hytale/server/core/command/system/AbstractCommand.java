package com.hypixel.hytale.server.core.command.system;

import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;

import java.util.concurrent.CompletableFuture;

/**
 * Stub — Base class for all Hytale commands.
 * Real class: com.hypixel.hytale.server.core.command.system.AbstractCommand
 */
public abstract class AbstractCommand {

    private final String name;
    private final String description;

    public AbstractCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** Add a sub-command to this command. */
    public void addSubCommand(AbstractCommand subCommand) {}

    /** Require a specific permission to execute this command. */
    public void requirePermission(String permission) {}

    /** Assign command to permission groups by name (e.g. "Adventure", "Creative"). */
    public void setPermissionGroups(String... groups) {}

    /** Add command aliases. */
    public void addAliases(String... aliases) {}

    /** Define a required argument for this command. */
    @SuppressWarnings("unchecked")
    public <T> RequiredArg<T> withRequiredArg(String name, String description, Object argType) {
        return new RequiredArg<>(name);
    }

    /** Define an optional argument for this command. */
    @SuppressWarnings("unchecked")
    public <T> OptionalArg<T> withOptionalArg(String name, String description, Object argType) {
        return new OptionalArg<>(name);
    }

    /** Abstract execution method. */
    public abstract CompletableFuture<Void> execute(CommandContext context);
}
