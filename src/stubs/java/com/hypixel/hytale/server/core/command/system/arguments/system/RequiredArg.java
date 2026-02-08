package com.hypixel.hytale.server.core.command.system.arguments.system;

import com.hypixel.hytale.server.core.command.system.CommandContext;

/**
 * Stub — Required command argument.
 * Real class: com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
 */
public class RequiredArg<T> {

    private final String name;

    public RequiredArg(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @SuppressWarnings("unchecked")
    public T get(CommandContext ctx) {
        throw new UnsupportedOperationException("Stub");
    }
}
