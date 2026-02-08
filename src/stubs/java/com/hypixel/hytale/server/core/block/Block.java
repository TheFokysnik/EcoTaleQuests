package com.hypixel.hytale.server.core.block;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Stub — Hytale block.
 */
public class Block {

    public String getType() {
        throw new UnsupportedOperationException("Stub");
    }

    public String getName() {
        throw new UnsupportedOperationException("Stub");
    }

    public String getPrefabName() {
        return "";
    }

    public boolean hasFamily(String family) {
        return false;
    }

    public boolean hasState(String state) {
        return false;
    }

    public int getStateInt(String key, int defaultValue) {
        return defaultValue;
    }

    public Set<String> getFamilyTags() {
        return Collections.emptySet();
    }

    public int getX() { return 0; }
    public int getY() { return 0; }
    public int getZ() { return 0; }

    public Map<String, Object> getBlockState() {
        return Collections.emptyMap();
    }

    public Object getState(String key) {
        return null;
    }
}
