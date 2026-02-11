# 📜 EcoTaleQuests

**Daily & Weekly quest system for Hytale servers**

Give players **daily** and **weekly** quests — kill mobs, mine ores, chop trees, harvest crops, earn currency, gain XP — with automatic generation from **46+ quest candidates**, wildcard targets, level-scaled rewards, a **native GUI panel**, an **admin settings panel**, fully localized quest names, and real-time chat progress notifications.

![Hytale Server Mod](https://img.shields.io/badge/Hytale-Server%20Mod-0ea5e9?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.2.1-10b981?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-f97316?style=for-the-badge&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-a855f7?style=for-the-badge)
![Ecotale](https://img.shields.io/badge/Ecotale-1.0.7-6366f1?style=for-the-badge)

[**Getting Started**](#-getting-started) · [**Features**](#-features) · [**Commands**](#-commands) · [**Configuration**](#-configuration) · [**Architecture**](#-architecture)

---

## ✨ Features

| Feature | Description |
|:--------|:------------|
| 📋 **Daily Quests** | Up to 10 fresh quests every day from a generated pool (4 active at once) |
| 📅 **Weekly Quests** | Up to 5 weekly quests with bigger rewards (2 active at once) |
| ⚔️ **Kill Mobs** | 11 mob types — zombie, skeleton, spider, trork, kweebec, scarrak, feran, outlander, raptor, magma golem + **any mob** wildcard |
| ⛏️ **Mine Ores** | 8 ore types — copper, iron, silver, gold, cobalt, emerald, mythril + **any ore** wildcard |
| 🪓 **Chop Wood** | 7 tree types — oak, birch, pine, willow, redwood, jungle + **any wood** wildcard |
| 🌾 **Harvest Crops** | 10 crop types — wheat, potato, carrot, berry, tomato, onion, pumpkin, corn, melon + **any crop** wildcard |
| 💰 **Earn Currency** | Meta-quests — earn a total amount of currency from any source |
| ✨ **Gain XP** | Meta-quests — gain RPG experience from any source |
| 🎯 **Wildcard Targets** | `any_mob`, `any_ore`, `any_wood`, `any_crop` — match any action of that type |
| 🖥️ **GUI Panel** | Native quest panel with Daily/Weekly tabs, accept/abandon/info buttons |
| 🛠️ **Admin Panel** | Admin settings GUI — toggle modules, reload config, save settings |
| 📊 **Localized Progress** | Fully translated quest names in chat (e.g. `[Q] Добыть: Железо: 3/22`) |
| 🎲 **Auto-Generation** | 46+ quest candidates, pools generated automatically each day/week |
| 📈 **Level Scaling** | Quest difficulty and rewards scale with player RPG level |
| 🏅 **Milestone Alerts** | Additional notifications at 25%, 50%, 75% progress |
| 🛡️ **Abuse Protection** | Accept cooldowns, world filtering, duplicate prevention, expiry enforcement |
| 💾 **JSON Storage** | Per-player quest data saved as JSON files |
| 🌍 **Localization** | RU / EN — all quest names, targets, and UI elements fully translated |
| 🔧 **Hot Reload** | `/quests reload` — no restart needed |

## 📦 Requirements

| Dependency | Version | Required | Description |
|:-----------|:--------|:--------:|:------------|
| [Ecotale](https://curseforge.com/hytale/mods/ecotale) | ≥ 1.0.7 | ✅ | Economy & currency (balance, deposit, withdraw) |
| [RPG Leveling](https://www.curseforge.com/hytale/mods/rpg-leveling-and-stats) | ≥ 0.2.0 | ❌ | XP quests, mob kills, reward scaling |

> [!TIP]
> Without RPG Leveling, all players are treated as level 1 — XP and mob kill quests won't track, but all other quest types work normally.

## 🚀 Getting Started

```bash
# 1. Copy JAR files to the server's mods/ folder
cp EcoTaleQuests-1.2.1.jar /server/mods/

# 2. Make sure Ecotale-1.0.7.jar is also in mods/
# 3. Start the server — config & lang files are created automatically
# 4. Customize quest templates if needed
nano mods/com.crystalrealm_EcoTaleQuests/EcoTaleQuests.json
```

**That's it.** Quest pools are generated on first startup. Players can immediately browse and accept quests.

## 🎮 Commands

| Command | Description | Permission |
|:--------|:------------|:-----------|
| `/quests` | Show active quests with progress | `ecotalequests.command.quests` |
| `/quests available` | Browse available quests to accept | `ecotalequests.command.quests` |
| `/quests accept <id>` | Accept a quest from the pool | `ecotalequests.command.quests` |
| `/quests abandon <id>` | Abandon an active quest | `ecotalequests.command.quests` |
| `/quests info <id>` | Detailed quest information | `ecotalequests.command.quests` |
| `/quests gui` | Open the quest GUI panel | `ecotalequests.use` |
| `/quests admin` | Open the admin settings panel | `ecotalequests.admin.settings` |
| `/quests stats` | Your quest completion statistics | `ecotalequests.use` |
| `/quests reload` | Reload config & lang files | `ecotalequests.admin.reload` |
| `/quests langen` | Switch language to English | — |
| `/quests langru` | Switch language to Russian | — |
| `/quests help` | Command reference | — |

> [!NOTE]
> Quest IDs use short 8-character identifiers (e.g., `a3f7b2c1`).

## 🖥️ GUI Panels

### Player Quest Panel (`/quests gui`)

Native GUI built on Hytale's `InteractiveCustomUIPage` API with `.ui` layouts:

- **Daily** tab — available daily quests with Accept / Abandon / Info buttons
- **Weekly** tab — available weekly quests with Accept / Abandon / Info buttons
- Progress bar and status displayed for each active quest

The panel auto-refreshes after accepting or abandoning a quest.

### Admin Settings Panel (`/quests admin`)

Admin-only panel for server configuration:

- Toggle modules on/off (mob kills, mining, woodcutting, farming, currency, XP)
- **Reload** — reload config & lang files
- **Refresh** — regenerate quest pools
- **Save** — persist current settings to disk

## 🔐 Permissions

**Base Permissions** — all players:
```yaml
ecotalequests.use              # /quests, available, accept, abandon, info, gui, stats
```

**VIP Tiers** (configurable in `VipTiers`):
```yaml
ecotalequests.multiplier.vip        # ×1.25 quest reward multiplier (VIP)
ecotalequests.multiplier.mvp        # ×1.50 quest reward multiplier (MVP)
ecotalequests.multiplier.mvp_plus   # ×2.00 quest reward multiplier (MVP+)
```

**Admin:**
```yaml
ecotalequests.admin.reload     # /quests reload
ecotalequests.admin.settings   # /quests admin (settings panel)
ecotalequests.*                # All permissions
```

## ⚙️ Configuration

Config file: `mods/com.crystalrealm_EcoTaleQuests/EcoTaleQuests.json`

### 📋 General & Limits

```json
{
  "General": {
    "DebugMode": false,
    "Language": "ru",
    "NotifyOnProgress": true,
    "NotifyOnComplete": true,
    "AutoSaveIntervalMinutes": 5
  },
  "QuestLimits": {
    "MaxDailyActive": 4,
    "MaxWeeklyActive": 2,
    "DailyPoolSize": 10,
    "WeeklyPoolSize": 5,
    "MaxAbandonPerDay": 3,
    "DailyResetTime": "00:00",
    "WeeklyResetDay": "MONDAY"
  }
}
```

| Key | Default | Description |
|:----|:--------|:------------|
| `MaxDailyActive` | 4 | Max daily quests a player can have active at once |
| `MaxWeeklyActive` | 2 | Max weekly quests active at once |
| `DailyPoolSize` | 10 | How many daily quests are generated each day |
| `WeeklyPoolSize` | 5 | How many weekly quests are generated each week |
| `MaxAbandonPerDay` | 3 | Max quest abandonments per day |
| `DailyResetTime` | 00:00 | Time when daily pool refreshes |
| `WeeklyResetDay` | MONDAY | Day of week for weekly pool refresh |

### 🎲 Quest Generation Templates

Each template defines a quest candidate with daily/weekly amount ranges and minimum player level:

```json
{
  "Generation": {
    "KillMobs": {
      "any_mob":     { "DailyMin": 10, "DailyMax": 30, "WeeklyMin": 50,  "WeeklyMax": 150, "MinLevel": 0  },
      "zombie":      { "DailyMin": 5,  "DailyMax": 15, "WeeklyMin": 30,  "WeeklyMax": 80,  "MinLevel": 0  },
      "skeleton":    { "DailyMin": 5,  "DailyMax": 15, "WeeklyMin": 25,  "WeeklyMax": 70,  "MinLevel": 0  },
      "kweebec":     { "DailyMin": 4,  "DailyMax": 12, "WeeklyMin": 20,  "WeeklyMax": 60,  "MinLevel": 3  },
      "scarrak":     { "DailyMin": 3,  "DailyMax": 8,  "WeeklyMin": 12,  "WeeklyMax": 40,  "MinLevel": 8  },
      "feran":       { "DailyMin": 3,  "DailyMax": 10, "WeeklyMin": 15,  "WeeklyMax": 50,  "MinLevel": 5  },
      "magma_golem": { "DailyMin": 1,  "DailyMax": 4,  "WeeklyMin": 5,   "WeeklyMax": 20,  "MinLevel": 20 }
    },
    "MineOres": {
      "any_ore":  { "DailyMin": 15, "DailyMax": 50, "WeeklyMin": 80,  "WeeklyMax": 250, "MinLevel": 0  },
      "copper":   { "DailyMin": 10, "DailyMax": 30, "WeeklyMin": 50,  "WeeklyMax": 150, "MinLevel": 0  },
      "iron":     { "DailyMin": 8,  "DailyMax": 25, "WeeklyMin": 40,  "WeeklyMax": 120, "MinLevel": 5  },
      "silver":   { "DailyMin": 6,  "DailyMax": 20, "WeeklyMin": 30,  "WeeklyMax": 100, "MinLevel": 8  },
      "gold":     { "DailyMin": 5,  "DailyMax": 15, "WeeklyMin": 25,  "WeeklyMax": 80,  "MinLevel": 10 },
      "emerald":  { "DailyMin": 2,  "DailyMax": 8,  "WeeklyMin": 10,  "WeeklyMax": 40,  "MinLevel": 25 },
      "mythril":  { "DailyMin": 2,  "DailyMax": 6,  "WeeklyMin": 10,  "WeeklyMax": 30,  "MinLevel": 30 }
    },
    "ChopWood": {
      "any_wood": { "DailyMin": 20, "DailyMax": 60, "WeeklyMin": 100, "WeeklyMax": 300, "MinLevel": 0  },
      "oak":      { "DailyMin": 15, "DailyMax": 40, "WeeklyMin": 80,  "WeeklyMax": 200, "MinLevel": 0  },
      "willow":   { "DailyMin": 8,  "DailyMax": 25, "WeeklyMin": 50,  "WeeklyMax": 120, "MinLevel": 5  },
      "jungle":   { "DailyMin": 4,  "DailyMax": 12, "WeeklyMin": 25,  "WeeklyMax": 70,  "MinLevel": 12 }
    },
    "HarvestCrops": {
      "any_crop": { "DailyMin": 15, "DailyMax": 40, "WeeklyMin": 80,  "WeeklyMax": 200, "MinLevel": 0  },
      "wheat":    { "DailyMin": 10, "DailyMax": 30, "WeeklyMin": 60,  "WeeklyMax": 150, "MinLevel": 0  },
      "berry":    { "DailyMin": 8,  "DailyMax": 20, "WeeklyMin": 40,  "WeeklyMax": 100, "MinLevel": 3  },
      "tomato":   { "DailyMin": 6,  "DailyMax": 18, "WeeklyMin": 35,  "WeeklyMax": 90,  "MinLevel": 5  },
      "melon":    { "DailyMin": 4,  "DailyMax": 12, "WeeklyMin": 20,  "WeeklyMax": 50,  "MinLevel": 8  }
    },
    "EarnCoins": { "DailyMin": 100, "DailyMax": 500,  "WeeklyMin": 500,  "WeeklyMax": 2500, "MinLevel": 0 },
    "GainXP":    { "DailyMin": 50,  "DailyMax": 200,  "WeeklyMin": 200,  "WeeklyMax": 1000, "MinLevel": 0 }
  }
}
```

> [!NOTE]
> **Wildcard targets** (`any_mob`, `any_ore`, `any_wood`, `any_crop`) match **any** action of that type. For example, `any_ore` counts progress when the player mines copper, iron, gold, or any other ore.

The generator picks from these templates randomly, applies level scaling to amounts, and creates quest objectives. With 46+ candidates across 6 quest types, players see varied quests every day.

### 💰 Rewards

```json
{
  "Rewards": {
    "BaseDailyCoins": 50.0,
    "BaseWeeklyCoins": 200.0,
    "BaseDailyXP": 25,
    "BaseWeeklyXP": 100,
    "LevelScalingFactor": 0.02,
    "MaxLevelMultiplier": 3.0,
    "DifficultyMultipliers": {
      "kill_mob": 1.0,
      "mine_ore": 0.8,
      "chop_wood": 0.7,
      "harvest_crop": 0.6,
      "earn_coins": 1.2,
      "gain_xp": 1.1
    }
  }
}
```

| Key | Default | Description |
|:----|:--------|:------------|
| `BaseDailyCoins` | 50.0 | Base currency reward for daily quests |
| `BaseWeeklyCoins` | 200.0 | Base currency reward for weekly quests |
| `BaseDailyXP` | 25 | Base RPG XP reward for daily quests |
| `BaseWeeklyXP` | 100 | Base RPG XP reward for weekly quests |
| `LevelScalingFactor` | 0.02 | Reward multiplier per player level |
| `MaxLevelMultiplier` | 3.0 | Maximum reward multiplier cap |
| `DifficultyMultipliers` | — | Per-quest-type reward scaling |

**Reward formula:** `Final Reward = Base × DifficultyMultiplier × min(1 + level × factor, MaxLevelMultiplier) × VipMultiplier`

### 🌟 VIP Tiers

```json
{
  "VipTiers": {
    "Tiers": [
      { "Permission": "ecotalequests.multiplier.mvp_plus", "Multiplier": 2.0, "DisplayName": "MVP+" },
      { "Permission": "ecotalequests.multiplier.mvp",      "Multiplier": 1.5, "DisplayName": "MVP" },
      { "Permission": "ecotalequests.multiplier.vip",      "Multiplier": 1.25, "DisplayName": "VIP" }
    ]
  }
}
```

| Key | Default | Description |
|:----|:--------|:------------|
| `Tiers` | 3 tiers | Ordered list of VIP tiers (first match wins — put highest tier first) |
| `Permission` | — | Permission node to check for this tier |
| `Multiplier` | — | Reward multiplier applied to quest coins |
| `DisplayName` | — | Name shown in reward message (e.g., "MVP+ ×2.00") |

> **Note:** Tiers are checked top-to-bottom; the first matching permission is used. If a player has no VIP permission, multiplier defaults to ×1.0.

### 🛡️ Protection

```json
{
  "Protection": {
    "RequireOnline": true,
    "MinPlaytimeMinutes": 5,
    "QuestAcceptCooldownMs": 1000,
    "PreventDuplicateTypes": true,
    "AllowedWorlds": []
  }
}
```

| Key | Default | Description |
|:----|:--------|:------------|
| `RequireOnline` | true | Player must be online to track progress |
| `MinPlaytimeMinutes` | 5 | Min playtime before accepting quests |
| `QuestAcceptCooldownMs` | 1000 | Cooldown between accepting quests (ms) |
| `PreventDuplicateTypes` | true | Prevent having two quests of same type active |
| `AllowedWorlds` | `[]` (all) | Restrict quest progress to specific worlds |

## 📝 Quest Lifecycle

```
┌─────────────┐    accept    ┌────────────┐   progress   ┌───────────────┐
│  AVAILABLE   │────────────▶│   ACTIVE    │─────────────▶│   COMPLETED   │
│  (in pool)   │             │ (tracking)  │   (100%)     │  (rewarded!)  │
└─────────────┘             └──────┬──────┘              └───────────────┘
                                   │
                          abandon  │  expire
                                   ▼
                            ┌──────────────┐
                            │  ABANDONED /  │
                            │   EXPIRED     │
                            └──────────────┘
```

1. **Pool Generation** — Server generates daily/weekly quest pools from config templates (10 daily, 5 weekly)
2. **Browse & Accept** — Players view available quests with localized names (GUI or chat) and accept
3. **Progress Tracking** — Actions automatically tracked via ECS events and balance polling
4. **Notifications** — Localized progress messages in chat + milestones at 25/50/75%
5. **Completion & Reward** — At 100%, currency is deposited via Ecotale API + bonus XP granted
6. **Expiry** — Unfinished quests expire at the next daily/weekly reset

## 🏗️ Architecture

```
EcoTaleQuests/
├── model/                          # Data models
│   ├── QuestType.java              #   6 types (KILL_MOB, MINE_ORE, CHOP_WOOD,
│   │                               #            HARVEST_CROP, EARN_COINS, GAIN_XP)
│   ├── QuestPeriod.java            #   DAILY / WEEKLY
│   ├── QuestStatus.java            #   AVAILABLE → ACTIVE → COMPLETED
│   ├── QuestObjective.java         #   Type + target + required amount + wildcard support
│   ├── QuestReward.java            #   Base currency + bonus XP
│   ├── Quest.java                  #   Immutable quest definition
│   └── PlayerQuestData.java        #   Per-player progress tracking
│
├── config/
│   ├── QuestsConfig.java           #   5-section typed config
│   └── ConfigManager.java          #   JSON config load/save/reload
│
├── lang/
│   └── LangManager.java            #   RU/EN localization with placeholders
│
├── generator/
│   └── QuestGenerator.java         #   Pool generation from 46+ candidates
│
├── reward/
│   └── QuestRewardCalculator.java  #   Reward calculation & grant (Ecotale API)
│
├── storage/
│   ├── QuestStorage.java           #   Storage interface
│   └── JsonQuestStorage.java       #   JSON file-based persistence
│
├── tracker/
│   └── QuestTracker.java           #   Central quest management, localized names & progress
│
├── listeners/
│   ├── MobKillQuestListener.java   #   RPG Leveling API: mobs + XP
│   ├── BlockQuestListener.java     #   ECS: BreakBlock + UseBlock.Post
│   └── CoinQuestListener.java     #   Balance polling via Ecotale API
│
├── gui/
│   ├── PlayerQuestsGui.java         #   Native quest panel (InteractiveCustomUIPage)
│   └── AdminQuestsGui.java          #   Admin settings panel (InteractiveCustomUIPage)
│
├── assets/
│   └── ui/
│       ├── QuestPanel.ui            #   Player GUI layout
│       └── AdminPanel.ui            #   Admin GUI layout
│
├── protection/
│   └── QuestAbuseGuard.java        #   Cooldowns & world filtering
│
├── commands/
│   └── QuestsCommandCollection.java  # 12 subcommands (incl. gui, admin, langen, langru)
│
├── util/
│   ├── PluginLogger.java           #   SLF4J-compatible logging
│   ├── MiniMessageParser.java      #   MiniMessage → Hytale JSON + stripTags()
│   └── MessageUtil.java            #   Formatting, progress bar, PlayerRef cache
│
└── EcoTaleQuestsPlugin.java        #   Main entry point & lifecycle
```

### Key Design Decisions

- **ECS Event Pattern** — Block listeners use Hytale's `EntityEventSystem<EntityStore, Event>` with `ArchetypeChunk` for player resolution
- **Reflection-first** — All Store, PlayerRef, Message calls use reflection for stub compatibility
- **Ecotale API (static)** — Rewards and currency tracking via `com.ecotale.api.EcotaleAPI` (static API from Ecotale-1.0.7)
- **Balance Polling** — `CoinQuestListener` checks balances every 2 seconds — if increased, the difference counts as earnings
- **RPG API Auto-detect** — Tries `get()`, `getInstance()`, `getAPI()` methods for cross-version compatibility
- **JSON Storage** — File-based persistence (`quests/` for pools, `players/<uuid>.json` for progress) — no database required
- **Immutable Quests** — `Quest` objects are immutable; only `PlayerQuestData` tracks mutable progress state
- **Native GUI** — Player and admin panels built on `InteractiveCustomUIPage` with `.ui` layouts — no external GUI library needed
- **Wildcard Matching** — `QuestObjective.matches()` supports `any_*` targets that match any action of their type
- **Localized Quest Names** — `QuestTracker.localizedQuestDesc()` builds player-specific quest descriptions from `quest.desc.*` + `target.*` lang keys — no raw IDs shown to players

## 🔌 EcoTale Ecosystem

EcoTaleQuests is part of the CrystalRealm EcoTale plugin family:

| Plugin | Description |
|:-------|:------------|
| **[EcoTaleIncome](https://github.com/CrystalRealm/EcoTaleIncome)** | Earn currency through mob kills, mining, woodcutting, farming |
| **[EcoTaleBanking](https://github.com/CrystalRealm/EcoTaleBanking)** | Banking system — deposits, loans, credit score, interest |
| **EcoTaleQuests** | Daily & weekly quest system ← *you are here* |

## 🌍 Localization

Built-in support for Russian and English. Language files are auto-generated on first start.

**Per-player switching:**
```
/quests langru
/quests langen
```

**All quest names are fully localized.** Players see translated quest descriptions (`Добыть: Железо ×24`, `Убить: Любые мобы ×22`) in both GUI and chat — never raw internal IDs.

**100+ localization keys**, including:
- `quest.completed` / `quest.action_progress` — progress and completion messages
- `quest.desc.*` — descriptions for all 6 quest types
- `quest.type.*` — category names (Мобы, Руда, Дерево, Урожай, Валюта, Опыт)
- `target.*` — 40+ targets (mobs, ores, trees, crops) including wildcards (Любые мобы, Любая руда…)
- `cmd.*` — command feedback
- `gui.*` — GUI panel elements

**Custom translations:** Edit the generated JSON files in `mods/com.crystalrealm_EcoTaleQuests/lang/`

## 📝 Changelog

### v1.2.1
- **Fix:** `earn_coins` and `gain_xp` quests gave disproportionately high rewards (up to 50× base) due to raw amount used in difficulty formula without normalization
- **Fix:** `DifficultyMultipliers` from config were loaded but never applied to reward calculation
- **New:** Amount normalization by quest type — `earn_coins` (÷10) and `gain_xp` (÷100) now produce balanced rewards comparable to other quest types

### v1.2.0
- **New:** Native GUI — full migration from HyUI to `InteractiveCustomUIPage` API
- **New:** Admin settings panel (`/quests admin`) — toggle modules, reload, refresh, save
- **New:** Language subcommands — `/quests langen`, `/quests langru`
- **New:** Separate duplicate type checking — daily and weekly quests validated independently
- **Fix:** `Could not find document` crash — added `IncludesAssetPack` to manifest
- **Fix:** Truncated GUI buttons (widened to 180px)
- **Removed:** HyUI dependency

### v1.1.0
- **Fix:** Mob kill quests not tracking kills (skeletons, kweebecs, and other mobs were not counted)
- **Fix:** Quest GUI text rendering issues with special formatting
- **New:** VIP reward multipliers for quest completion
- **New:** Native ECS death system for reliable mob kill detection
- **Improved:** Overall quest tracking stability

### v1.0.1
- Initial release

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the existing code style and patterns
4. Compile against Hytale server stubs (`src/stubs/`)
5. Test with Ecotale and RPG Leveling installed
6. Submit a pull request

### Building

```bash
# Clone
git clone https://github.com/CrystalRealm/EcoTaleQuests.git
cd EcoTaleQuests

# Build
./gradlew clean jar

# Output: build/libs/EcoTaleQuests-1.2.1.jar
```

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file.

---

**Made with ❤️ by [CrystalRealm](https://hytale-server.pro-gamedev.ru/) for the Hytale community**
