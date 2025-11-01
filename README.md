# Hookshot Plugin

A Minecraft Spigot plugin that transforms fishing rods into grappling hooks!

## Features

- **Toggle System**: Players can enable/disable the grappling hook feature with `/hookshot toggle`
- **Configurable Range**: Each player can set their own hook range (within server limits)
- **Extended Range**: Hooks travel further than normal fishing rods
- **Pull Mechanics**: Players are pulled to where the hook lands
- **Per-Player Configuration**: Each player's settings are saved
- **Sound & Particle Effects**: Visual and audio feedback (configurable)

## Commands

- `/hookshot` or `/hookshot toggle` - Toggle the grappling hook on/off
- `/hookshot range <value>` - Set your hook range (requires permission)
- `/hookshot info` - View your current hookshot settings
- Aliases: `/hook`, `/grapple`

## Permissions

- `hookshot.use` (default: true) - Allow use of the grappling hook
- `hookshot.range` (default: true) - Allow players to configure their range
- `hookshot.admin` (default: op) - Administrative access

## Configuration

Edit `config.yml` to customize:

- `default-range`: Default hook range (30 blocks)
- `min-range`: Minimum range players can set (10 blocks)
- `max-range`: Maximum range players can set (100 blocks)
- `pull-speed`: How fast players are pulled (2.0)
- `vertical-boost`: Upward momentum (0.3)
- `hook-velocity-multiplier`: Hook speed multiplier (2.0)
- `play-sound`: Enable/disable sound effects (true)
- `show-particles`: Enable/disable particle effects (true)

## Building

1. Ensure you have Maven installed
2. Run: `mvn clean package`
3. Find the compiled JAR in the `target/` folder
4. Place the JAR in your server's `plugins/` folder
5. Restart your server

## Usage

1. Players use `/hookshot toggle` to enable the grappling hook
2. Right-click with a fishing rod to launch the hook
3. When the hook lands, you'll be pulled to that location
4. Adjust range with `/hookshot range <value>` (e.g., `/hookshot range 50`)
5. Check settings with `/hookshot info`

## Requirements

- Spigot/Paper 1.19+
- Java 17+

## Support

For issues or questions, please refer to the plugin documentation or contact the server administrator.
