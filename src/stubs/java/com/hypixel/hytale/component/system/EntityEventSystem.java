package com.hypixel.hytale.component.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;

public abstract class EntityEventSystem<ECS_TYPE, EventType extends EcsEvent>
        extends EventSystem<EventType>
        implements QuerySystem<ECS_TYPE> {

    protected EntityEventSystem(Class<EventType> eventType) { super(eventType); }

    public abstract void handle(int index, ArchetypeChunk<ECS_TYPE> chunk,
                                Store<ECS_TYPE> store, CommandBuffer<ECS_TYPE> commandBuffer,
                                EventType event);
}
