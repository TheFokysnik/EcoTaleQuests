package com.crystalrealm.ecotalequests.provider.leveling;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Universal interface for leveling / XP providers.
 *
 * <p>Implement this interface to add support for any leveling plugin.
 * Built-in implementations:</p>
 * <ul>
 *   <li>{@code rpgleveling} — RPG Leveling by zuxaw (default)</li>
 *   <li>{@code endlessleveling} — EndlessLeveling by Airijko</li>
 *   <li>{@code mmoskilltree} — MMOSkillTree by Ziggfreed</li>
 *   <li>{@code generic} — Reflection-based adapter for any XP/leveling API</li>
 * </ul>
 */
public interface LevelProvider {

    /** Display name of this provider. */
    @Nonnull
    String getName();

    /** Whether the underlying leveling plugin is loaded and available. */
    boolean isAvailable();

    /**
     * Gets the current level of a player.
     *
     * @param playerUuid player UUID
     * @return player level, or 1 if unavailable
     */
    int getPlayerLevel(@Nonnull UUID playerUuid);

    /**
     * Grants XP to a player.
     *
     * @param playerUuid player UUID
     * @param amount     XP amount
     * @param reason     reason for the XP grant (for logging)
     * @return true if successful
     */
    boolean grantXP(@Nonnull UUID playerUuid, double amount, @Nonnull String reason);

    /**
     * Called when a player joins. Providers that need ECS context (Store/Ref)
     * can cache it here.
     */
    default void onPlayerJoin(@Nonnull UUID uuid, @Nonnull Object store, @Nonnull Object ref) {}

    /** Called when a player leaves. */
    default void onPlayerLeave(@Nonnull UUID uuid) {}
}
