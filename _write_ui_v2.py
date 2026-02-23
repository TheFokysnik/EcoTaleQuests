#!/usr/bin/env python3
"""Rewrite BOTH .ui files with native TextButton styles (like EcoTaleReforge)."""
import os

BASE = os.path.join("src", "main", "resources", "Common", "UI", "Custom", "Pages")

# ═══════════════════════════════════════════════════════════════════════════════
#  AdminPanel.ui
# ═══════════════════════════════════════════════════════════════════════════════

ADMIN_PATH = os.path.join(BASE, "CrystalRealm_EcoTaleQuests_AdminPanel.ui")
ADMIN_CONTENT = r'''$C = "../Common.ui";

// ═══════════════════════════════════════════════════════════
//  BUTTON STYLES (native TextButton — no template overhead)
// ═══════════════════════════════════════════════════════════

@SmBtnLabel = LabelStyle(
  FontSize: 12,
  RenderBold: true,
  TextColor: #c0d0e0,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@SmBtnStyle = TextButtonStyle(
  Default:  (Background: #1a2a3a, LabelStyle: @SmBtnLabel),
  Hovered:  (Background: #253a4a, LabelStyle: @SmBtnLabel),
  Pressed:  (Background: #15202e, LabelStyle: @SmBtnLabel),
  Disabled: (Background: #151e2a, LabelStyle: (...@SmBtnLabel, TextColor: #445566)),
  Sounds: $C.@ButtonSounds
);

@ToggleBtnLabel = LabelStyle(
  FontSize: 11,
  RenderBold: true,
  TextColor: #c0d0e0,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@ToggleBtnStyle = TextButtonStyle(
  Default:  (Background: #1a2535, LabelStyle: @ToggleBtnLabel),
  Hovered:  (Background: #253545, LabelStyle: @ToggleBtnLabel),
  Pressed:  (Background: #151e2a, LabelStyle: @ToggleBtnLabel),
  Disabled: (Background: #151e2a, LabelStyle: (...@ToggleBtnLabel, TextColor: #445566)),
  Sounds: $C.@ButtonSounds
);

@ActionBtnLabel = LabelStyle(
  FontSize: 12,
  RenderBold: true,
  TextColor: #eeffee,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@ActionBtnStyle = TextButtonStyle(
  Default:  (Background: #2a7a4a, LabelStyle: @ActionBtnLabel),
  Hovered:  (Background: #35a060, LabelStyle: @ActionBtnLabel),
  Pressed:  (Background: #226640, LabelStyle: @ActionBtnLabel),
  Disabled: (Background: #2a3545, LabelStyle: (...@ActionBtnLabel, TextColor: #667788)),
  Sounds: $C.@ButtonSounds
);

@DelBtnLabel = LabelStyle(
  FontSize: 12,
  RenderBold: true,
  TextColor: #ff8888,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@DelBtnStyle = TextButtonStyle(
  Default:  (Background: #3a1a1a, LabelStyle: @DelBtnLabel),
  Hovered:  (Background: #4a2525, LabelStyle: @DelBtnLabel),
  Pressed:  (Background: #2a1010, LabelStyle: @DelBtnLabel),
  Disabled: (Background: #1a1215, LabelStyle: (...@DelBtnLabel, TextColor: #664444)),
  Sounds: $C.@ButtonSounds
);

// ═══════════════════════════════════════════════════════════
//  LABEL STYLES
// ═══════════════════════════════════════════════════════════

@GoldTitle = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 18,
  TextColor: #f0c040,
  RenderBold: true,
  HorizontalAlignment: Center
);

@White = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 13,
  TextColor: #e8edf2
);

@Gray = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 11,
  TextColor: #8a95a5
);

@Value = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 13,
  TextColor: #55ff88,
  RenderBold: true
);

@SectionLabel = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 14,
  TextColor: #f0c040,
  RenderBold: true
);

@Error = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 13,
  TextColor: #ff4444,
  RenderBold: true
);

@Success = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 13,
  TextColor: #55ff88,
  RenderBold: true
);

@EditorValue = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 13,
  TextColor: #55ccff,
  RenderBold: true
);

@EditorHint = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 10,
  TextColor: #7a8595
);

// ═══════════════════════════════════════════════════════════
//  PAGE LAYOUT
// ═══════════════════════════════════════════════════════════

$C.@PageOverlay {
  LayoutMode: Middle;

  $C.@DecoratedContainer {
    Anchor: (Width: 780, Height: 640);

    #Title {
      Label #TitleLabel { Style: @GoldTitle; Text: "Admin Settings"; }
    }

    #Content {
      LayoutMode: Top;
      Padding: (Left: 12, Right: 12, Top: 6, Bottom: 6);

      // ── Banners ────────────────────────────────────────────
      Group #ErrorBanner { Anchor: (Height: 20); Visible: false;
        Label #ErrorText { Style: @Error; Text: ""; }
      }
      Group #SuccessBanner { Anchor: (Height: 20); Visible: false;
        Label #SuccessText { Style: @Success; Text: ""; }
      }

      Group {
        LayoutMode: TopScrolling;
        FlexWeight: 1;
        ScrollbarStyle: $C.@DefaultScrollbarStyle;

        // ══════════════════════════════════════════════════════
        //  SECTION: General
        // ══════════════════════════════════════════════════════
        Label #SecGeneral { Style: @SectionLabel; Anchor: (Height: 20); Text: "General"; }

        Group #S1 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S1Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S1Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S1Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }
        Group #S2 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S2Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S2Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S2Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }
        Group #S3 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S3Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S3Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S3Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }
        Group #S4 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S4Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S4Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S4Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }
        Group #S5 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S5Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S5Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S5Down { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "-"; }
          Group { Anchor: (Width: 3); }
          TextButton #S5Up   { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "+"; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Quest Limits
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 4); }
        Label #SecLimits { Style: @SectionLabel; Anchor: (Height: 20); Text: "Quest Limits"; }

        Group #S6 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S6Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S6Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S6Down { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "-"; }
          Group { Anchor: (Width: 3); }
          TextButton #S6Up   { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "+"; }
        }
        Group #S7 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S7Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S7Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S7Down { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "-"; }
          Group { Anchor: (Width: 3); }
          TextButton #S7Up   { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "+"; }
        }
        Group #S8 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S8Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S8Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S8Down { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "-"; }
          Group { Anchor: (Width: 3); }
          TextButton #S8Up   { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "+"; }
        }
        Group #S9 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S9Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S9Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S9Down { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "-"; }
          Group { Anchor: (Width: 3); }
          TextButton #S9Up   { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "+"; }
        }
        Group #S10 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S10Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S10Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S10Down { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "-"; }
          Group { Anchor: (Width: 3); }
          TextButton #S10Up   { Anchor: (Width: 28, Height: 20); Style: @SmBtnStyle; Text: "+"; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Protection
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 4); }
        Label #SecProtection { Style: @SectionLabel; Anchor: (Height: 20); Text: "Protection"; }

        Group #S11 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S11Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S11Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S11Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }
        Group #S12 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S12Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S12Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S12Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Boards
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 4); }
        Label #SecBoards { Style: @SectionLabel; Anchor: (Height: 20); Text: "Boards"; }

        Group #S13 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S13Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S13Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S13Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }
        Group #S14 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #S14Label { Style: @White; Anchor: (Width: 240); Text: ""; }
          Label #S14Value { Style: @Value; Anchor: (Width: 70); Text: ""; }
          TextButton #S14Toggle { Anchor: (Width: 80, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Actions
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 6); }
        Label #SecActions { Style: @SectionLabel; Anchor: (Height: 20); Text: "Actions"; }

        Group { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4);
          TextButton #AB1 { Anchor: (Width: 200, Height: 24); Style: @ActionBtnStyle; Text: "Reload Config"; }
          Group { Anchor: (Width: 6); }
          TextButton #AB2 { Anchor: (Width: 200, Height: 24); Style: @ActionBtnStyle; Text: "Refresh Pools"; }
          Group { Anchor: (Width: 6); }
          TextButton #AB3 { Anchor: (Width: 200, Height: 24); Style: @ActionBtnStyle; Text: "Save Data"; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Stats
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 4); }
        Label #SecStats { Style: @SectionLabel; Anchor: (Height: 20); Text: "Stats"; }
        Group { LayoutMode: Left; Anchor: (Height: 18); Padding: (Left: 4);
          Label #StatDailyLabel { Style: @Gray; Anchor: (Width: 240); Text: ""; }
          Label #StatDailyValue { Style: @Value; Text: ""; }
        }
        Group { LayoutMode: Left; Anchor: (Height: 18); Padding: (Left: 4);
          Label #StatWeeklyLabel { Style: @Gray; Anchor: (Width: 240); Text: ""; }
          Label #StatWeeklyValue { Style: @Value; Text: ""; }
        }
        Group { LayoutMode: Left; Anchor: (Height: 18); Padding: (Left: 4);
          Label #StatVersionLabel { Style: @Gray; Anchor: (Width: 240); Text: ""; }
          Label #StatVersionValue { Style: @Value; Text: ""; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Quest Editor
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 8); }
        Label #SecEditor { Style: @SectionLabel; Anchor: (Height: 20); Text: "Quest Editor"; }
        Label #EditorHint { Style: @EditorHint; Anchor: (Height: 14); Text: ""; }

        // E1: Type (cycle)
        Group #E1 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E1Label { Style: @White; Anchor: (Width: 170); Text: "Type"; }
          Label #E1Value { Style: @EditorValue; Anchor: (Width: 120); Text: ""; }
          TextButton #E1Toggle { Anchor: (Width: 100, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }

        // E2: Target (cycle)
        Group #E2 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E2Label { Style: @White; Anchor: (Width: 170); Text: "Target"; }
          Label #E2Value { Style: @EditorValue; Anchor: (Width: 120); Text: ""; }
          TextButton #E2Toggle { Anchor: (Width: 100, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }

        // E3: Amount (4 buttons)
        Group #E3 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E3Label { Style: @White; Anchor: (Width: 170); Text: "Amount"; }
          Label #E3Value { Style: @EditorValue; Anchor: (Width: 60); Text: ""; }
          TextButton #E3DownBig  { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "-10"; }
          Group { Anchor: (Width: 2); }
          TextButton #E3Down     { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "-1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E3Up       { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "+1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E3UpBig    { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "+10"; }
        }

        // E4: Period (toggle)
        Group #E4 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E4Label { Style: @White; Anchor: (Width: 170); Text: "Period"; }
          Label #E4Value { Style: @EditorValue; Anchor: (Width: 120); Text: ""; }
          TextButton #E4Toggle { Anchor: (Width: 100, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }

        // E5: Rank (cycle)
        Group #E5 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E5Label { Style: @White; Anchor: (Width: 170); Text: "Rank"; }
          Label #E5Value { Style: @EditorValue; Anchor: (Width: 120); Text: ""; }
          TextButton #E5Toggle { Anchor: (Width: 100, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }

        // E6: Coins (4 buttons)
        Group #E6 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E6Label { Style: @White; Anchor: (Width: 170); Text: "Coins"; }
          Label #E6Value { Style: @EditorValue; Anchor: (Width: 60); Text: ""; }
          TextButton #E6DownBig  { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "-100"; }
          Group { Anchor: (Width: 2); }
          TextButton #E6Down     { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "-10"; }
          Group { Anchor: (Width: 2); }
          TextButton #E6Up       { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "+10"; }
          Group { Anchor: (Width: 2); }
          TextButton #E6UpBig    { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "+100"; }
        }

        // E7: XP (4 buttons)
        Group #E7 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E7Label { Style: @White; Anchor: (Width: 170); Text: "XP"; }
          Label #E7Value { Style: @EditorValue; Anchor: (Width: 60); Text: ""; }
          TextButton #E7DownBig  { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "-50"; }
          Group { Anchor: (Width: 2); }
          TextButton #E7Down     { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "-5"; }
          Group { Anchor: (Width: 2); }
          TextButton #E7Up       { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "+5"; }
          Group { Anchor: (Width: 2); }
          TextButton #E7UpBig    { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "+50"; }
        }

        // E8: Rank Points (4 buttons)
        Group #E8 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E8Label { Style: @White; Anchor: (Width: 170); Text: "Rank Points"; }
          Label #E8Value { Style: @EditorValue; Anchor: (Width: 60); Text: ""; }
          TextButton #E8DownBig  { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "-10"; }
          Group { Anchor: (Width: 2); }
          TextButton #E8Down     { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "-1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E8Up       { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "+1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E8UpBig    { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "+10"; }
        }

        // E9: Duration (4 buttons)
        Group #E9 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E9Label { Style: @White; Anchor: (Width: 170); Text: "Duration"; }
          Label #E9Value { Style: @EditorValue; Anchor: (Width: 60); Text: ""; }
          TextButton #E9DownBig  { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "-30"; }
          Group { Anchor: (Width: 2); }
          TextButton #E9Down     { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "-5"; }
          Group { Anchor: (Width: 2); }
          TextButton #E9Up       { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "+5"; }
          Group { Anchor: (Width: 2); }
          TextButton #E9UpBig    { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "+30"; }
        }

        // E10: Access Type (cycle)
        Group #E10 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E10Label { Style: @White; Anchor: (Width: 170); Text: "Access Type"; }
          Label #E10Value { Style: @EditorValue; Anchor: (Width: 120); Text: ""; }
          TextButton #E10Toggle { Anchor: (Width: 100, Height: 20); Style: @ToggleBtnStyle; Text: ""; }
        }

        // E11: Max Slots (4 buttons)
        Group #E11 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E11Label { Style: @White; Anchor: (Width: 170); Text: "Max Slots"; }
          Label #E11Value { Style: @EditorValue; Anchor: (Width: 60); Text: ""; }
          TextButton #E11DownBig { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "-5"; }
          Group { Anchor: (Width: 2); }
          TextButton #E11Down    { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "-1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E11Up      { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "+1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E11UpBig   { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "+5"; }
        }

        // E12: Min Level (4 buttons)
        Group #E12 { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4, Right: 4);
          Label #E12Label { Style: @White; Anchor: (Width: 170); Text: "Min Level"; }
          Label #E12Value { Style: @EditorValue; Anchor: (Width: 60); Text: ""; }
          TextButton #E12DownBig { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "-5"; }
          Group { Anchor: (Width: 2); }
          TextButton #E12Down    { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "-1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E12Up      { Anchor: (Width: 30, Height: 20); Style: @SmBtnStyle; Text: "+1"; }
          Group { Anchor: (Width: 2); }
          TextButton #E12UpBig   { Anchor: (Width: 42, Height: 20); Style: @SmBtnStyle; Text: "+5"; }
        }

        // ── Editor action buttons ────────────────────────────
        Group { Anchor: (Height: 6); }
        Group { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4);
          TextButton #EBtnCreate  { Anchor: (Width: 180, Height: 24); Style: @ActionBtnStyle; Text: "Create"; }
          Group { Anchor: (Width: 4); }
          TextButton #EBtnClear   { Anchor: (Width: 100, Height: 24); Style: @ToggleBtnStyle; Text: "Clear"; }
          Group { Anchor: (Width: 4); }
          TextButton #EBtnDelete  { Anchor: (Width: 140, Height: 24); Style: @DelBtnStyle; Text: "Delete Last"; }
          Group { Anchor: (Width: 6); }
          Label #EditorStatus { Style: @Success; Anchor: (Height: 24); Text: ""; }
        }

        // ── Custom quests count ──────────────────────────────
        Group { Anchor: (Height: 4); }
        Group { LayoutMode: Left; Anchor: (Height: 18); Padding: (Left: 4);
          Label #StatCustomLabel { Style: @Gray; Anchor: (Width: 240); Text: ""; }
          Label #StatCustomValue { Style: @EditorValue; Text: ""; }
        }

        // ── Custom quest browse ──────────────────────────────
        Group { Anchor: (Height: 4); }
        Group #CQBrowse { LayoutMode: Left; Anchor: (Height: 24); Padding: (Left: 4); Visible: false;
          TextButton #CQPrev { Anchor: (Width: 60, Height: 20); Style: @SmBtnStyle; Text: "< Prev"; }
          Group { Anchor: (Width: 4); }
          Label #CQInfo { Style: @EditorValue; Anchor: (Width: 380); Text: ""; }
          Group { Anchor: (Width: 4); }
          TextButton #CQNext { Anchor: (Width: 60, Height: 20); Style: @SmBtnStyle; Text: "Next >"; }
        }
      }
    }
  }
}
'''

