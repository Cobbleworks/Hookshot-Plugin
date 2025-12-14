# Hookshot Plugin

A Minecraft Spigot plugin that transforms fishing rods into grappling hooks!

## Features

- **Toggle System**: Players can enable/disable the grappling hook feature with `/hookshot toggle`
- **Fully Customizable**: Each player can configure all hookshot parameters
- **Extended Range**: Hooks travel further than normal fishing rods
- **Pull Mechanics**: Players are pulled to where the hook lands
- **Per-Player Configuration**: All settings are saved per-player
- **Sound & Particle Effects**: Visual and audio feedback (toggleable per-player)
- **Reset Command**: Restore all settings to server defaults

## Commands

| Command                      | Description                             |
| ---------------------------- | --------------------------------------- |
| `/hookshot`                  | Toggle the grappling hook on/off        |
| `/hookshot toggle`           | Toggle the grappling hook on/off        |
| `/hookshot range <value>`    | Set your hook range (in blocks)         |
| `/hookshot speed <value>`    | Set pull speed (how fast you're pulled) |
| `/hookshot boost <value>`    | Set vertical boost (upward momentum)    |
| `/hookshot velocity <value>` | Set hook throw velocity                 |
| `/hookshot sound`            | Toggle sound effects on/off             |
| `/hookshot particles`        | Toggle particle effects on/off          |
| `/hookshot reset`            | Reset all settings to server defaults   |
| `/hookshot info`             | View your current settings              |

**Aliases**: `/hook`, `/grapple`

## Permissions

| Permission          | Default | Description                         |
| ------------------- | ------- | ----------------------------------- |
| `hookshot.use`      | true    | Allow use of the grappling hook     |
| `hookshot.settings` | true    | Allow configuring hookshot settings |
| `hookshot.admin`    | op      | Administrative access               |

## Configuration

Edit `config.yml` to customize default values and limits:

### Default Values (for new players)

| Setting                    | Default | Description                  |
| -------------------------- | ------- | ---------------------------- |
| `default-range`            | 30.0    | Default hook range in blocks |
| `pull-speed`               | 2.0     | How fast players are pulled  |
| `vertical-boost`           | 0.3     | Upward momentum when pulled  |
| `hook-velocity-multiplier` | 2.0     | How fast the hook is thrown  |
| `play-sound`               | true    | Sound effects enabled        |
| `show-particles`           | true    | Particle effects enabled     |

### Setting Limits

| Setting        | Min                       | Max                       | Description        |
| -------------- | ------------------------- | ------------------------- | ------------------ |
| Range          | `min-range: 10.0`         | `max-range: 100.0`        | Hook range limits  |
| Pull Speed     | `min-pull-speed: 0.5`     | `max-pull-speed: 5.0`     | Pull speed limits  |
| Vertical Boost | `min-vertical-boost: 0.0` | `max-vertical-boost: 1.5` | Boost limits       |
| Hook Velocity  | `min-hook-velocity: 1.0`  | `max-hook-velocity: 5.0`  | Throw speed limits |

## Building

1. Ensure you have Maven installed
2. Run: `mvn clean package`
3. Find the compiled JAR in the `target/` folder
4. Place the JAR in your server's `plugins/` folder
5. Restart your server

## Usage

1. Use `/hookshot` to enable the grappling hook
2. Right-click with a fishing rod to launch the hook
3. When the hook lands, you'll be pulled to that location
4. Customize your settings:
   - `/hookshot range 50` - Set range to 50 blocks
   - `/hookshot speed 3` - Increase pull speed
   - `/hookshot velocity 4` - Make hook throw faster
   - `/hookshot sound` - Toggle sound on/off
5. Use `/hookshot reset` to restore all defaults
6. Check your settings with `/hookshot info`

## Requirements

- Spigot/Paper 1.19+
- Java 17+
