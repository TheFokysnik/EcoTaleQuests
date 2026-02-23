package com.hypixel.hytale.server.core;

/**
 * Stub — Hytale message object used for chat/system messages.
 * Real class: com.hypixel.hytale.server.core.Message
 */
public class Message {

    private final String text;

    public Message(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /** Create a Message from a plain string. */
    public static Message of(String text) {
        return new Message(text);
    }

    /** Create a raw text Message. Real API: Message.raw(String). */
    public static Message raw(String text) {
        return new Message(text);
    }

    /** Parse a MiniMessage-formatted string. Real API: Message.parse(String). */
    public static Message parse(String text) {
        return new Message(text);
    }

    /** Create a Message from a translation key. Real API: Message.translation(String). */
    public static Message translation(String key) {
        return new Message(key);
    }

    /** Apply a hex color to this message. Real API: Message.color(String). */
    public Message color(String hexColor) {
        return this;
    }

    @Override
    public String toString() {
        return text;
    }
}
