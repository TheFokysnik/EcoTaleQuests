package com.crystalrealm.ecotalequests.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads {@code permissions.json} and resolves group-based permissions manually,
 * because Hytale's native {@code hasPermission()} does NOT resolve groups.
 *
 * <p>Supports wildcard matching: {@code ecotalequests.*} and {@code *}.</p>
 */
public final class PermissionHelper {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final Gson GSON = new Gson();

    private static volatile PermissionHelper instance;

    private final Map<String, List<String>> userGroups = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupPermissions = new ConcurrentHashMap<>();

    private Path permissionsFile;

    private PermissionHelper() {}

    @Nonnull
    public static PermissionHelper getInstance() {
        if (instance == null) {
            synchronized (PermissionHelper.class) {
                if (instance == null) {
                    instance = new PermissionHelper();
                }
            }
        }
        return instance;
    }

    public void init(@Nonnull Path pluginDataDir) {
        permissionsFile = findPermissionsFile(pluginDataDir);
        if (permissionsFile != null) {
            load();
        } else {
            LOGGER.warn("permissions.json not found — group permission resolution disabled");
        }
    }

    public void reload() {
        if (permissionsFile != null && Files.exists(permissionsFile)) {
            load();
        }
    }

    // ── LuckPerms API (reflection) ─────────────────────────

    private Object luckPermsUserManager;
    private boolean luckPermsAvailable = false;
    private boolean luckPermsChecked = false;

    private void initLuckPerms() {
        if (luckPermsChecked) return;
        luckPermsChecked = true;
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = providerClass.getMethod("get").invoke(null);
            luckPermsUserManager = api.getClass().getMethod("getUserManager").invoke(api);
            luckPermsAvailable = true;
            LOGGER.info("LuckPerms API detected — using it for permission resolution");
        } catch (ClassNotFoundException e) {
            LOGGER.info("LuckPerms not found — using permissions.json only");
        } catch (Exception e) {
            LOGGER.warn("LuckPerms API init failed: {} — using permissions.json", e.getMessage());
        }
    }

    @Nullable
    private Boolean checkLuckPerms(@Nonnull UUID uuid, @Nonnull String permission) {
        initLuckPerms();
        if (!luckPermsAvailable || luckPermsUserManager == null) return null;
        try {
            Object user = luckPermsUserManager.getClass()
                    .getMethod("getUser", UUID.class).invoke(luckPermsUserManager, uuid);
            if (user == null) {
                Object future = luckPermsUserManager.getClass()
                        .getMethod("loadUser", UUID.class).invoke(luckPermsUserManager, uuid);
                user = future.getClass().getMethod("join").invoke(future);
            }
            if (user == null) return null;
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object triState = permData.getClass().getMethod("checkPermission", String.class)
                    .invoke(permData, permission);
            return (Boolean) triState.getClass().getMethod("asBoolean").invoke(triState);
        } catch (Exception e) {
            LOGGER.debug("LuckPerms check failed for {}: {}", uuid, e.getMessage());
            return null;
        }
    }

    public boolean hasPermission(@Nonnull UUID uuid, @Nonnull String permission) {
        // 1. Try LuckPerms API first
        Boolean lpResult = checkLuckPerms(uuid, permission);
        if (lpResult != null) return lpResult;

        // 2. Fallback: permissions.json
        String uuidStr = uuid.toString();
        List<String> groups = userGroups.get(uuidStr);
        if (groups == null || groups.isEmpty()) {
            Set<String> defaultPerms = groupPermissions.get("Default");
            if (defaultPerms != null) {
                return matchesAny(defaultPerms, permission);
            }
            return false;
        }

        for (String group : groups) {
            Set<String> perms = groupPermissions.get(group);
            if (perms != null && matchesAny(perms, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAny(@Nonnull Set<String> grantedPerms, @Nonnull String requested) {
        if (grantedPerms.contains(requested)) return true;
        if (grantedPerms.contains("*")) return true;
        for (String granted : grantedPerms) {
            if (granted.endsWith(".*")) {
                String prefix = granted.substring(0, granted.length() - 2);
                if (requested.startsWith(prefix + ".") || requested.equals(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void load() {
        userGroups.clear();
        groupPermissions.clear();

        try (Reader reader = new InputStreamReader(
                Files.newInputStream(permissionsFile), StandardCharsets.UTF_8)) {

            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> root = GSON.fromJson(reader, mapType);

            Map<String, Object> users = (Map<String, Object>) root.get("users");
            if (users != null) {
                for (Map.Entry<String, Object> entry : users.entrySet()) {
                    String uuid = entry.getKey();
                    Map<String, Object> userData = (Map<String, Object>) entry.getValue();
                    List<String> groups = (List<String>) userData.get("groups");
                    if (groups != null) {
                        userGroups.put(uuid, new ArrayList<>(groups));
                    }
                }
            }

            Map<String, Object> groups = (Map<String, Object>) root.get("groups");
            if (groups != null) {
                for (Map.Entry<String, Object> entry : groups.entrySet()) {
                    String groupName = entry.getKey();
                    List<String> perms = (List<String>) entry.getValue();
                    if (perms != null) {
                        Set<String> permSet = new HashSet<>();
                        for (String p : perms) {
                            if (p != null && !p.startsWith("#")) {
                                permSet.add(p.trim());
                            }
                        }
                        groupPermissions.put(groupName, permSet);
                    }
                }
            }

            LOGGER.info("permissions.json loaded: {} users, {} groups",
                    userGroups.size(), groupPermissions.size());

        } catch (IOException e) {
            LOGGER.error("Failed to load permissions.json: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error parsing permissions.json: {}", e.getMessage());
        }
    }

    @Nullable
    private Path findPermissionsFile(@Nonnull Path pluginDataDir) {
        Path[] candidates = {
                Paths.get("permissions.json"),
                pluginDataDir.resolve("../permissions.json"),
                pluginDataDir.resolve("../../permissions.json"),
                pluginDataDir.getParent() != null
                        ? pluginDataDir.getParent().resolve("permissions.json")
                        : null
        };

        for (Path candidate : candidates) {
            if (candidate != null) {
                Path normalized = candidate.toAbsolutePath().normalize();
                if (Files.exists(normalized)) {
                    LOGGER.info("Found permissions.json at: {}", normalized);
                    return normalized;
                }
            }
        }

        Path current = pluginDataDir.toAbsolutePath().normalize();
        for (int i = 0; i < 5; i++) {
            Path parent = current.getParent();
            if (parent == null) break;
            Path test = parent.resolve("permissions.json");
            if (Files.exists(test)) {
                LOGGER.info("Found permissions.json at: {}", test);
                return test;
            }
            current = parent;
        }

        return null;
    }
}
