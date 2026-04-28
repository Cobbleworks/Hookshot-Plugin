<p align="center">
  <img src="images/plugin-logo.png" alt="Hookshot Plugin" width="180" />
</p>
<h1 align="center">Hookshot Plugin</h1>
<p align="center">
  <b>Transform fishing rods into configurable grappling hooks.</b><br>
  <b>Per-player settings for range, velocity, sound, particles, and vertical boost.</b>
</p>
<p align="center">
  <a href="https://github.com/Cobbleworks/Hookshot-Plugin/releases"><img src="https://img.shields.io/github/v/release/Cobbleworks/Hookshot-Plugin?include_prereleases&style=flat-square&color=4CAF50" alt="Latest Release"></a>&nbsp;&nbsp;<a href="https://github.com/Cobbleworks/Hookshot-Plugin/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"></a>&nbsp;&nbsp;<img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Minecraft-1.19+-green?style=flat-square" alt="Minecraft Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Platform-Spigot%2FPaper-yellow?style=flat-square" alt="Platform">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Status-Active-brightgreen?style=flat-square" alt="Status">&nbsp;&nbsp;<a href="https://github.com/Cobbleworks/Hookshot-Plugin/issues"><img src="https://img.shields.io/github/issues/Cobbleworks/Hookshot-Plugin?style=flat-square&color=orange" alt="Open Issues"></a>
</p>

Hookshot is an open-source Minecraft plugin that transforms fishing rods into fully configurable grappling hooks. When grappling mode is enabled, casting a fishing rod launches the hook at the player's configured throw velocity — and upon hitting a block, the player is immediately pulled toward the hook's landing position with a configurable pull speed and vertical boost. When the hook catches a non-player living entity, the entity is pulled toward the player instead. Every setting — range, pull speed, throw velocity, vertical boost, sound, and particles — is independently configurable per player and persisted automatically in `playerdata.yml` across server restarts.

Originally built for custom server gameplay, Hookshot is designed to be straightforward to install and intuitive to use. No dependencies are required, configuration is minimal, and players can fine-tune their own settings through a single command without any admin involvement.

### **Core Features**

- **Toggle Mode:** Each player independently enables or disables grappling hook mode — casting while disabled behaves as a normal fishing rod with no side effects
- **Block Grappling:** When the hook lands on a block within range, the player is pulled toward that position immediately with velocity proportional to their configured pull speed and vertical boost
- **Entity Grappling:** When the hook catches a non-player living entity (mob, animal, etc.), the entity is pulled toward the player — useful for mob retrieval and custom gameplay scenarios
- **Configurable Hook Velocity:** Adjustable throw multiplier (1.0–5.0) controls how fast and far the hook travels through the air before landing
- **Configurable Pull Speed:** Adjustable pull speed (0.5–5.0) controls how quickly the player is moved toward the hook landing point
- **Vertical Boost:** Optional upward momentum (0.0–1.5) added to the pull direction, helping players clear ledges and obstacles naturally
- **Range Limit:** Each player has a maximum hook range (10–100 blocks set by the server); pulls beyond that range are cancelled with an informational message
- **Pull Cooldown:** A 250 ms gap is enforced between consecutive pulls to prevent rubber-banding or velocity stacking
- **Sound Feedback:** Optional pull sound (`ENTITY_ENDER_DRAGON_FLAP`) played at the player's location, toggleable per player
- **Particle Trail:** Optional `CLOUD` particle burst spawned at the player's location on pull, toggleable per player
- **Persistent Settings:** All per-player data is written to `plugins/Hookshot/playerdata.yml` on every change and on server shutdown, and reloaded on every player join
- **Reset Command:** Restores all personal settings to the server-configured defaults in a single command

### **Supported Platforms**

- **Server Software:** `Spigot`, `Paper`, `Purpur`, `CraftBukkit`
- **Minecraft Versions:** `1.19` and higher
- **Java Requirements:** `Java 17+`
- **Dependencies:** None — fully self-contained, no external plugins required

## **Table of Contents**

