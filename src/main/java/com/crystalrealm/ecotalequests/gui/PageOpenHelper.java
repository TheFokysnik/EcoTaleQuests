package com.crystalrealm.ecotalequests.gui;

import com.crystalrealm.ecotalequests.util.PluginLogger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

/**
 * Utility that opens/closes InteractiveCustomUIPage through 100% reflection.
 *
 * <p>{@code getPageManager()} lives on the ECS component {@code Player},
 * <b>not</b> on {@code PlayerRef}. We obtain the Player component from the
 * entity store and invoke everything reflectively so that stub descriptor
 * mismatches never cause {@code NoSuchMethodError}.</p>
 */
public final class PageOpenHelper {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();

    /** Cached Player class (loaded once). */
    private static Class<?> PLAYER_CLASS;
    /** Cached Player.getComponentType() result. */
    private static Object PLAYER_COMP_TYPE;

    static {
        try {
            PLAYER_CLASS = Class.forName(
                    "com.hypixel.hytale.server.core.entity.entities.Player");
            Method getCompType = PLAYER_CLASS.getMethod("getComponentType");
            PLAYER_COMP_TYPE = getCompType.invoke(null);
        } catch (Exception e) {
            LOGGER.error("[PageOpenHelper] Failed to resolve Player class: {}", e.getMessage());
        }
    }

    private PageOpenHelper() {}

    /**
     * Open a custom UI page for the player associated with the given
     * {@code ref}/{@code store} pair.
     *
     * <p>Equivalent to:
     * <pre>{@code
     *   Player player = store.getComponent(ref, Player.getComponentType());
     *   player.getPageManager().openCustomPage(ref, store, page);
     * }</pre></p>
     */
    public static void openPage(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull InteractiveCustomUIPage<?> page) {
        try {
            if (PLAYER_CLASS == null || PLAYER_COMP_TYPE == null) {
                throw new IllegalStateException("Player class was not resolved during init");
            }

            // 1. store.getComponent(ref, Player.getComponentType())
            Method getComp = findMethod(store.getClass(), "getComponent", 2);
            Object player = getComp.invoke(store, ref, PLAYER_COMP_TYPE);
            if (player == null) {
                throw new IllegalStateException("Player component is null for ref " + ref);
            }

            // 2. player.getPageManager()
            Method getPm = player.getClass().getMethod("getPageManager");
            Object pm = getPm.invoke(player);
            if (pm == null) {
                throw new IllegalStateException("PageManager is null");
            }

            // 3. pm.openCustomPage(ref, store, page)
            Method openMethod = findMethod(pm.getClass(), "openCustomPage", 3);
            openMethod.invoke(pm, ref, store, page);

        } catch (Exception e) {
            LOGGER.error("[PageOpenHelper] Failed to open page: {}", e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────

    private static Method findMethod(Class<?> clazz, String name, int paramCount) {
        for (Method m : clazz.getMethods()) {
            if (name.equals(m.getName()) && m.getParameterCount() == paramCount) {
                return m;
            }
        }
        throw new IllegalStateException("Method " + name + " with " + paramCount
                + " params not found on " + clazz.getName());
    }
}
