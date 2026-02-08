package com.hypixel.hytale.component.system;

import com.hypixel.hytale.component.query.Query;

public interface QuerySystem<ECS_TYPE> extends ISystem<ECS_TYPE> {
    Query<ECS_TYPE> getQuery();
}
