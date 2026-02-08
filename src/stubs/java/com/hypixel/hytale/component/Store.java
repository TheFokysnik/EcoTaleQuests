package com.hypixel.hytale.component;

/**
 * Stub — ECS entity store.
 */
public class Store<ECS_TYPE> {

    /**
     * Get a component from an entity reference.
     *
     * @param ref  the entity reference
     * @param type the component type
     * @return the component instance, or null
     */
    @SuppressWarnings("unchecked")
    public <C> C getComponent(Ref<ECS_TYPE> ref, ComponentType<ECS_TYPE, C> type) {
        return null;
    }
}