package com.hypixel.hytale.server.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Stub — HytaleServer main class.
 * Real server provides a static SCHEDULED_EXECUTOR for task scheduling.
 */
public class HytaleServer {

    /**
     * Global scheduled executor service (single-threaded daemon, name "Scheduler").
     * Used for repeating tasks, delayed tasks, cleanup timers, etc.
     */
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor();
}
