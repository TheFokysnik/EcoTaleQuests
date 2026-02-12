package com.crystalrealm.ecotalequests.listeners;

import com.crystalrealm.ecotalequests.EcoTaleQuestsPlugin;
import com.crystalrealm.ecotalequests.gui.PageOpenHelper;
import com.crystalrealm.ecotalequests.gui.PlayerQuestsGui;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * ECS listener that detects player interaction (F-key) with quest board blocks
 * and opens the quest GUI panel.
 *
 * <p>Listens to {@link UseBlockEvent.Pre} to intercept the interaction before
 * any default handler. Matches against known quest board block type IDs.</p>
 */
public class QuestBoardInteractionListener {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    /**
     * Known block type ID fragments that identify our quest boards.
     * We match using contains() to handle different Hytale ID formats:
     * - "EcoTale_Quest_Board" (simple)
     * - "com.crystalrealm_EcoTaleQuests:EcoTale_Quest_Board" (full qualified)
     * - "EcoTaleQuests/EcoTale_Quest_Board" (path-based)
     */
    private static final Set<String> BOARD_ID_FRAGMENTS = Set.of(
            "ecotale_quest_board",
            "EcoTale_Quest_Board"
    );

    private final EcoTaleQuestsPlugin plugin;

    public QuestBoardInteractionListener(@Nonnull EcoTaleQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register the ECS event system for quest board interaction.
     */
    public void register(@Nonnull ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        entityStoreRegistry.registerSystem(new QuestBoardUseSystem());
        LOGGER.info("QuestBoardInteractionListener registered (UseBlock.Pre for quest boards).");
    }

    /**
     * Checks if a block type ID matches any known quest board pattern.
     */
    private static boolean isQuestBoard(@Nonnull String blockId) {
        for (String fragment : BOARD_ID_FRAGMENTS) {
            if (blockId.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  ECS Event System
    // ═══════════════════════════════════════════════════════════

    /**
     * ECS system that handles UseBlockEvent.Pre for quest board blocks.
     *
     * <p>When a player presses F on a quest board, this system:
     * <ol>
     *   <li>Detects the block type as a quest board</li>
     *   <li>Gets the player's Ref and PlayerRef from the ECS chunk</li>
     *   <li>Opens the PlayerQuestsGui directly via PageOpenHelper</li>
     * </ol></p>
     */
    private class QuestBoardUseSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

        protected QuestBoardUseSystem() {
            super(UseBlockEvent.Pre.class);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           UseBlockEvent.Pre event) {
            try {
                BlockType blockType = event.getBlockType();
                if (blockType == null) return;

                String blockId = blockType.getId();
                if (blockId == null || blockId.isEmpty()) return;

                // Debug logging — helps identify exact block IDs in runtime
                LOGGER.debug("UseBlock.Pre on block: {}", blockId);

                if (!isQuestBoard(blockId)) return;

                // Check if board interaction is allowed by access mode
                if (!plugin.getConfigManager().getConfig().getBoards().isBoardAllowed()) {
                    LOGGER.debug("Board interaction blocked — QuestAccessMode is gui_only");
                    return;
                }

                // Cancel the default interaction (prevent OpenCustomUI handler)
                event.setCancelled(true);

                PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
                if (playerRef == null || !playerRef.isValid()) return;

                UUID playerUuid = playerRef.getUuid();
                Ref<EntityStore> ref = chunk.getReferenceTo(index);
                if (ref == null || !ref.isValid()) return;

                LOGGER.info("Player {} interacted with quest board at block {}", playerUuid, blockId);

                // Open the quest GUI directly — same as /quests gui
                PlayerQuestsGui page = new PlayerQuestsGui(plugin, playerRef, playerUuid);
                PageOpenHelper.openPage(ref, store, page);

            } catch (Throwable e) {
                LOGGER.error("Error in QuestBoardUseSystem: {}", e.getMessage());
            }
        }
    }
}
