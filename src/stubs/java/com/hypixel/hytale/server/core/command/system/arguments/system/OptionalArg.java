package com.hypixel.hytale.server.core.command.system.arguments.system;

import com.hypixel.hytale.server.core.command.system.CommandContext;

/**
 * Stub — Optional command argument.
 * Real class: com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
 */
public class OptionalArg<T> {

    private final String name;

    public OptionalArg(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @SuppressWarnings("unchecked")
    public T get(CommandContext ctx) {
        throw new UnsupportedOperationException("Stub");
    }

    public boolean provided(CommandContext ctx) {
        throw new UnsupportedOperationException("Stub");
    }
}
