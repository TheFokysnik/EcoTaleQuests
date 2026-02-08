package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.model.QuestType;
import com.crystalrealm.ecotalequests.tracker.QuestTracker;
import com.crystalrealm.ecotalequests.util.MessageUtil;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Слушатель блоковых событий для квестов типов MINE_ORE, CHOP_WOOD, HARVEST_CROP.
 *
 * <p>Реагирует на {@link BreakBlockEvent} (добыча руды/дерева через LMB)
 * и {@link UseBlockEvent.Post} (сбор урожая через F-key).</p>
 */
public class BlockQuestListener {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    private final QuestTracker questTracker;

    public BlockQuestListener(@Nonnull QuestTracker questTracker) {
        this.questTracker = questTracker;
    }

    /**
     * Регистрирует слушатели блоковых событий.
     */
    public void register(@Nonnull ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(new BreakBlockQuestSystem());
        entityStoreRegistry.registerSystem(new UseBlockQuestSystem());
        LOGGER.info("BlockQuestListener registered (BreakBlock + UseBlock.Post).");
    }

    // ═══════════════════════════════════════════════════════════
    //  ECS Event Systems (inner classes)
    // ═══════════════════════════════════════════════════════════

    /**
     * ECS-система для BreakBlockEvent — добыча руды, дерева и урожая через LMB.
     */
    private class BreakBlockQuestSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

        protected BreakBlockQuestSystem() {
            super(BreakBlockEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           BreakBlockEvent event) {
            try {
                PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
                if (playerRef == null || !playerRef.isValid()) return;

                UUID playerUuid = playerRef.getUuid();
                BlockType blockType = event.getBlockType();
                if (blockType == null) return;

                String blockId = blockType.getId();
                if (blockId == null || blockId.isEmpty()) return;

                // Кешируем PlayerRef для отправки сообщений
                try {
                    MessageUtil.cachePlayerRef(playerUuid, playerRef);
                } catch (Exception ignored) {}

                String sanitized = sanitizeBlockId(blockId);
                int playerLevel = resolvePlayerLevel(playerUuid);

                // Определяем тип квеста по блоку
                if (isOre(sanitized)) {
                    questTracker.handleAction(playerUuid, QuestType.MINE_ORE, extractOreName(sanitized), 1, playerLevel);
                } else if (isWood(sanitized)) {
                    questTracker.handleAction(playerUuid, QuestType.CHOP_WOOD, extractWoodName(sanitized), 1, playerLevel);
                } else if (isCrop(sanitized)) {
                    questTracker.handleAction(playerUuid, QuestType.HARVEST_CROP, extractCropName(sanitized), 1, playerLevel);
                }
            } catch (Throwable e) {
                LOGGER.debug("Error in BreakBlockQuestSystem: {}", e.getMessage());
            }
        }
    }

    /**
     * ECS-система для UseBlockEvent.Post — сбор урожая через F-key.
     */
    private class UseBlockQuestSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {

        protected UseBlockQuestSystem() {
            super(UseBlockEvent.Post.class);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           UseBlockEvent.Post event) {
            try {
                PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
                if (playerRef == null || !playerRef.isValid()) return;

                UUID playerUuid = playerRef.getUuid();
                BlockType blockType = event.getBlockType();
                if (blockType == null) return;

                String blockId = blockType.getId();
                if (blockId == null || blockId.isEmpty()) return;

                try {
                    MessageUtil.cachePlayerRef(playerUuid, playerRef);
                } catch (Exception ignored) {}

                String sanitized = sanitizeBlockId(blockId);
                int playerLevel = resolvePlayerLevel(playerUuid);

                // UseBlock.Post — только для урожая (F-key harvest)
                if (isCrop(sanitized)) {
                    questTracker.handleAction(playerUuid, QuestType.HARVEST_CROP, extractCropName(sanitized), 1, playerLevel);
                }
            } catch (Throwable e) {
                LOGGER.debug("Error in UseBlockQuestSystem: {}", e.getMessage());
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  BLOCK IDENTIFICATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Очищает ID блока от Hytale-специфичных декораций.
     * "*Plant_Crop_Wheat_Block_Eternal_State_Definitions_StageFinal" → "plant_crop_wheat"
     */
    private static String sanitizeBlockId(String blockId) {
        String clean = blockId;
        if (clean.startsWith("*")) clean = clean.substring(1);
        clean = clean.replace("_Block_Eternal_State_Definitions", "")
                     .replace("_StageFinal", "")
                     .replace("_Stage", "")
                     .toLowerCase();
        return clean;
    }

    private static boolean isOre(String id) {
        return id.contains("ore") || id.contains("_vein");
    }

    private static boolean isWood(String id) {
        return id.contains("log") || id.contains("wood") || id.contains("trunk");
    }

    private static boolean isCrop(String id) {
        return id.contains("plant_crop_") || id.contains("crop_");
    }

    /**
     * Извлекает имя руды: "copper_ore_block" → "copper"
     */
    private static String extractOreName(String id) {
        return id.replace("_ore", "")
                 .replace("_vein", "")
                 .replace("_block", "")
                 .trim();
    }

    /**
     * Извлекает имя дерева: "oak_log_block" → "oak"
     */
    private static String extractWoodName(String id) {
        return id.replace("_log", "")
                 .replace("_wood", "")
                 .replace("_trunk", "")
                 .replace("_block", "")
                 .trim();
    }

    /**
     * Извлекает имя культуры: "plant_crop_wheat" → "wheat"
     */
    private static String extractCropName(String id) {
        return id.replace("plant_crop_", "")
                 .replace("crop_", "")
                 .replace("_block", "")
                 .trim();
    }

    private static int resolvePlayerLevel(UUID playerUuid) {
        // Через reflection пробуем получить уровень из RPG Leveling
        try {
            Class<?> rpgClass = Class.forName("org.zuxaw.plugin.api.RPGLevelingAPI");
            Object api = null;
            for (String methodName : new String[]{"get", "getInstance", "getAPI"}) {
                try {
                    java.lang.reflect.Method m = rpgClass.getMethod(methodName);
                    api = m.invoke(null);
                    if (api != null) break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (api != null) {
                java.lang.reflect.Method getLevel = api.getClass().getMethod("getPlayerLevel", UUID.class);
                Object level = getLevel.invoke(api, playerUuid);
                if (level instanceof Number n) return n.intValue();
            }
        } catch (Exception ignored) {}
        return 1;
    }
}
