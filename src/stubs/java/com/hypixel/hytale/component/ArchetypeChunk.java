package com.hypixel.hytale.component;

/**
 * Stub — ArchetypeChunk stores entity data in the ECS system.
 * Methods documented at: lait-kelomins/hytale-modding-docs
 */
public class ArchetypeChunk<ECS_TYPE> {

    /** Get a component for entity at index. */
    public <T extends Component<ECS_TYPE>> T getComponent(int index, ComponentType<ECS_TYPE, T> type) {
        return null;
    }

    /** Set a component for entity at index. */
    public <T extends Component<ECS_TYPE>> void setComponent(int index, ComponentType<ECS_TYPE, T> type, T value) {}

    /** Get entity reference at index. */
    public Ref<ECS_TYPE> getReferenceTo(int index) { return null; }

    /** Get the archetype of entities in this chunk. */
    public Object getArchetype() { return null; }

    /** Number of entities in this chunk. */
    public int size() { return 0; }
}
