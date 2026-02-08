package com.hypixel.hytale.server.core.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

/**
 * Stub — Real TaskRegistry from com.hypixel.hytale.server.core.task.
 */
public class TaskRegistry {

    /** Register a one-shot async task. */
    public Object registerTask(CompletableFuture<Void> future) {
        return null;
    }

    /** Register a scheduled (repeating) task. */
    public Object registerTask(ScheduledFuture<Void> future) {
        return null;
    }

    /**
     * Convenience method for repeating tasks.
     * TODO: replace with real TaskRegistry API (registerTask + ScheduledFuture)
     * once the plugin's start() is refactored.
     */
    public void scheduleRepeating(Runnable task, long initialDelay, long period) {}
}
