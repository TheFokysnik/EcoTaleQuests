#!/usr/bin/env python3
"""Rewrite AdminPanel.ui with fixed editor layout — 4 buttons per numeric field."""
import os

PATH = os.path.join("src", "main", "resources", "Common", "UI", "Custom", "Pages",
                     "CrystalRealm_EcoTaleQuests_AdminPanel.ui")

CONTENT = r'''$C = "../Common.ui";

// ═══════════════════════════════════════════════════════════
//  STYLES — Admin Panel (v1.4.0)
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
  FontSize: 12,
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
  FontSize: 11,
  TextColor: #8a95a5
);

@BtnLabel = LabelStyle(
  ...$C.@DefaultLabelStyle,
  FontSize: 12,
  TextColor: #c0c8d0,
  HorizontalAlignment: Center
);

// ═══════════════════════════════════════════════════════════
//  PAGE LAYOUT
// ═══════════════════════════════════════════════════════════

$C.@PageOverlay {
  LayoutMode: Middle;

  $C.@DecoratedContainer {
    Anchor: (Width: 800, Height: 620);

    #Title {
      Label #TitleLabel { Style: @GoldTitle; Text: "Admin Settings"; }
    }

    #Content {
      LayoutMode: Top;
      Padding: (Full: 8);

      // ── Banners ────────────────────────────────────────────
      Group #ErrorBanner { Anchor: (Height: 22); Visible: false;
        Label #ErrorText { Style: @Error; Text: ""; }
      }
      Group #SuccessBanner { Anchor: (Height: 22); Visible: false;
        Label #SuccessText { Style: @Success; Text: ""; }
      }

      Group {
        LayoutMode: TopScrolling;
        FlexWeight: 1;
        ScrollbarStyle: $C.@DefaultScrollbarStyle;

        // ══════════════════════════════════════════════════════
        //  SECTION: General
        // ══════════════════════════════════════════════════════
        Label #SecGeneral { Style: @SectionLabel; Anchor: (Height: 22); Text: "General"; }

        Group #S1 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S1Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S1Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S1Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }
        Group #S2 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S2Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S2Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S2Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }
        Group #S3 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S3Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S3Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S3Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }
        Group #S4 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S4Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S4Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S4Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }
        Group #S5 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S5Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S5Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S5Down { Anchor: (Width: 30, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 4); }
          $C.@SecondaryTextButton #S5Up { Anchor: (Width: 30, Height: 24); Text: "+"; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Quest Limits
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 6); }
        Label #SecLimits { Style: @SectionLabel; Anchor: (Height: 22); Text: "Quest Limits"; }

        Group #S6 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S6Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S6Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S6Down { Anchor: (Width: 30, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 4); }
          $C.@SecondaryTextButton #S6Up { Anchor: (Width: 30, Height: 24); Text: "+"; }
        }
        Group #S7 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S7Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S7Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S7Down { Anchor: (Width: 30, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 4); }
          $C.@SecondaryTextButton #S7Up { Anchor: (Width: 30, Height: 24); Text: "+"; }
        }
        Group #S8 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S8Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S8Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S8Down { Anchor: (Width: 30, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 4); }
          $C.@SecondaryTextButton #S8Up { Anchor: (Width: 30, Height: 24); Text: "+"; }
        }
        Group #S9 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S9Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S9Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S9Down { Anchor: (Width: 30, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 4); }
          $C.@SecondaryTextButton #S9Up { Anchor: (Width: 30, Height: 24); Text: "+"; }
        }
        Group #S10 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S10Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S10Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S10Down { Anchor: (Width: 30, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 4); }
          $C.@SecondaryTextButton #S10Up { Anchor: (Width: 30, Height: 24); Text: "+"; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Protection
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 6); }
        Label #SecProtection { Style: @SectionLabel; Anchor: (Height: 22); Text: "Protection"; }

        Group #S11 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S11Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S11Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S11Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }
        Group #S12 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S12Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S12Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S12Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Boards
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 6); }
        Label #SecBoards { Style: @SectionLabel; Anchor: (Height: 22); Text: "Boards"; }

        Group #S13 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S13Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S13Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S13Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }
        Group #S14 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #S14Label { Style: @White; Anchor: (Width: 260); Text: ""; }
          Label #S14Value { Style: @Value; Anchor: (Width: 80); Text: ""; }
          $C.@SecondaryTextButton #S14Toggle { Anchor: (Width: 180, Height: 24); Text: ""; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Actions
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 8); }
        Label #SecActions { Style: @SectionLabel; Anchor: (Height: 22); Text: "Actions"; }

        Group {
          LayoutMode: Left;
          Anchor: (Height: 34);
          Padding: (Left: 4);
          $C.@SecondaryTextButton #AB1 { Anchor: (Width: 230, Height: 28); Text: "Reload Config"; }
          Group { Anchor: (Width: 6); }
          $C.@SecondaryTextButton #AB2 { Anchor: (Width: 230, Height: 28); Text: "Refresh Pools"; }
          Group { Anchor: (Width: 6); }
          $C.@SecondaryTextButton #AB3 { Anchor: (Width: 230, Height: 28); Text: "Save Data"; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Stats
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 6); }
        Label #SecStats { Style: @SectionLabel; Anchor: (Height: 22); Text: "Stats"; }
        Group { LayoutMode: Left; Anchor: (Height: 20); Padding: (Left: 4);
          Label #StatDailyLabel { Style: @Gray; Anchor: (Width: 260); Text: ""; }
          Label #StatDailyValue { Style: @Value; Text: ""; }
        }
        Group { LayoutMode: Left; Anchor: (Height: 20); Padding: (Left: 4);
          Label #StatWeeklyLabel { Style: @Gray; Anchor: (Width: 260); Text: ""; }
          Label #StatWeeklyValue { Style: @Value; Text: ""; }
        }
        Group { LayoutMode: Left; Anchor: (Height: 20); Padding: (Left: 4);
          Label #StatVersionLabel { Style: @Gray; Anchor: (Width: 260); Text: ""; }
          Label #StatVersionValue { Style: @Value; Text: ""; }
        }

        // ══════════════════════════════════════════════════════
        //  SECTION: Quest Editor
        // ══════════════════════════════════════════════════════
        Group { Anchor: (Height: 10); }
        Label #SecEditor { Style: @SectionLabel; Anchor: (Height: 24); Text: "Quest Editor"; }
        Label #EditorHint { Style: @EditorHint; Anchor: (Height: 16); Text: ""; }

        // E1: Type (cycle)
        Group #E1 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E1Label { Style: @White; Anchor: (Width: 180); Text: "Type"; }
          Label #E1Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E1Toggle { Anchor: (Width: 140, Height: 24); Text: ""; }
        }

        // E2: Target (cycle)
        Group #E2 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E2Label { Style: @White; Anchor: (Width: 180); Text: "Target"; }
          Label #E2Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E2Toggle { Anchor: (Width: 140, Height: 24); Text: ""; }
        }

        // E3: Amount (4 buttons: --/-/+/++)
        Group #E3 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E3Label { Style: @White; Anchor: (Width: 180); Text: "Amount"; }
          Label #E3Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E3DownBig  { Anchor: (Width: 45, Height: 24); Text: "--"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E3Down     { Anchor: (Width: 35, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E3Up       { Anchor: (Width: 35, Height: 24); Text: "+"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E3UpBig    { Anchor: (Width: 45, Height: 24); Text: "++"; }
        }

        // E4: Period (toggle)
        Group #E4 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E4Label { Style: @White; Anchor: (Width: 180); Text: "Period"; }
          Label #E4Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E4Toggle { Anchor: (Width: 140, Height: 24); Text: ""; }
        }

        // E5: Rank (cycle E->D->C->B->A->S)
        Group #E5 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E5Label { Style: @White; Anchor: (Width: 180); Text: "Rank"; }
          Label #E5Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E5Toggle { Anchor: (Width: 140, Height: 24); Text: ""; }
        }

        // E6: Coins (4 buttons)
        Group #E6 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E6Label { Style: @White; Anchor: (Width: 180); Text: "Coins"; }
          Label #E6Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E6DownBig  { Anchor: (Width: 45, Height: 24); Text: "--"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E6Down     { Anchor: (Width: 35, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E6Up       { Anchor: (Width: 35, Height: 24); Text: "+"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E6UpBig    { Anchor: (Width: 45, Height: 24); Text: "++"; }
        }

        // E7: XP (4 buttons)
        Group #E7 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E7Label { Style: @White; Anchor: (Width: 180); Text: "XP"; }
          Label #E7Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E7DownBig  { Anchor: (Width: 45, Height: 24); Text: "--"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E7Down     { Anchor: (Width: 35, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E7Up       { Anchor: (Width: 35, Height: 24); Text: "+"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E7UpBig    { Anchor: (Width: 45, Height: 24); Text: "++"; }
        }

        // E8: Rank Points (4 buttons)
        Group #E8 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E8Label { Style: @White; Anchor: (Width: 180); Text: "Rank Points"; }
          Label #E8Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E8DownBig  { Anchor: (Width: 45, Height: 24); Text: "--"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E8Down     { Anchor: (Width: 35, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E8Up       { Anchor: (Width: 35, Height: 24); Text: "+"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E8UpBig    { Anchor: (Width: 45, Height: 24); Text: "++"; }
        }

        // E9: Duration (4 buttons)
        Group #E9 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E9Label { Style: @White; Anchor: (Width: 180); Text: "Duration"; }
          Label #E9Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E9DownBig  { Anchor: (Width: 45, Height: 24); Text: "--"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E9Down     { Anchor: (Width: 35, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E9Up       { Anchor: (Width: 35, Height: 24); Text: "+"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E9UpBig    { Anchor: (Width: 45, Height: 24); Text: "++"; }
        }

        // E10: Access Type (cycle)
        Group #E10 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E10Label { Style: @White; Anchor: (Width: 180); Text: "Access Type"; }
          Label #E10Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E10Toggle { Anchor: (Width: 140, Height: 24); Text: ""; }
        }

        // E11: Max Slots (4 buttons)
        Group #E11 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E11Label { Style: @White; Anchor: (Width: 180); Text: "Max Slots"; }
          Label #E11Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E11DownBig { Anchor: (Width: 45, Height: 24); Text: "--"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E11Down    { Anchor: (Width: 35, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E11Up      { Anchor: (Width: 35, Height: 24); Text: "+"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E11UpBig   { Anchor: (Width: 45, Height: 24); Text: "++"; }
        }

        // E12: Min Level (4 buttons)
        Group #E12 { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4, Right: 4);
          Label #E12Label { Style: @White; Anchor: (Width: 180); Text: "Min Level"; }
          Label #E12Value { Style: @EditorValue; Anchor: (Width: 130); Text: ""; }
          $C.@SecondaryTextButton #E12DownBig { Anchor: (Width: 45, Height: 24); Text: "--"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E12Down    { Anchor: (Width: 35, Height: 24); Text: "-"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E12Up      { Anchor: (Width: 35, Height: 24); Text: "+"; }
          Group { Anchor: (Width: 3); }
          $C.@SecondaryTextButton #E12UpBig   { Anchor: (Width: 45, Height: 24); Text: "++"; }
        }

        // ── Editor action buttons ────────────────────────────
        Group { Anchor: (Height: 6); }
        Group {
          LayoutMode: Left;
          Anchor: (Height: 34);
          Padding: (Left: 4);
          $C.@SecondaryTextButton #EBtnCreate  { Anchor: (Width: 200, Height: 28); Text: ""; }
          Group { Anchor: (Width: 6); }
          $C.@SecondaryTextButton #EBtnClear   { Anchor: (Width: 120, Height: 28); Text: ""; }
          Group { Anchor: (Width: 6); }
          $C.@SecondaryTextButton #EBtnDelete  { Anchor: (Width: 150, Height: 28); Text: ""; }
          Group { Anchor: (Width: 8); }
          Label #EditorStatus { Style: @Success; Anchor: (Height: 28); Text: ""; }
        }

        // ── Custom quests list ───────────────────────────────
        Group { Anchor: (Height: 4); }
        Group { LayoutMode: Left; Anchor: (Height: 20); Padding: (Left: 4);
          Label #StatCustomLabel { Style: @Gray; Anchor: (Width: 260); Text: ""; }
          Label #StatCustomValue { Style: @EditorValue; Text: ""; }
        }

        // Existing custom quest browse (prev/next + view)
        Group { Anchor: (Height: 4); }
        Group #CQBrowse { LayoutMode: Left; Anchor: (Height: 28); Padding: (Left: 4); Visible: false;
          $C.@SecondaryTextButton #CQPrev { Anchor: (Width: 80, Height: 24); Text: "< Prev"; }
          Group { Anchor: (Width: 4); }
          Label #CQInfo { Style: @EditorValue; Anchor: (Width: 400); Text: ""; }
          Group { Anchor: (Width: 4); }
          $C.@SecondaryTextButton #CQNext { Anchor: (Width: 80, Height: 24); Text: "Next >"; }
        }
      }
    }
  }
}
'''

os.makedirs(os.path.dirname(PATH), exist_ok=True)
with open(PATH, 'w', encoding='utf-8') as f:
    f.write(CONTENT)
print(f"Written {os.path.getsize(PATH)} bytes to {PATH}")
