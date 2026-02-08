package com.crystalrealm.ecotalequests.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts MiniMessage-formatted strings to Hytale JSON rich text.
 * Hytale's Message.parse() expects PascalCase keys and hex colors.
 */
public final class MiniMessageParser {

    private static final Map<String, String> COLOR_HEX = new HashMap<>();
    static {
        COLOR_HEX.put("black",        "#000000");
        COLOR_HEX.put("dark_blue",    "#0000aa");
        COLOR_HEX.put("dark_green",   "#00aa00");
        COLOR_HEX.put("dark_aqua",    "#00aaaa");
        COLOR_HEX.put("dark_red",     "#aa0000");
        COLOR_HEX.put("dark_purple",  "#aa00aa");
        COLOR_HEX.put("gold",         "#ffaa00");
        COLOR_HEX.put("gray",         "#aaaaaa");
        COLOR_HEX.put("dark_gray",    "#555555");
        COLOR_HEX.put("blue",         "#5555ff");
        COLOR_HEX.put("green",        "#55ff55");
        COLOR_HEX.put("aqua",         "#55ffff");
        COLOR_HEX.put("red",          "#ff5555");
        COLOR_HEX.put("light_purple", "#ff55ff");
        COLOR_HEX.put("yellow",       "#ffff55");
        COLOR_HEX.put("white",        "#ffffff");
    }

    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([a-z_]+|#[0-9a-fA-F]{6})>");

    private MiniMessageParser() {}

    public static String toJson(String input) {
        if (input == null || input.isEmpty()) return "{\"RawText\":\"\"}";
        if (!input.contains("<")) return "{\"RawText\":\"" + escapeJson(input) + "\"}";

        List<String> components = new ArrayList<>();
        String color = null;
        boolean bold = false, italic = false, underline = false;
        Matcher m = TAG_PATTERN.matcher(input);
        int lastEnd = 0;

        while (m.find()) {
            if (m.start() > lastEnd) {
                String text = input.substring(lastEnd, m.start());
                if (!text.isEmpty()) components.add(buildComponent(text, color, bold, italic, underline));
            }
            boolean closing = "/".equals(m.group(1));
            String tag = m.group(2);
            if (closing) {
                if ("bold".equals(tag)) bold = false;
                else if ("italic".equals(tag)) italic = false;
                else if ("underlined".equals(tag)) underline = false;
                else if (tag.startsWith("#")) color = null;
            } else {
                if ("bold".equals(tag)) bold = true;
                else if ("italic".equals(tag)) italic = true;
                else if ("underlined".equals(tag)) underline = true;
                else if (tag.startsWith("#")) color = tag.toLowerCase();
                else if (COLOR_HEX.containsKey(tag)) color = COLOR_HEX.get(tag);
            }
            lastEnd = m.end();
        }

        if (lastEnd < input.length()) {
            String text = input.substring(lastEnd);
            if (!text.isEmpty()) components.add(buildComponent(text, color, bold, italic, underline));
        }

        if (components.isEmpty()) return "{\"RawText\":\"\"}";
        if (components.size() == 1) return components.get(0);

        StringBuilder sb = new StringBuilder("{\"RawText\":\"\",\"Children\":[");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(components.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String stripTags(String input) {
        if (input == null) return "";
        return TAG_PATTERN.matcher(input).replaceAll("");
    }

    private static String buildComponent(String text, String hex, boolean bold, boolean italic, boolean underline) {
        StringBuilder sb = new StringBuilder("{\"RawText\":\"").append(escapeJson(text)).append("\"");
        if (hex != null) sb.append(",\"Color\":\"").append(hex).append("\"");
        if (bold) sb.append(",\"Bold\":true");
        if (italic) sb.append(",\"Italic\":true");
        if (underline) sb.append(",\"Underline\":true");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