1. [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation Steps](#installation-steps)
    - [First Launch & Configuration](#first-launch--configuration)
    - [Verifying Installation](#verifying-installation)
2. [Configuration](#configuration)
    - [config.yml — Server Defaults & Limits](#configyml--server-defaults--limits)
    - [playerdata.yml — Per-Player Storage](#playerdatayml--per-player-storage)
3. [How It Works](#how-it-works)
    - [Block Grappling](#block-grappling)
    - [Entity Grappling](#entity-grappling)
    - [Pull Cooldown](#pull-cooldown)
4. [Player Commands](#player-commands)
    - [Command Reference](#command-reference)
    - [Setting Details](#setting-details)
    - [Tab Completion](#tab-completion)
5. [Permissions](#permissions)
6. [Building from Source](#building-from-source)
7. [License](#license)
8. [Screenshots](#screenshots)

## **Getting Started**

### **Prerequisites**

Before installing Hookshot, confirm the following requirements are met:

- A Minecraft server running **Spigot**, **Paper**, **Purpur**, or any compatible fork
- Server version **1.19 or higher** (`api-version: 1.19` is the minimum)
- **Java 17** or newer installed on the machine running the server
- Operator or console access to install plugin files

No additional plugins or libraries are needed. Hookshot has zero external dependencies.

### **Installation Steps**

1. Download the latest `Hookshot-x.x.x.jar` from the [Releases](https://github.com/Cobbleworks/Hookshot-Plugin/releases) page
2. **Stop your server completely** before placing any files
3. Copy the `.jar` into your server's `plugins/` directory
4. Start the server — Hookshot generates its configuration folder automatically on first boot

### **First Launch & Configuration**

On the first server start after installation, Hookshot creates the following directory structure:

```
plugins/
└── Hookshot/
    ├── config.yml        ← Server-wide defaults and per-player limits
    └── playerdata.yml    ← Per-player settings (auto-managed, do not edit manually)
```

- **`config.yml`** contains two sections: the server-wide default values applied to every new player and the minimum/maximum limits that cap what players can configure for themselves. Edit this file to tune the plugin for your server, then restart to apply changes.
- **`playerdata.yml`** is written and read by the plugin automatically. It stores each player's personal settings (range, speed, boost, velocity, sound, particles, enabled state) keyed by UUID. Do not edit this file manually — any changes will be overwritten on the next save.

After editing `config.yml` and restarting, only **new players** and players who run `/hookshot reset` will receive the updated defaults. Existing player data is not automatically migrated.

### **Verifying Installation**

- Run `/plugins` in-game — `Hookshot` should appear green in the list
- Run `/version Hookshot` to confirm the installed version matches the release you downloaded
- Hold a fishing rod, run `/hookshot toggle`, and right-click to cast toward a block — you should be pulled toward it
- If the plugin fails to load, check the server console for `Hookshot` error messages (common causes: wrong Java version, corrupt JAR, or an unsupported API version)

## **Configuration**

### **config.yml — Server Defaults & Limits**

`plugins/Hookshot/config.yml` is divided into two sections. The **default values** define what every new player receives and what `/hookshot reset` restores. The **setting limits** cap the values players are permitted to configure using commands — any attempt to set a value outside these bounds is rejected with an error message.

```yaml
# ===== DEFAULT VALUES =====
default-range: 30.0
pull-speed: 2.0
vertical-boost: 0.3
hook-velocity-multiplier: 2.0
play-sound: true
show-particles: true

# ===== SETTING LIMITS =====
min-range: 10.0
max-range: 100.0
min-pull-speed: 0.5
max-pull-speed: 5.0
min-vertical-boost: 0.0
max-vertical-boost: 1.5
min-hook-velocity: 1.0
max-hook-velocity: 5.0
```

**Default Values** — applied to new players and restored by `/hookshot reset`:

| Key | Default | Description |
|-----|---------|-------------|
| `default-range` | `30.0` | Starting hook range in blocks for new players |
| `pull-speed` | `2.0` | Default pull speed multiplier |
| `vertical-boost` | `0.3` | Default upward momentum added to the pull vector |
| `hook-velocity-multiplier` | `2.0` | Default hook throw speed multiplier |
| `play-sound` | `true` | Whether sound is enabled by default |
| `show-particles` | `true` | Whether particles are enabled by default |

**Setting Limits** — hard bounds on what players can configure:

| Key | Default | Description |
|-----|---------|-------------|
| `min-range` | `10.0` | Minimum range a player may set (blocks) |
| `max-range` | `100.0` | Maximum range a player may set (blocks) |
| `min-pull-speed` | `0.5` | Minimum pull speed a player may set |
| `max-pull-speed` | `5.0` | Maximum pull speed a player may set |
| `min-vertical-boost` | `0.0` | Minimum vertical boost (0 = no upward assist) |
| `max-vertical-boost` | `1.5` | Maximum vertical boost a player may set |
| `min-hook-velocity` | `1.0` | Minimum hook throw velocity a player may set |
| `max-hook-velocity` | `5.0` | Maximum hook throw velocity a player may set |

> **Important:** Config changes only apply after a server restart. Existing player settings in `playerdata.yml` are never automatically updated — only new players or players who run `/hookshot reset` receive the new defaults.

### **playerdata.yml — Per-Player Storage**

This file is generated and maintained entirely by the plugin. It stores one entry per player, keyed by their UUID:

```yaml
players:
  "550e8400-e29b-41d4-a716-446655440000":
    enabled: false
    range: 30.0
    pull-speed: 2.0
    vertical-boost: 0.3
    hook-velocity: 2.0
    sound-enabled: true
    particles-enabled: true
```

The file is loaded on plugin startup and on every player join. It is saved automatically on every setting change and on plugin shutdown (`onDisable`). There is no manual reload command — the data is always in sync with what the plugin has in memory.

## **How It Works**

### **Block Grappling**

Block grappling is the primary mechanic. This is what happens step by step when a player casts with hookshot mode active:

1. The player holds a **fishing rod** and **right-clicks** to cast
2. The plugin intercepts the `ProjectileLaunchEvent` and multiplies the hook's velocity by the player's `hook-velocity-multiplier`, sending it further and faster than a normal fishing rod
3. The hook travels as a standard `FishHook` projectile
4. When the hook contacts a **block**, the `ProjectileHitEvent` fires
5. The plugin measures the straight-line distance between the hook's landing position and the player
6. If the distance exceeds the player's configured range, a red message is shown (`Hook too far! Maximum range: X blocks`) and no pull is applied
7. If within range, the direction vector from the player to the hook is calculated, normalized, and multiplied by the player's `pull-speed`
8. The player's `vertical-boost` value is added to the Y component of this vector, providing upward assistance to clear ledges
9. The resulting vector is applied directly as the player's velocity via `player.setVelocity()`
10. If sound is enabled, `ENTITY_ENDER_DRAGON_FLAP` plays at the player's location at volume 1.0 and pitch 1.5
11. If particles are enabled, 20 `CLOUD` particles are spawned around the player's location with spread 0.5 on each axis

### **Entity Grappling**

When the hook catches a **living entity that is not a player**:

1. The `PlayerFishEvent` fires with state `CAUGHT_ENTITY`
2. The plugin confirms the caught entity is a `LivingEntity` and not a `Player`
3. The entity's distance from the player is checked against the player's range limit
4. If within range, the direction from the entity toward the player is calculated, normalized, and multiplied by `pull-speed × 1.5` (entities receive a 1.5× strength multiplier for a more dramatic pull feel)
5. The vertical component receives a `vertical-boost × 1.2` multiplier
6. The velocity is applied to the entity via `entity.setVelocity()`
7. If sound is enabled, `ENTITY_FISHING_BOBBER_RETRIEVE` plays at the entity's location at volume 1.0 and pitch 0.8
8. If particles are enabled, 15 `CRIT` particles are spawned at the entity's center (height/2) with 0.3 spread

> **Key distinction:** Block grappling pulls **the player** to the hook. Entity grappling pulls **the entity** toward the player.

### **Pull Cooldown**

A minimum gap of **250 milliseconds** is enforced between consecutive pulls for each player. If a second pull is triggered within this window (e.g., from a rapid re-cast or dual event), it is silently ignored. This prevents velocity stacking from multiple near-simultaneous pulls and eliminates rubber-banding on high-speed servers.

The cooldown is stored in memory per player UUID (`Map<UUID, Long>` tracking the timestamp of the last successful pull) and does not persist across restarts.

## **Player Commands**

All commands require the `hookshot.use` permission, which is granted to all players by default. Commands that change settings additionally require `hookshot.settings`, also granted by default. Only the `hookshot.admin` permission is restricted to operators.

### **Command Reference**

| Command | Permission | Description |
|---------|------------|-------------|
| `/hookshot` | `hookshot.use` | Toggle grappling hook mode on or off (shorthand for `toggle`) |
| `/hookshot toggle` | `hookshot.use` | Toggle grappling hook mode on or off |
| `/hookshot range <value>` | `hookshot.settings` | Set your hook range in blocks — must be within server min/max |
| `/hookshot speed <value>` | `hookshot.settings` | Set your pull speed — controls how fast you are pulled to the hook |
| `/hookshot boost <value>` | `hookshot.settings` | Set your vertical boost — upward momentum applied on every pull |
| `/hookshot velocity <value>` | `hookshot.settings` | Set your hook throw velocity — controls how far the hook is launched |
| `/hookshot sound` | `hookshot.settings` | Toggle your personal sound effects on or off |
| `/hookshot particles` | `hookshot.settings` | Toggle your personal particle effects on or off |
| `/hookshot reset` | `hookshot.use` | Reset all your settings to the current server default values |
| `/hookshot info` | `hookshot.use` | Display all of your current hookshot settings in chat |

**Aliases:** `/hook`, `/grapple` — all subcommands work with these aliases

### **Setting Details**

**`/hookshot range <value>`**
Sets the maximum distance in blocks between you and the hook's landing point. If the hook lands further than your range, the pull is cancelled and you see an error message. The server administrator controls the allowed minimum and maximum via `config.yml`.

Example: `/hookshot range 50`

---

**`/hookshot speed <value>`**
Sets how quickly you are pulled toward the hook. A value of `1.0` is a gentle, slow pull; `5.0` is an extremely fast, aggressive launch. The pull is always in the exact direction of the hook from your current position.

Example: `/hookshot speed 2.5`

---

**`/hookshot boost <value>`**
Adds upward momentum to the pull vector. Without boost (`0.0`), the pull is purely directional and will not help you clear ledges above you. With boost, a fraction of upward force is added on top of the directional pull, making it much easier to land on elevated surfaces.

Example: `/hookshot boost 0.5`

---

**`/hookshot velocity <value>`**
Controls how fast the fishing hook is launched when you cast. Higher values throw the hook further in less time, effectively increasing your usable range in practice. This is separate from your range limit — a hook can travel fast but still be cancelled if it lands beyond your range setting.

Example: `/hookshot velocity 3.0`

---

**`/hookshot info`**
Displays all of your current settings in chat:

```
=== Hookshot Settings ===
Status: Enabled
Range: 30.0 blocks
Pull Speed: 2.0
Vertical Boost: 0.3
Hook Velocity: 2.0
Sound: On
Particles: On
```

---

**`/hookshot reset`**
Resets every personal setting (range, speed, boost, velocity, sound, particles, and toggle state) to the server-configured default values from `config.yml`. Useful if your settings feel wrong or you want to start fresh.

### **Tab Completion**

Full tab completion is provided for all subcommands and their values. Typing `/hookshot ` and pressing Tab shows all available subcommands. Typing a value argument and pressing Tab shows common example values:

| Subcommand | Suggested values |
|------------|-----------------|
| `range` | `10`, `20`, `30`, `50`, `100` |
| `speed` | `1.0`, `1.5`, `2.0`, `2.5`, `3.0` |
| `boost` | `0.0`, `0.3`, `0.5`, `0.8`, `1.0` |
| `velocity` | `1.0`, `1.5`, `2.0`, `3.0`, `4.0` |

## **Permissions**

All player-facing permissions default to `true`, so no permission manager configuration is needed for standard servers. The admin node defaults to `op`.

| Permission | Description | Default |
|------------|-------------|---------|
| `hookshot.use` | Use the grappling hook and run `/hookshot toggle`, `/hookshot reset`, `/hookshot info` | `true` |
| `hookshot.settings` | Adjust personal settings with `/hookshot range`, `speed`, `boost`, `velocity`, `sound`, `particles` | `true` |
| `hookshot.admin` | Administrative access to hookshot features | `op` |

To restrict hookshot to specific player groups, revoke `hookshot.use` from the default group and grant it only to the groups that should have access. Example using LuckPerms:

```
/lp group default permission unset hookshot.use
/lp group member permission set hookshot.use true
```

## **Building from Source**

Hookshot uses **Apache Maven** as its build system. The plugin is built as a fat JAR using the Maven Shade Plugin — no runtime dependencies need to be placed on the server.

**Requirements:**
- Java 17 or newer
- Apache Maven 3.6 or newer

**Steps:**

```bash
# Clone the repository
git clone https://github.com/Cobbleworks/Hookshot-Plugin.git
cd Hookshot-Plugin

# Compile and package
mvn clean package
```

The output JAR is written to `target/Hookshot-x.x.x.jar`. Copy it into your server's `plugins/` folder as described in the [Installation Steps](#installation-steps) section.

**Project Structure:**

```
src/main/
├── java/com/hookshot/
│   ├── HookshotPlugin.java          ← Plugin entry point (onEnable / onDisable)
│   ├── commands/
│   │   └── HookshotCommand.java     ← All /hookshot subcommands + tab completion
│   ├── listeners/
│   │   └── FishingRodListener.java  ← Grappling physics (launch, hit, fish events)
│   └── managers/
│       └── PlayerDataManager.java   ← Per-player data storage and config access
└── resources/
    ├── config.yml                   ← Default server configuration
    └── plugin.yml                   ← Plugin metadata, commands, permissions
```

## **License**

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

## **Screenshots**

The screenshots below showcase Hookshot across a variety of builds and biomes, demonstrating block grappling, entity range, configurable reach, and real-world traversal scenarios.

<table>
  <tr>
    <th>Hookshot Plugin - Battleship Bowser Climb</th>
    <th>Hookshot Plugin - Range Configuration</th>
  </tr>
  <tr>
    <td><a href="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-battleship-bowser.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-battleship-bowser.png" alt="Battleship Bowser Climb" width="450"></a></td>
    <td><a href="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-range-configuration.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-range-configuration.png" alt="Range Configuration" width="450"></a></td>
  </tr>
  <tr>
    <th>Hookshot Plugin - Industry Crane Container</th>
    <th>Hookshot Plugin - Desert Town Ascent</th>
  </tr>
  <tr>
    <td><a href="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-industry-crane.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-industry-crane.png" alt="Industry Crane Container" width="450"></a></td>
    <td><a href="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-desert-town.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-desert-town.png" alt="Desert Town Ascent" width="450"></a></td>
  </tr>
  <tr>
    <th>Hookshot Plugin - Lava Dungeon Traversal</th>
    <th>Hookshot Plugin - Jungle House Climbing</th>
  </tr>
  <tr>
    <td><a href="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-lava-dungeon.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-lava-dungeon.png" alt="Lava Dungeon Traversal" width="450"></a></td>
    <td><a href="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-jungle-house.png" target="_blank" rel="noopener noreferrer"><img src="https://github.com/Cobbleworks/Hookshot-Plugin/raw/main/images/screenshot-jungle-house.png" alt="Jungle House Climbing" width="450"></a></td>
  </tr>
</table>