# ═══════════════════════════════════════════════════════════════════════════════
#  QuestPanel.ui — more compact layout
# ═══════════════════════════════════════════════════════════════════════════════

QUEST_PATH = os.path.join(BASE, "CrystalRealm_EcoTaleQuests_QuestPanel.ui")
QUEST_CONTENT = r'''$C = "../Common.ui";

// ═══════════════════════════════════════════════════════════
//  BUTTON STYLES
// ═══════════════════════════════════════════════════════════

@BtnLabel = LabelStyle(
  FontSize: 11,
  RenderBold: true,
  TextColor: #c0d0e0,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@BtnStyle = TextButtonStyle(
  Default:  (Background: #1a2a3a, LabelStyle: @BtnLabel),
  Hovered:  (Background: #253a4a, LabelStyle: @BtnLabel),
  Pressed:  (Background: #15202e, LabelStyle: @BtnLabel),
  Disabled: (Background: #151e2a, LabelStyle: (...@BtnLabel, TextColor: #445566)),
  Sounds: $C.@ButtonSounds
);

@AcceptBtnLabel = LabelStyle(
  FontSize: 11,
  RenderBold: true,
  TextColor: #eeffee,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@AcceptBtnStyle = TextButtonStyle(
  Default:  (Background: #1a4a2a, LabelStyle: @AcceptBtnLabel),
  Hovered:  (Background: #256a3a, LabelStyle: @AcceptBtnLabel),
  Pressed:  (Background: #153a20, LabelStyle: @AcceptBtnLabel),
  Disabled: (Background: #151e2a, LabelStyle: (...@AcceptBtnLabel, TextColor: #445566)),
  Sounds: $C.@ButtonSounds
);

@CancelBtnLabel = LabelStyle(
  FontSize: 11,
  RenderBold: true,
  TextColor: #ff8888,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@CancelBtnStyle = TextButtonStyle(
  Default:  (Background: #3a1a1a, LabelStyle: @CancelBtnLabel),
  Hovered:  (Background: #4a2525, LabelStyle: @CancelBtnLabel),
  Pressed:  (Background: #2a1010, LabelStyle: @CancelBtnLabel),
  Disabled: (Background: #151e2a, LabelStyle: (...@CancelBtnLabel, TextColor: #664444)),
  Sounds: $C.@ButtonSounds
);

@RankReqBtnLabel = LabelStyle(
  FontSize: 10,
  RenderBold: true,
  TextColor: #ffaa44,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@RankReqBtnStyle = TextButtonStyle(
  Default:  (Background: #2a2010, LabelStyle: @RankReqBtnLabel),
  Hovered:  (Background: #2a2010, LabelStyle: @RankReqBtnLabel),
  Pressed:  (Background: #2a2010, LabelStyle: @RankReqBtnLabel),
  Disabled: (Background: #2a2010, LabelStyle: @RankReqBtnLabel),
  Sounds: $C.@ButtonSounds
);

@TabLabel = LabelStyle(
  FontSize: 12,
  RenderBold: true,
  TextColor: #e0e8f0,
  HorizontalAlignment: Center,
  VerticalAlignment: Center
);

@TabBtnStyle = TextButtonStyle(
  Default:  (Background: #1a2535, LabelStyle: @TabLabel),
  Hovered:  (Background: #253a4a, LabelStyle: @TabLabel),
  Pressed:  (Background: #15202e, LabelStyle: @TabLabel),
  Disabled: (Background: #151e2a, LabelStyle: (...@TabLabel, TextColor: #445566)),
  Sounds: $C.@ButtonSounds
);

// ═══════════════════════════════════════════════════════════
//  LABEL STYLES
// ═══════════════════════════════════════════════════════════

@GoldTitle = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 18,
  TextColor: #f0c040,
  RenderBold: true,
  HorizontalAlignment: Center
);

@QuestName = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 12,
  TextColor: #e8edf2,
  RenderBold: true
);

@QuestDesc = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 10,
  TextColor: #8a95a5
);

@QuestReward = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 12,
  TextColor: #55ff88,
  RenderBold: true
);

@QuestXP = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 10,
  TextColor: #55ccff,
  RenderBold: true
);

@RankBadge = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 11,
  TextColor: #ffaa00,
  RenderBold: true
);

@RankPoints = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 10,
  TextColor: #8a95a5
);

@RankProgress = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 9,
  TextColor: #7a8595
);

@Progress = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 11,
  TextColor: #aaffaa
);

@ProgressBar = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 10,
  TextColor: #55ff88,
  RenderBold: true
);

@Timer = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 10,
  TextColor: #ffcc44,
  RenderBold: true
);

@Period = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 9,
  TextColor: #55ccff,
  RenderBold: true
);

@CatIcon = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 12,
  TextColor: #f0c040,
  RenderBold: true,
  HorizontalAlignment: Center
);

@SmGray = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 10,
  TextColor: #7a8595
);

@Gray = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 11,
  TextColor: #8a95a5
);

@Error = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 12,
  TextColor: #ff5555,
  RenderBold: true
);

@Success = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 12,
  TextColor: #55ff88,
  RenderBold: true
);

// ═══════════════════════════════════════════════════════════
//  PAGE LAYOUT
// ═══════════════════════════════════════════════════════════

$C.@PageOverlay {
  LayoutMode: Middle;

  $C.@DecoratedContainer {
    Anchor: (Width: 740, Height: 520);

    #Title {
      Label #TitleLabel { Style: @GoldTitle; Text: "Quests"; }
    }

    #Content {
      LayoutMode: Top;
      Padding: (Left: 6, Right: 6, Top: 4, Bottom: 4);

      // -- Rank Bar -------------------------------------------------------
      Group #RankBar {
        LayoutMode: Left;
        Anchor: (Height: 22);
        Padding: (Left: 6, Right: 6);
        Label #RankBadgeLabel    { Style: @RankBadge;    Anchor: (Width: 40); Text: ""; }
        Label #RankNameLabel     { Style: @SmGray;       Anchor: (Width: 85); Text: ""; }
        Label #RankPointsLabel   { Style: @RankPoints;   Anchor: (Width: 60); Text: ""; }
        Label #RankProgressLabel { Style: @RankProgress;  FlexWeight: 1; Text: ""; }
      }

      // -- Banners --------------------------------------------------------
      Group #ErrorBanner   { Anchor: (Height: 18); Visible: false; Label #ErrorText   { Style: @Error;   Text: ""; } }
      Group #SuccessBanner { Anchor: (Height: 18); Visible: false; Label #SuccessText { Style: @Success; Text: ""; } }

      // -- Tab Bar --------------------------------------------------------
      Group {
        LayoutMode: Left;
        Anchor: (Height: 28);
        Padding: (Left: 4);

        TextButton #TabDaily  { Anchor: (Width: 120, Height: 24); Style: @TabBtnStyle; Text: "Daily"; }
        Group { Anchor: (Width: 4); }
        TextButton #TabWeekly { Anchor: (Width: 120, Height: 24); Style: @TabBtnStyle; Text: "Weekly"; }
        Group { Anchor: (Width: 4); }
        TextButton #TabActive { Anchor: (Width: 120, Height: 24); Style: @TabBtnStyle; Text: "Active"; }

        Group { FlexWeight: 1; }
        Label #TabStatsLabel { Style: @SmGray; Anchor: (Width: 160); Text: ""; }
      }

      // === DAILY TAB =====================================================
      Group #DailyContent {
        FlexWeight: 1;
        LayoutMode: TopScrolling;
        ScrollbarStyle: $C.@DefaultScrollbarStyle;

        Label #NoDailyMsg { Style: @Gray; Text: ""; Visible: false; Anchor: (Height: 24); }
''' + ''.join([f'''
        Group #D{i} {{ Visible: false; LayoutMode: Top; Anchor: (Height: 42); Padding: (Left: 4, Right: 4, Top: 1, Bottom: 1);
          Group {{ LayoutMode: Left; Anchor: (Height: 18);
            Label #D{i}Cat    {{ Style: @CatIcon;    Anchor: (Width: 34); Text: ""; }}
            Label #D{i}Rank   {{ Style: @RankBadge;  Anchor: (Width: 28); Text: ""; }}
            Label #D{i}Name   {{ Style: @QuestName;  FlexWeight: 1; Text: ""; }}
            Label #D{i}XP     {{ Style: @QuestXP;    Anchor: (Width: 55); Text: ""; }}
            Label #D{i}Reward {{ Style: @QuestReward; Anchor: (Width: 70); Text: ""; }}
          }}
          Group {{ LayoutMode: Left; Anchor: (Height: 20);
            Label #D{i}Desc   {{ Style: @QuestDesc;  FlexWeight: 1; Text: ""; }}
            TextButton #D{i}Btn {{ Anchor: (Width: 130, Height: 18); Style: @AcceptBtnStyle; Text: ""; }}
          }}
        }}
''' for i in range(1, 11)]) + r'''      }

      // === WEEKLY TAB ====================================================
      Group #WeeklyContent {
        FlexWeight: 1;
        LayoutMode: TopScrolling;
        Visible: false;
        ScrollbarStyle: $C.@DefaultScrollbarStyle;

        Label #NoWeeklyMsg { Style: @Gray; Text: ""; Visible: false; Anchor: (Height: 24); }
''' + ''.join([f'''
        Group #W{i} {{ Visible: false; LayoutMode: Top; Anchor: (Height: 42); Padding: (Left: 4, Right: 4, Top: 1, Bottom: 1);
          Group {{ LayoutMode: Left; Anchor: (Height: 18);
            Label #W{i}Cat    {{ Style: @CatIcon;    Anchor: (Width: 34); Text: ""; }}
            Label #W{i}Rank   {{ Style: @RankBadge;  Anchor: (Width: 28); Text: ""; }}
            Label #W{i}Name   {{ Style: @QuestName;  FlexWeight: 1; Text: ""; }}
            Label #W{i}XP     {{ Style: @QuestXP;    Anchor: (Width: 55); Text: ""; }}
            Label #W{i}Reward {{ Style: @QuestReward; Anchor: (Width: 70); Text: ""; }}
          }}
          Group {{ LayoutMode: Left; Anchor: (Height: 20);
            Label #W{i}Desc   {{ Style: @QuestDesc;  FlexWeight: 1; Text: ""; }}
            TextButton #W{i}Btn {{ Anchor: (Width: 130, Height: 18); Style: @AcceptBtnStyle; Text: ""; }}
          }}
        }}
''' for i in range(1, 6)]) + r'''      }

      // === ACTIVE TAB ====================================================
      Group #ActiveContent {
        FlexWeight: 1;
        LayoutMode: TopScrolling;
        Visible: false;
        ScrollbarStyle: $C.@DefaultScrollbarStyle;

        Label #NoActiveMsg { Style: @Gray; Text: ""; Visible: false; Anchor: (Height: 24); }
''' + ''.join([f'''
        Group #A{i} {{ Visible: false; LayoutMode: Top; Anchor: (Height: 56); Padding: (Left: 4, Right: 4, Top: 1, Bottom: 1);
          Group {{ LayoutMode: Left; Anchor: (Height: 18);
            Label #A{i}Cat     {{ Style: @CatIcon;    Anchor: (Width: 34); Text: ""; }}
            Label #A{i}Period  {{ Style: @Period;     Anchor: (Width: 22); Text: ""; }}
            Label #A{i}Name    {{ Style: @QuestName;  FlexWeight: 1; Text: ""; }}
            Label #A{i}Reward  {{ Style: @QuestReward; Anchor: (Width: 70); Text: ""; }}
          }}
          Group {{ LayoutMode: Left; Anchor: (Height: 18);
            Label #A{i}ProgBar  {{ Style: @ProgressBar; Anchor: (Width: 260); Text: ""; }}
            Label #A{i}Timer    {{ Style: @Timer;       Anchor: (Width: 80); Text: ""; }}
            Group {{ FlexWeight: 1; }}
            TextButton #A{i}Btn {{ Anchor: (Width: 120, Height: 18); Style: @CancelBtnStyle; Text: ""; }}
          }}
          Group {{ LayoutMode: Left; Anchor: (Height: 16);
            Label #A{i}Progress {{ Style: @Progress; FlexWeight: 1; Text: ""; }}
          }}
        }}
''' for i in range(1, 7)]) + r'''      }
    }
  }
}
'''

# Write both files
for path, content in [(ADMIN_PATH, ADMIN_CONTENT), (QUEST_PATH, QUEST_CONTENT)]:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Written {os.path.getsize(path)} bytes to {path}")
