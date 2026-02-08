package org.zuxaw.plugin.api;

import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Stub — XP gain event context.
 * See: https://docs.rpg-leveling.zuxaw.com/api
 */
public class ExperienceGainedEvent {

    public PlayerRef getPlayer() { return null; }

    public double getXpAmount() { return 0; }

    public void setXpAmount(double amount) {}

    public XPSource getSource() { return null; }

    public boolean isCancelled() { return false; }

    public void setCancelled(boolean cancelled) {}

    public Object getSourceContext() { return null; }

    public EntityKillContext getEntityKillContext() { return null; }
}
