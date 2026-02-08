package com.hypixel.hytale.server.core.logging;

/**
 * Stub — Hytale logger API.
 */
public class HytaleLogger {

    public static HytaleLogger forEnclosingClass() {
        return new HytaleLogger();
    }

    public void info(String msg, Object... args) {}
    public void warn(String msg, Object... args) {}
    public void error(String msg, Object... args) {}
    public void debug(String msg, Object... args) {}
}
