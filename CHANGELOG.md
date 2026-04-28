# Changelog

All notable changes to Hookshot will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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
