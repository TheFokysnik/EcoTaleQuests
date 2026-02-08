package com.crystalrealm.ecotalequests.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data class for Quest GUI events — decoded from UI event bindings.
 *
 * <p>Uses TravelAnchors-style BuilderCodec pattern:
 * {@code .builder(Class, Supplier).append(keyed, setter, getter).add().build()}</p>
 */
public class QuestGuiData {

    /** Codec for serialization/deserialization of QuestGuiData. */
    public static final BuilderCodec<QuestGuiData> CODEC = BuilderCodec
            .builder(QuestGuiData.class, QuestGuiData::new)
            .append(
                    new KeyedCodec<String>("@Action", Codec.STRING),
                    (data, val, extra) -> data.action = val,
                    (data, extra) -> data.action
            ).add()
            .append(
                    new KeyedCodec<String>("@QuestId", Codec.STRING),
                    (data, val, extra) -> data.questId = val,
                    (data, extra) -> data.questId
            ).add()
            .append(
                    new KeyedCodec<String>("@Tab", Codec.STRING),
                    (data, val, extra) -> data.tab = val,
                    (data, extra) -> data.tab
            ).add()
            .build();

    private String action = "";
    private String questId = "";
    private String tab = "";

    public QuestGuiData() {}

    // ── Getters / Setters ───────────────────────────────────

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }

    public String getTab() { return tab; }
    public void setTab(String tab) { this.tab = tab; }

    @Override
    public String toString() {
        return "QuestGuiData{action=" + action + ", questId=" + questId + ", tab=" + tab + "}";
    }
}
