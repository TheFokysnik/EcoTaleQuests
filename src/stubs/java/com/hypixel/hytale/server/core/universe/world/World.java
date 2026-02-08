package com.hypixel.hytale.server.core.universe.world;

import java.util.concurrent.Executor;

/**
 * Stub — Hytale World.
 * Implements Executor so it can be used as the executor in CompletableFuture.runAsync().
 * Real class: com.hypixel.hytale.server.core.universe.world.World
 */
public class World implements Executor {

    public String getName() { return ""; }

    /** Execute a task on the world thread. */
    @Override
    public void execute(Runnable command) {}
}
