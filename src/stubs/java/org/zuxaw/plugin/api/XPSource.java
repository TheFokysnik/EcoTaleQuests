package org.zuxaw.plugin.api;

/**
 * Stub — XP sources for RPG Leveling.
 * See: https://docs.rpg-leveling.zuxaw.com/api
 */
public class XPSource {

    public static final XPSource ENTITY_KILL = new XPSource("ENTITY_KILL");
    public static final XPSource COMMAND = new XPSource("COMMAND");

    private final String name;

    private XPSource(String name) {
        this.name = name;
    }

    public static XPSource create(String name) {
        return new XPSource(name);
    }

    public String getName() { return name; }

    public Object getMetadata() { return null; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof XPSource other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() { return name.hashCode(); }
}
