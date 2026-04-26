# Hookshot-Plugin

Hookshot-Plugin transforms fishing rods into configurable grappling hooks, letting players move vertically and horizontally with per-player settings for range, velocity, and effects.

## Features

- Toggle grappling hook mode on demand
- Player-specific configuration profiles
- Adjustable hook range, pull speed, boost, and throw velocity
- Optional particles and sound effects per player
- Quick reset command to revert to server defaults

## Commands

Main command:

- `/hookshot` (aliases: `/hook`, `/grapple`)

| Command | Description |
| --- | --- |
| `/hookshot` | Toggle hookshot mode |
| `/hookshot toggle` | Toggle hookshot mode |
| `/hookshot range <value>` | Set hook range |
| `/hookshot speed <value>` | Set pull speed |
| `/hookshot boost <value>` | Set vertical boost |
| `/hookshot velocity <value>` | Set hook throw velocity |
| `/hookshot sound` | Toggle sound effects |
| `/hookshot particles` | Toggle particle effects |
| `/hookshot reset` | Reset to default values |
| `/hookshot info` | Show active player settings |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `hookshot.use` | true | Use hookshot movement |
| `hookshot.settings` | true | Change personal hookshot settings |
| `hookshot.admin` | op | Administrative access |

## Installation

1. Download the latest jar from Releases.
2. Place the jar in your server `plugins` directory.
3. Restart the server.
4. Review `config.yml` for default and limit tuning.

## Compatibility

- API: Paper/Spigot/Bukkit 1.19+

## License

This project is licensed under the MIT License.
   - `/hookshot sound` - Toggle sound on/off
5. Use `/hookshot reset` to restore all defaults
6. Check your settings with `/hookshot info`

## Requirements

- Spigot/Paper 1.19+
- Java 17+
