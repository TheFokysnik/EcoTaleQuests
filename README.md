# 📜 EcoTaleQuests

**Daily & Weekly quest system for your Hytale server**

Give players **daily** and **weekly** quests — kill mobs, mine ores, chop trees, harvest crops — with automatic generation, level-scaled rewards, and milestone notifications.

![Hytale Server Mod](https://img.shields.io/badge/Hytale-Server%20Mod-0ea5e9?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.0.0-10b981?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-f97316?style=for-the-badge&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-a855f7?style=for-the-badge)
![EcoTale API](https://img.shields.io/badge/EcoTale%20API-1.0.0-6366f1?style=for-the-badge)

[**Getting Started**](#-getting-started) · [**Features**](#-features) · [**Commands**](#-commands) · [**Configuration**](#-configuration) · [**Architecture**](#-architecture) · [**Contributing**](#-contributing)

---

## ✨ Features

| Feature | Description |
|:--------|:------------|
| 📋 **Daily Quests** | Up to 5 fresh quests every day from a generated pool |
| 📅 **Weekly Quests** | Up to 2 challenging weekly quests with bigger rewards |
| ⚔️ **Kill Mobs** | Quests for specific mob types with kill count targets |
| ⛏️ **Mine Ores** | Quests for mining copper, iron, gold, diamond and more |
| 🪓 **Chop Wood** | Quests for harvesting various tree types |
| 🌾 **Harvest Crops** | Quests for farming wheat, pumpkins, berries and more |
| 💰 **Earn Coins** | Meta-quests — earn a total amount of currency |
| ✨ **Gain XP** | Meta-quests — gain RPG experience points |
| 🎲 **Auto-Generation** | Quest pools are generated automatically from config templates |
| 📈 **Level Scaling** | Quest difficulty and rewards scale with player RPG level |
| 🏅 **Milestone Alerts** | Notifications at 25%, 50%, 75% progress |
| 🛡️ **Abuse Protection** | Accept cooldowns, world filtering, expiry enforcement |
| 💾 **JSON Storage** | Per-player quest data saved as JSON files |
| 🔌 **EcoTale API** | Uses shared EcoTale API for economy transactions |
| 🌍 **Localization** | RU / EN with per-player language switching |
| 🔧 **Hot Reload** | `/quests reload` — no restart needed |

## 📦 Requirements

| Dependency | Version | Required | Description |
|:-----------|:--------|:--------:|:------------|
| [Ecotale](https://curseforge.com/hytale/mods/ecotale) | ≥ 1.0.0 | ✅ | Economy & currency system |
| [EcoTale API](https://github.com/CrystalRealm/ecotale-api) | ≥ 1.0.0 | ✅ | Shared economy API contracts |
| [RPG Leveling](https://www.curseforge.com/hytale/mods/rpg-leveling-and-stats) | ≥ 0.2.0 | ❌ | Level-scaling for quest rewards |

> [!TIP]
> Without RPG Leveling, all players are treated as level 1. Quest generation and rewards still work — just without level scaling.

## 🚀 Getting Started

```bash
# 1. Download the latest release
# 2. Drop into your server's mods/ folder
cp EcoTaleQuests-1.0.0.jar /server/mods/

# 3. Make sure EcoTale API jar is also in mods/
cp ecotale-api-1.0.0.jar /server/mods/

# 4. Start the server — config & lang files are created automatically
# 5. Edit config to customize quest templates
nano mods/com.crystalrealm_EcoTaleQuests/EcoTaleQuests.json
```

**That's it.** Quest pools are generated on first startup. Players can immediately browse and accept quests.

## 🎮 Commands

| Command | Description | Permission |
|:--------|:------------|:-----------|
| `/quests` | Show active quests with progress | `ecotalequests.command.quests` |
| `/quests active` | List your active quests | `ecotalequests.command.quests` |
| `/quests available` | Browse available quests to accept | `ecotalequests.command.quests` |
| `/quests accept <id>` | Accept a quest from the pool | `ecotalequests.command.quests` |
| `/quests abandon <id>` | Abandon an active quest | `ecotalequests.command.quests` |
| `/quests info <id>` | Detailed quest information | `ecotalequests.command.quests` |
| `/quests stats` | Your quest completion statistics | `ecotalequests.command.stats` |
| `/quests reload` | Reload config & lang files | `ecotalequests.admin.reload` |
| `/quests lang <en\|ru>` | Switch language | — |
| `/quests help` | Command reference | — |

> [!NOTE]
> Quest IDs use short 8-character identifiers (e.g., `a3f7b2c1`). Tab completion is supported.

## 🔐 Permissions

**Base Permissions** — all players:
```yaml
ecotalequests.command.quests   # /quests, /quests active, available, accept, abandon, info
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

### Quest System Settings

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

The generator picks from these templates randomly, applies level scaling to amounts, and creates quest objectives. Templates with higher `maxAmount` tend to appear as weekly quests.

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
| `dailyBaseCoins` | 15.0 | Base coin reward for daily quests |
| `weeklyBaseCoins` | 75.0 | Base coin reward for weekly quests |
| `bonusXpPerQuest` | 50 | Bonus RPG XP per completed quest |
| `levelScalingFactor` | 0.08 | Reward multiplier per player level (level × factor) |
| `vipMultiplier` | 1.25 | Reward multiplier for VIP players |
| `premiumMultiplier` | 1.50 | Reward multiplier for Premium players |

**Reward formula:** `Final Reward = Base × (1 + level × factor) × VIP mult`

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
2. **Browse & Accept** — Players view available quests and accept ones they want
3. **Progress Tracking** — Actions (kills, mining, etc.) automatically tracked via ECS events
4. **Milestone Notifications** — Players get alerts at 25%, 50%, 75% completion
5. **Completion & Reward** — Upon 100%, coins are deposited via EcoTale API + bonus XP granted
6. **Expiry** — Unfinished quests expire at the next daily/weekly reset

## 🏗️ Architecture

```
EcoTaleQuests/
├── model/                    # Data models
│   ├── QuestType.java        #   6 quest types (KILL_MOB, MINE_ORE, ...)
│   ├── QuestPeriod.java      #   DAILY / WEEKLY
│   ├── QuestStatus.java      #   AVAILABLE → ACTIVE → COMPLETED
│   ├── QuestObjective.java   #   Type + target + required amount
│   ├── QuestReward.java      #   Base coins + bonus XP
│   ├── Quest.java            #   Immutable quest definition
│   └── PlayerQuestData.java  #   Per-player progress tracking
│
├── config/
│   ├── QuestsConfig.java     #   5-section typed config
│   └── ConfigManager.java    #   JSON config load/save/reload
│
├── lang/
│   └── LangManager.java      #   RU/EN localization with placeholders
│
├── generator/
│   └── QuestGenerator.java   #   Pool generation from templates
│
├── reward/
│   └── QuestRewardCalculator.java  # EcoTale API integration
│
├── storage/
│   ├── QuestStorage.java     #   Storage interface
│   └── JsonQuestStorage.java #   JSON file-based persistence
│
├── tracker/
│   └── QuestTracker.java     #   Central quest management & progress
│
├── listeners/
│   ├── MobKillQuestListener.java   # RPG Leveling API hook
│   └── BlockQuestListener.java     # ECS BreakBlock + UseBlock.Post
│
├── protection/
│   └── QuestAbuseGuard.java  #   Cooldowns & world filtering
│
├── commands/
│   └── QuestsCommandCollection.java  # 9 subcommands
│
├── util/
│   ├── PluginLogger.java     #   SLF4J-style logging
│   ├── MiniMessageParser.java #  MiniMessage → Hytale JSON
│   └── MessageUtil.java      #   Format helpers & progress bar
│
└── EcoTaleQuestsPlugin.java  #   Main entry point & lifecycle
```

### Key Design Decisions

- **ECS Event Pattern** — Block listeners use Hytale's `EntityEventSystem<EntityStore, Event>` with `ArchetypeChunk` for player resolution, matching EcoTaleIncome's proven approach
- **EcoTale API** — All economy transactions use the shared `EconomyService` contract with `TransactionSource.QUEST`
- **JSON Storage** — Simple file-based persistence (`quests/` for pools, `players/<uuid>.json` for progress) — no database required
- **Immutable Quests** — `Quest` objects are immutable; only `PlayerQuestData` tracks mutable progress state
- **Template-based Generation** — Quest diversity through configurable templates rather than hardcoded quests

## 🔌 EcoTale Ecosystem

EcoTaleQuests is part of the CrystalRealm EcoTale plugin family:

| Plugin | Description |
|:-------|:------------|
| **[ecotale-api](https://github.com/CrystalRealm/ecotale-api)** | Shared API contracts for economy plugins |
| **[EcoTaleIncome](https://github.com/CrystalRealm/EcoTaleIncome)** | Earn currency through mob kills, mining, woodcutting, farming |
| **EcoTaleQuests** | Daily & weekly quest system ← *you are here* |

## 🌍 Localization

Built-in support for Russian and English. Language files are auto-generated on first start.

**Per-player switching:**
```
/quests lang ru
/quests lang en
```

**Custom translations:** Edit the generated JSON files in `mods/com.crystalrealm_EcoTaleQuests/lang/`

**Available message keys (50+):**

- `quest.active.*` — Active quest display
- `quest.available.*` — Available quest browsing
- `quest.accept.*` / `quest.abandon.*` — Quest management
- `quest.progress.*` — Milestone notifications
- `quest.complete.*` — Completion messages
- `quest.info.*` — Detailed quest info
- `quest.stats.*` — Statistics display
- `quest.error.*` — Error messages
- `command.*` — Command feedback
- `general.*` — Common messages

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the existing code style and patterns
4. Compile against Hytale server stubs (`src/stubs/`)
5. Test with EcoTale API and EcoTaleIncome installed
6. Submit a pull request

### Building

```bash
# Clone
git clone https://github.com/CrystalRealm/EcoTaleQuests.git
cd EcoTaleQuests

# Build (requires ecotale-api-1.0.0.jar in libs/)
./gradlew build

# Output: build/libs/EcoTaleQuests-1.0.0.jar
```

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file.

---

**Made with ❤️ by [CrystalRealm](https://hytale-server.pro-gamedev.ru/) for the Hytale community**
