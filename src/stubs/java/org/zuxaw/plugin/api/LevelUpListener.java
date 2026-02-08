package org.zuxaw.plugin.api;

/**
 * Stub — Level up event listener.
 * See: https://docs.rpg-leveling.zuxaw.com/api
 */
@FunctionalInterface
public interface LevelUpListener {
    void onLevelUp(LevelUpEvent event);
}
