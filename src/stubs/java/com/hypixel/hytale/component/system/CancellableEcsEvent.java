package com.hypixel.hytale.component.system;

public abstract class CancellableEcsEvent extends EcsEvent implements ICancellableEcsEvent {
    private boolean cancelled;
    public final boolean isCancelled() { return cancelled; }
    public final void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
