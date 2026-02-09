package com.hypixel.hytale.component.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;

/**
 * Stub — ECS system that reacts to a component being added to an entity.
 * Real class: com.hypixel.hytale.component.system.RefChangeSystem
 *
 * <p>When a component of type {@code C} is added to an entity,
 * {@link #onComponentAdded} is called with the entity ref and the new component.</p>
 *
 * @param <ECS_TYPE> the entity store type
 * @param <C>        the component type being observed
 */
public abstract class RefChangeSystem<ECS_TYPE, C extends Component<ECS_TYPE>>
        implements ISystem<ECS_TYPE>, QuerySystem<ECS_TYPE> {

    /**
     * Query that determines which entities trigger this system.
     */
    @Override
    public abstract Query<ECS_TYPE> getQuery();

    /**
     * Called when a component of type C is added to the entity.
     *
     * @param ref           reference to the entity
     * @param component     the added component
     * @param store         entity store
     * @param commandBuffer command buffer for component access
     */
    public abstract void onComponentAdded(
            Ref<ECS_TYPE> ref,
            C component,
            Store<ECS_TYPE> store,
            CommandBuffer<ECS_TYPE> commandBuffer
    );
}
