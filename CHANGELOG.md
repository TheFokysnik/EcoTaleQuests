# EcoTaleQuests — Changelog

## v1.3.2 — 2026-02-13

### Bug Fixes
- **[CRITICAL] Fixed startup crash** — `NoClassDefFoundError: GameMode`. Replaced `setPermissionGroup(GameMode.Adventure)` with string-based API `setPermissionGroups("Adventure")` which doesn't require importing the server enum.
- **Fixed permissions for regular players** — `/quests` command is now accessible in Adventure mode via `setPermissionGroups("Adventure")`.

### Changes
- Added LuckPerms support (optional dependency).
- Three-tier permission check: LuckPerms API → permissions.json → wildcard matching.
- `PermissionHelper` — utility for reading server `permissions.json` with group and inheritance support.
