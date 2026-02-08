package com.hypixel.hytale.server.core.event.events.ecs;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

/**
 * Stub — ECS event fired when a player interacts with a block (F key).
 * Used for crop harvesting, opening containers, etc.
 */
public class UseBlockEvent extends CancellableEcsEvent {
    public Vector3i getTargetBlock() { return new Vector3i(); }
    public BlockType getBlockType() { return null; }

    /** Event phase fired BEFORE the use action is applied. */
    public static class Pre extends UseBlockEvent {}

    /** Event phase fired AFTER the use action completes. */
    public static class Post extends UseBlockEvent {}
}
