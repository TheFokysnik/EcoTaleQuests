package com.hypixel.hytale.server.core.command.system.arguments.types;

/**
 * Stub — Argument type definitions for typed command arguments.
 * Real class: com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
 */
public final class ArgTypes {

    private ArgTypes() {}

    public static final Object STRING = "STRING";
    public static final Object INTEGER = "INTEGER";
    public static final Object DOUBLE = "DOUBLE";
    public static final Object FLOAT = "FLOAT";
    public static final Object BOOLEAN = "BOOLEAN";
    public static final Object PLAYER_REF = "PLAYER_REF";
    public static final Object ITEM_ASSET = "ITEM_ASSET";
    public static final Object BLOCK_TYPE_KEY = "BLOCK_TYPE_KEY";

    @SuppressWarnings("unchecked")
    public static <T> Object forEnum(String name, Class<T> enumClass) {
        return name;
    }
}
