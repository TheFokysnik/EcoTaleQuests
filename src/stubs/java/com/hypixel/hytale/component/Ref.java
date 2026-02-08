package com.hypixel.hytale.component;

/**
 * Stub — Entity reference in the ECS system.
 */
public class Ref<ECS_TYPE> {

    public boolean isValid() { return true; }

    public void validate() {}

    public int getIndex() { return 0; }

    public Store<ECS_TYPE> getStore() { return null; }

    public Object get() { return null; }
}
