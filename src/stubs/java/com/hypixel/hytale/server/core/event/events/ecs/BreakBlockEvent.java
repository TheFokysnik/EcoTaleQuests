package com.hypixel.hytale.server.core.event.events.ecs;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;

/**
 * Stub — ECS event fired when a block is fully broken.
 */
public class BreakBlockEvent extends CancellableEcsEvent {
    public Vector3i getTargetBlock() { return new Vector3i(); }
    public void setTargetBlock(Vector3i pos) {}
    public BlockType getBlockType() { return null; }
    public ItemStack getItemInHand() { return null; }
}
