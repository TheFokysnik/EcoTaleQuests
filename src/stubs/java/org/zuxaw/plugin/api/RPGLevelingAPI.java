package org.zuxaw.plugin.api;

import java.util.UUID;

/**
 * Stub — RPG Leveling API (org.zuxaw.plugin.api).
 * Real class provided by Zuxaw:RPGLeveling plugin at runtime.
 * See: https://docs.rpg-leveling.zuxaw.com/api
 */
public class RPGLevelingAPI {

    public static boolean isAvailable() {
        return false;
    }

    public static RPGLevelingAPI get() {
        return null;
    }

    public static String getVersion() {
        throw new UnsupportedOperationException("Stub");
    }

    // ── XP Management ──

    public void addXP(UUID playerUuid, double amount) {}

    public void addXP(UUID playerUuid, double amount, XPSource source) {}

    // ── Player Level Info ──

    public int getPlayerLevel(UUID playerUuid) { return -1; }

    public double getPlayerXP(UUID playerUuid) { return 0; }

    public boolean isPlayerMaxLevel(UUID playerUuid) { return false; }

    public PlayerLevelInfo getPlayerLevelInfo(UUID playerUuid) { return null; }

    // ── Monster Level ──

    public int getMonsterLevel(Object store, Object npcRef) { return 0; }

    // ── Listeners ──

    public void registerExperienceGainedListener(ExperienceGainedListener listener) {}

    public boolean unregisterExperienceGainedListener(ExperienceGainedListener listener) { return false; }

    public boolean isExperienceGainedListenerRegistered(ExperienceGainedListener listener) { return false; }

    public int getExperienceGainedListenerCount() { return 0; }

    public void registerLevelUpListener(LevelUpListener listener) {}

    public boolean unregisterLevelUpListener(LevelUpListener listener) { return false; }
}
