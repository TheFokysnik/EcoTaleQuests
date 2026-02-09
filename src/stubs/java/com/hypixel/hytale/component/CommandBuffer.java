package com.hypixel.hytale.component;

/**
 * Stub — ECS command buffer for deferred component reads/writes.
 * Real class: com.hypixel.hytale.component.CommandBuffer
 */
public class CommandBuffer<ECS_TYPE> {

    /** Get a component from the entity referenced by {@code ref}. */
    public <C> C getComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, C> type) { return null; }

    /** Get or create a component on the entity referenced by {@code ref}. */
    public <C> C ensureAndGetComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, C> type) { return null; }
}
