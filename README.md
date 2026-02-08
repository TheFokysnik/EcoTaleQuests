# 📜 EcoTaleQuests

**Daily & Weekly quest system for Hytale servers**

Give players **daily** and **weekly** quests — kill mobs, mine ores, chop trees, harvest crops, earn currency, gain XP — with automatic generation, level-scaled rewards, an interactive GUI panel, and real-time chat progress notifications.

![Hytale Server Mod](https://img.shields.io/badge/Hytale-Server%20Mod-0ea5e9?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.0.0-10b981?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-f97316?style=for-the-badge&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-a855f7?style=for-the-badge)
![Ecotale](https://img.shields.io/badge/Ecotale-1.0.7-6366f1?style=for-the-badge)
![HyUI](https://img.shields.io/badge/HyUI-0.8.1-e11d48?style=for-the-badge)

[**Getting Started**](#-getting-started) · [**Features**](#-features) · [**Commands**](#-commands) · [**Configuration**](#-configuration) · [**Architecture**](#-architecture)

---

## ✨ Features

| Feature | Description |
|:--------|:------------|
| 📋 **Daily Quests** | Up to 5 fresh quests every day from a generated pool |
| 📅 **Weekly Quests** | Up to 2 challenging weekly quests with bigger rewards |
| ⚔️ **Kill Mobs** | Quests for specific mob types with kill count targets |
| ⛏️ **Mine Ores** | Quests for mining copper, iron, gold, cobalt and more |
| 🪓 **Chop Wood** | Quests for harvesting various tree types |
| 🌾 **Harvest Crops** | Quests for farming wheat, pumpkins, berries and more |
| 💰 **Earn Currency** | Meta-quests — earn a total amount of currency |
| ✨ **Gain XP** | Meta-quests — gain RPG experience from any source |
| 🖥️ **GUI Panel** | Interactive quest panel via HyUI with Daily/Weekly/Active tabs |
| 📊 **Chat Progress** | Notifications on every action (mined ore → `[Q] Mine copper ore: 3/22`) |
| 🎲 **Auto-Generation** | Quest pools are generated automatically from config templates |
| 📈 **Level Scaling** | Quest difficulty and rewards scale with player RPG level |
| 🏅 **Milestone Alerts** | Additional notifications at 25%, 50%, 75% progress |
| 🛡️ **Abuse Protection** | Accept cooldowns, world filtering, expiry enforcement |
| 💾 **JSON Storage** | Per-player quest data saved as JSON files |
| 🔌 **Ecotale API** | Uses Ecotale for economy operations and earnings tracking |
| 🌍 **Localization** | RU / EN with per-player language switching |
| 🔧 **Hot Reload** | `/quests reload` — no restart needed |

## 📦 Requirements

| Dependency | Version | Required | Description |
|:-----------|:--------|:--------:|:------------|
| [Ecotale](https://curseforge.com/hytale/mods/ecotale) | ≥ 1.0.7 | ✅ | Economy & currency (balance, deposit, withdraw) |
| [HyUI](https://github.com/MineInAbyss/HyUI) | ≥ 0.8.0 | ❌ | GUI quest panel (optional) |
| [RPG Leveling](https://www.curseforge.com/hytale/mods/rpg-leveling-and-stats) | ≥ 0.2.0 | ❌ | XP quests, mob kills, reward scaling |

> [!TIP]
> Without RPG Leveling, all players are treated as level 1 — XP and mob kill quests won't track, but all other quest types work normally.
> Without HyUI, commands work via chat; the GUI panel will be unavailable.

## 🚀 Getting Started

```bash
# 1. Copy JAR files to the server's mods/ folder
cp EcoTaleQuests-1.0.0.jar /server/mods/

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
| `/quests gui` | Open the GUI quest panel (HyUI) | `ecotalequests.command.quests` |
| `/quests stats` | Your quest completion statistics | `ecotalequests.command.stats` |
| `/quests reload` | Reload config & lang files | `ecotalequests.admin.reload` |
| `/quests lang <en\|ru>` | Switch language | — |
| `/quests help` | Command reference | — |

> [!NOTE]
> Quest IDs use short 8-character identifiers (e.g., `a3f7b2c1`).

## 🖥️ GUI Panel

Interactive panel via HyUI with three tabs:

- **Daily** — available daily quests with an "Accept" button
- **Weekly** — available weekly quests with an "Accept" button
- **Active** — current quests with a progress bar and "Abandon" button

Open with `/quests gui`. The panel auto-refreshes after accepting or abandoning a quest.

## 🔐 Permissions

**Base Permissions** — all players:
```yaml
ecotalequests.command.quests   # /quests, available, accept, abandon, info, gui
ecotalequests.command.stats    # /quests stats
```

**VIP / Premium:**
```yaml
ecotalequests.multiplier.vip       # ×1.25 quest reward multiplier
ecotalequests.multiplier.premium   # ×1.50 quest reward multiplier
```

**Admin:**
```yaml
ecotalequests.admin.reload    # /quests reload
ecotalequests.*               # All permissions
```

## ⚙️ Configuration

Config file: `mods/com.crystalrealm_EcoTaleQuests/EcoTaleQuests.json`

### 📋 General & Limits

```json
{
  "General": {
    "debug": false,
    "defaultLanguage": "ru",
    "autoSaveIntervalMinutes": 5
  },
  "QuestLimits": {
    "maxActiveDailyQuests": 3,
    "maxActiveWeeklyQuests": 1,
    "dailyPoolSize": 5,
    "weeklyPoolSize": 2,
    "dailyResetHour": 6,
    "weeklyResetDay": "MONDAY"
  }
}
```

| Key | Default | Description |
|:----|:--------|:------------|
| `maxActiveDailyQuests` | 3 | Max daily quests a player can have active at once |
| `maxActiveWeeklyQuests` | 1 | Max weekly quests active at once |
| `dailyPoolSize` | 5 | How many daily quests are generated each day |
| `weeklyPoolSize` | 2 | How many weekly quests are generated each week |
| `dailyResetHour` | 6 | Hour (0–23) when daily pool refreshes |
| `weeklyResetDay` | MONDAY | Day of week for weekly pool refresh |

### 🎲 Quest Generation Templates

Each template defines a quest that can appear in the generated pool:

```json
{
  "Generation": {
    "mobTemplates": [
      { "target": "Kweebec",     "minAmount": 5,  "maxAmount": 15 },
      { "target": "Trork",       "minAmount": 3,  "maxAmount": 10 },
      { "target": "Scarak",      "minAmount": 5,  "maxAmount": 20 },
      { "target": "Feran",       "minAmount": 3,  "maxAmount": 12 },
      { "target": "Fen_Stalker", "minAmount": 2,  "maxAmount": 8  },
      { "target": "Void_Dragon", "minAmount": 1,  "maxAmount": 3  }
    ],
    "oreTemplates": [
      { "target": "copper", "minAmount": 10, "maxAmount": 30 },
      { "target": "iron",   "minAmount": 8,  "maxAmount": 25 },
      { "target": "gold",   "minAmount": 5,  "maxAmount": 15 },
      { "target": "cobalt", "minAmount": 3,  "maxAmount": 10 }
    ],
    "woodTemplates": [
      { "target": "oak",    "minAmount": 15, "maxAmount": 40 },
      { "target": "birch",  "minAmount": 15, "maxAmount": 40 },
      { "target": "pine",   "minAmount": 10, "maxAmount": 30 },
      { "target": "ebony",  "minAmount": 5,  "maxAmount": 15 }
    ],
    "cropTemplates": [
      { "target": "wheat",   "minAmount": 20, "maxAmount": 50 },
      { "target": "pumpkin", "minAmount": 10, "maxAmount": 30 },
      { "target": "berry",   "minAmount": 15, "maxAmount": 40 }
    ]
  }
}
```

The generator picks from these templates randomly, applies level scaling to amounts, and creates quest objectives.

### 💰 Rewards

```json
{
  "Rewards": {
    "dailyBaseCoins": 15.0,
    "weeklyBaseCoins": 75.0,
    "bonusXpPerQuest": 50,
    "levelScalingFactor": 0.08,
    "vipMultiplier": 1.25,
    "premiumMultiplier": 1.5
  }
}
```

| Key | Default | Description |
|:----|:--------|:------------|
| `dailyBaseCoins` | 15.0 | Base currency reward for daily quests |
| `weeklyBaseCoins` | 75.0 | Base currency reward for weekly quests |
| `bonusXpPerQuest` | 50 | Bonus RPG XP per completed quest |
| `levelScalingFactor` | 0.08 | Reward multiplier per player level (level × factor) |

**Reward formula:** `Final Reward = Base × (1 + level × factor)`

### 🛡️ Protection

```json
{
  "Protection": {
    "acceptCooldownSeconds": 30,
    "allowedWorlds": [],
    "blockAbuseInCreative": true
  }
}
```

| Key | Default | Description |
|:----|:--------|:------------|
| `acceptCooldownSeconds` | 30 | Cooldown between accepting quests |
| `allowedWorlds` | `[]` (all) | Restrict quest progress to specific worlds |
| `blockAbuseInCreative` | true | Prevent quest progress in creative mode |

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

1. **Pool Generation** — Server generates daily/weekly quest pools from config templates
2. **Browse & Accept** — Players view available quests (GUI or chat) and accept
3. **Progress Tracking** — Actions automatically tracked via ECS events and balance polling
4. **Notifications** — Players receive chat messages on every action + milestones at 25/50/75%
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
│   ├── QuestObjective.java         #   Type + target + required amount
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
│   └── QuestGenerator.java         #   Pool generation from templates
│
├── reward/
│   └── QuestRewardCalculator.java  #   Reward calculation & grant (Ecotale API)
│
├── storage/
│   ├── QuestStorage.java           #   Storage interface
│   └── JsonQuestStorage.java       #   JSON file-based persistence
│
├── tracker/
│   └── QuestTracker.java           #   Central quest management & progress
│
├── listeners/
│   ├── MobKillQuestListener.java   #   RPG Leveling API: mobs + XP
│   ├── BlockQuestListener.java     #   ECS: BreakBlock + UseBlock.Post
│   └── CoinQuestListener.java     #   Balance polling via Ecotale API
│
├── gui/
│   └── QuestGui.java               #   HyUI GUI panel with tabs
│
├── protection/
│   └── QuestAbuseGuard.java        #   Cooldowns & world filtering
│
├── commands/
│   └── QuestsCommandCollection.java  # 10 subcommands (incl. gui)
│
├── util/
│   ├── PluginLogger.java           #   SLF4J-compatible logging
│   ├── MiniMessageParser.java      #   MiniMessage → Hytale JSON
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
- **HyUI GUI** — Tabbed quest panel with accept/abandon buttons, progress bars, auto-refresh

## 🔌 EcoTale Ecosystem

EcoTaleQuests is part of the CrystalRealm EcoTale plugin family:

| Plugin | Description |
|:-------|:------------|
| **[EcoTaleIncome](https://github.com/CrystalRealm/EcoTaleIncome)** | Earn currency through mob kills, mining, woodcutting, farming |
| **EcoTaleQuests** | Daily & weekly quest system ← *you are here* |

## 🌍 Localization

Built-in support for Russian and English. Language files are auto-generated on first start.

**Per-player switching:**
```
/quests lang ru
/quests lang en
```

**92 localization keys**, including:
- `quest.completed` / `quest.action_progress` — progress and completion
- `quest.desc.*` — descriptions for all 6 quest types
- `target.*` — 18 targets (mobs, ores, trees, crops)
- `cmd.*` — command feedback
- `gui.*` — GUI panel elements

**Custom translations:** Edit the generated JSON files in `mods/com.crystalrealm_EcoTaleQuests/lang/`

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

# Build (requires HyUI-0.8.1-all.jar in libs/)
./gradlew clean jar

# Output: build/libs/EcoTaleQuests-1.0.0.jar
```

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file.

---

**Made with ❤️ by [CrystalRealm](https://hytale-server.pro-gamedev.ru/) for the Hytale community**
