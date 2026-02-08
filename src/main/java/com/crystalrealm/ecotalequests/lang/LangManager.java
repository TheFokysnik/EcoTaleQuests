package com.crystalrealm.ecotalequests.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.crystalrealm.ecotalequests.util.PluginLogger;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер локализации с поддержкой RU/EN.
 * Загружает из ресурсов JAR + пользовательские переопределения.
 */
public class LangManager {

    private static final PluginLogger LOGGER = PluginLogger.forEnclosingClass();
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    public static final List<String> SUPPORTED_LANGS = List.of("en", "ru");
    public static final String DEFAULT_LANG = "ru";

    private final Map<String, Map<String, String>> translations = new HashMap<>();
    private final Map<UUID, String> playerLangs = new ConcurrentHashMap<>();
    private String serverLang;
    private final Path dataDirectory;

    public LangManager(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.serverLang = DEFAULT_LANG;
    }

    public void load(@Nonnull String defaultLang) {
        this.serverLang = SUPPORTED_LANGS.contains(defaultLang) ? defaultLang : DEFAULT_LANG;

        for (String lang : SUPPORTED_LANGS) {
            Map<String, String> messages = loadLangFile(lang);
            if (messages != null && !messages.isEmpty()) {
                translations.put(lang, messages);
                LOGGER.info("Loaded {} messages for locale '{}'.", messages.size(), lang);
            }
        }
        loadCustomOverrides();
        LOGGER.info("LangManager initialized. Server language: '{}'", serverLang);
    }

    public void reload(@Nonnull String defaultLang) {
        translations.clear();
        load(defaultLang);
    }

    // ── Message Retrieval ───────────────────────────────────────

    @Nonnull
    public String get(@Nonnull String key, @Nonnull String... args) {
        return getForLang(serverLang, key, args);
    }

    @Nonnull
    public String getForLang(@Nonnull String langCode, @Nonnull String key, @Nonnull String... args) {
        String message = getRaw(langCode, key);
        if (args.length >= 2) {
            for (int i = 0; i < args.length - 1; i += 2) {
                message = message.replace("{" + args[i] + "}", args[i + 1]);
            }
        }
        return message;
    }

    @Nonnull
    public String getForPlayer(@Nonnull UUID playerUuid, @Nonnull String key, @Nonnull String... args) {
        String lang = playerLangs.getOrDefault(playerUuid, serverLang);
        return getForLang(lang, key, args);
    }

    @Nonnull
    public String getPrefixForPlayer(@Nonnull UUID playerUuid) {
        String lang = playerLangs.getOrDefault(playerUuid, serverLang);
        return getRaw(lang, "prefix");
    }

    @Nonnull
    private String getRaw(@Nonnull String langCode, @Nonnull String key) {
        Map<String, String> messages = translations.get(langCode);
        if (messages != null) {
            String value = messages.get(key);
            if (value != null) return value;
        }
        if (!"en".equals(langCode)) {
            Map<String, String> en = translations.get("en");
            if (en != null) {
                String value = en.get(key);
                if (value != null) return value;
            }
        }
        return key;
    }

    // ── Player Language ─────────────────────────────────────────

    public boolean setPlayerLang(@Nonnull UUID playerUuid, @Nonnull String langCode) {
        if (!SUPPORTED_LANGS.contains(langCode.toLowerCase())) return false;
        playerLangs.put(playerUuid, langCode.toLowerCase());
        return true;
    }

    @Nonnull
    public String getPlayerLang(@Nonnull UUID playerUuid) {
        return playerLangs.getOrDefault(playerUuid, serverLang);
    }

    public void clearPlayerData() { playerLangs.clear(); }

    @Nonnull public String getServerLang() { return serverLang; }

    // ── Loading ─────────────────────────────────────────────────

    private Map<String, String> loadLangFile(String langCode) {
        String resourcePath = "lang/" + langCode + ".json";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return Collections.emptyMap();
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, MAP_TYPE);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load language file: " + resourcePath, e);
            return Collections.emptyMap();
        }
    }

    private void loadCustomOverrides() {
        Path langDir = dataDirectory.resolve("lang");
        if (!Files.isDirectory(langDir)) return;

        for (String lang : SUPPORTED_LANGS) {
            Path customFile = langDir.resolve(lang + ".json");
            if (Files.exists(customFile)) {
                try (Reader reader = Files.newBufferedReader(customFile, StandardCharsets.UTF_8)) {
                    Map<String, String> overrides = GSON.fromJson(reader, MAP_TYPE);
                    if (overrides != null) {
                        translations.computeIfAbsent(lang, k -> new HashMap<>()).putAll(overrides);
                        LOGGER.info("Applied {} custom overrides for '{}'.", overrides.size(), lang);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load custom lang file: " + customFile, e);
                }
            }
        }
    }
}
