package com.crystalrealm.ecotalequests.provider.economy;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Universal interface for economy providers.
 *
 * <p>Implement this interface to add support for any economy plugin.
 * Built-in implementations:</p>
 * <ul>
 *   <li>{@code ecotale} — Ecotale Economy (default)</li>
 *   <li>{@code generic} — Reflection-based adapter for any economy API</li>
 * </ul>
 */
public interface EconomyProvider {

    /** Display name of this provider. */
    @Nonnull
    String getName();

    /** Whether the underlying economy plugin is loaded and available. */
    boolean isAvailable();

    /**
     * Deposits currency to a player's account.
     *
     * @param playerUuid player UUID
     * @param amount     positive amount to deposit
     * @param reason     transaction reason (for logging)
     * @return true if successful
     */
    boolean deposit(@Nonnull UUID playerUuid, double amount, @Nonnull String reason);

    /**
     * Gets the current balance of a player.
     *
     * @param playerUuid player UUID
     * @return balance, or -1 if unavailable
     */
    double getBalance(@Nonnull UUID playerUuid);
}
