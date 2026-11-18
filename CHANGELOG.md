# Changelog

All notable changes to Hookshot will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Changed

- **Fire Control**: hooks now fire with right-click while holding a sword instead of the Swap Hands key (F), which swaps items normally again

## [2.0.0] - 2026-11-18

Hookshot 2.0 replaces the fishing-rod grapple with a wearable **Grapple Harness** and a rope simulation that runs every tick. You can now swing, reel in, steer in mid-air, and keep your momentum, with both hands free for combat.

### Breaking Changes

- **Paper 26.2+ and Java 25 required**: Spigot is no longer supported
- **Fishing-rod mode removed**, along with `/hookshot toggle|range|speed|boost|velocity|sound|particles|reset|info` and the per-player `playerdata.yml`
- **New `config.yml` layout**: old keys are ignored

### Grapple Harness

- **Wearable Gear**: hooks fire from leggings, so both hands stay free
- **Crafting Recipe**: configurable shaped recipe (tripwire hooks, iron chain, iron, leather leggings, wind charges), added to the recipe book on join
- **Give Command**: `/hookshot give [player] [amount]` for admins and the console

### Movement

- **Dual Hooks**: up to two hooks at once that anchor to blocks, mobs, vehicles, and optionally players
- **Rope Physics**: pendulum swings, reeling with Sneak, and wall perching at full reel
- **Momentum**: low air drag while swinging, and momentum carried after letting go
- **Air Control**: W/A/S/D steering while airborne
- **Gas**: reeling and mid-air bursts use gas, which refills on the ground and is shown on the action bar
- **Rope Rendering**: smooth display-entity ropes that are never saved to disk

### Combat And Safety

- **Momentum Damage**: melee bonus that scales with speed
- **Fall Damage Multiplier** while wearing the harness, and no fall damage while hanging on a rope
- **Flight-Kick Protection** while hanging still, with crash-safe cleanup on rejoin
- **Server-Side Ground Check**, so modified clients cannot refill gas in mid-air

## [1.0.0] - 2026-04-28

Hookshot v1.0.0 is the initial release, transforming fishing rods into fully configurable per-player grappling hooks with block and entity pull, persistent settings, and visual feedback.

### Grappling Mechanics

- **Toggle Mode**: Each player independently enables or disables grappling mode — fishing rods behave normally when disabled
- **Block Grappling**: Player is pulled toward the hook landing position on block hit with configurable pull speed and vertical boost
- **Entity Grappling**: Non-player living entities are pulled toward the player when the hook catches them
- **250 ms Pull Cooldown**: Enforced gap between consecutive pulls to prevent velocity stacking and rubber-banding

### Player Configuration

- **Hook Throw Velocity**: Configurable throw multiplier (1.0–5.0) controlling hook travel speed and range
- **Pull Speed**: Configurable pull speed (0.5–5.0) controlling how quickly the player moves toward the landing point
- **Vertical Boost**: Optional upward momentum (0.0–1.5) added to the pull direction for clearing ledges naturally
- **Maximum Range**: Server-defined maximum hook range (10–100 blocks); pulls beyond range are cancelled with feedback
- **Sound Toggle**: Optional pull sound toggleable per player
- **Particle Toggle**: Optional `CLOUD` particle burst on pull, toggleable per player

### Persistence And Commands

- **Persistent Settings**: All per-player data stored in `playerdata.yml` and reloaded on every player join
- **Reset Command**: `/hookshot reset` restores all personal settings to server-configured defaults

**Note:** If you encounter any bugs or issues, please don't hesitate to open an [issue](https://github.com/Cobbleworks/Hookshot-Plugin/issues). For any questions or to start a discussion, feel free to initiate a [discussion](https://github.com/Cobbleworks/Hookshot-Plugin/discussions) on the GitHub repository.
