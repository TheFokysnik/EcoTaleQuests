package com.hypixel.hytale.component.system;

public abstract class EventSystem<EventType extends EcsEvent> {
    private final Class<EventType> eventType;
    protected EventSystem(Class<EventType> eventType) { this.eventType = eventType; }
    public Class<EventType> getEventType() { return eventType; }
}
