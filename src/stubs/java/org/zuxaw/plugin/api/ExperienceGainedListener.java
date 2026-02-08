package org.zuxaw.plugin.api;

/**
 * Stub — XP gain event from RPG Leveling.
 * See: https://docs.rpg-leveling.zuxaw.com/api
 */
@FunctionalInterface
public interface ExperienceGainedListener {
    void onExperienceGained(ExperienceGainedEvent event);
}
