package com.hookshot.config;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Immutable snapshot of config.yml. Values are clamped to safe ranges so a typo
 * cannot launch players into orbit.
 */
public record HookshotSettings(
        Material harnessMaterial,
        Color harnessColor,
        String harnessName,
        boolean recipeEnabled,
        List<String> recipeShape,
        Map<Character, Material> recipeIngredients,
        double range,
        int maxHooks,
        double hookTravelSpeed,
        int fireCooldownTicks,
        boolean hookEntities,
        boolean hookPlayers,
        int autoRetractGroundTicks,
        boolean releaseOnSneakRelease,
        double minRopeLength,
        double reelSpeed,
        double reelAcceleration,
        double groundSlackDrop,
        Material ropeBlock,
        float ropeWidth,
        double airDrag,
        double steerAcceleration,
        double maxSpeed,
        int momentumTicks,
        double gasCapacity,
        double reelGasCost,
        boolean burstEnabled,
        double burstGasCost,
        double burstPower,
        int burstCooldownTicks,
        double gasRegenPerTick,
        int gasRegenDelayTicks,
        boolean momentumDamage,
        double momentumSpeedThreshold,
        double momentumBonusPerSpeed,
        double momentumMaxBonus,
        double fallDamageMultiplier,
        boolean sounds,
        boolean particles) {

    /** Longest a rope may get, relative to the firing range. */
    public double maxRopeLength() {
        return range * 1.25;
    }

    public static HookshotSettings load(FileConfiguration config, Logger logger) {
        return load(config, logger, Material::isBlock, Material::isItem);
    }

    /**
     * Loads settings with custom material checks. {@link Material#isBlock()} needs a running
     * server, so tests pass lenient checks instead.
     */
    static HookshotSettings load(FileConfiguration config, Logger logger, Predicate<Material> isBlock,
                                 Predicate<Material> isItem) {
        Material harnessMaterial = material(config, "harness.material", Material.LEATHER_LEGGINGS, logger, isItem);
        Material ropeBlock = material(config, "rope.block", Material.GRAY_CONCRETE, logger, isBlock);

        return new HookshotSettings(
                harnessMaterial,
                color(config.getString("harness.color", "#3B2F2F"), logger),
                config.getString("harness.name", "Grapple Harness"),
                config.getBoolean("recipe.enabled", true),
                List.copyOf(config.getStringList("recipe.shape")),
                ingredients(config.getConfigurationSection("recipe.ingredients"), logger, isItem),
                clamp(config.getDouble("hooks.range", 40.0), 5.0, 128.0),
                (int) clamp(config.getInt("hooks.max-hooks", 2), 1, 4),
                clamp(config.getDouble("hooks.travel-speed", 4.0), 0.5, 16.0),
                (int) clamp(config.getInt("hooks.fire-cooldown-ticks", 4), 0, 200),
                config.getBoolean("hooks.hook-entities", true),
                config.getBoolean("hooks.hook-players", false),
                (int) clamp(config.getInt("hooks.auto-retract-ground-ticks", 40), 0, 72000),
                config.getBoolean("controls.release-on-sneak-release", true),
                clamp(config.getDouble("rope.min-length", 1.5), 0.5, 10.0),
                clamp(config.getDouble("rope.reel-speed", 0.6), 0.05, 5.0),
                clamp(config.getDouble("rope.reel-acceleration", 0.15), 0.0, 2.0),
                clamp(config.getDouble("rope.ground-slack-drop", 1.25), 0.0, 16.0),
                ropeBlock,
                (float) clamp(config.getDouble("rope.width", 0.05), 0.01, 0.5),
                clamp(config.getDouble("movement.air-drag", 0.995), 0.8, 1.0),
                clamp(config.getDouble("movement.steer-acceleration", 0.04), 0.0, 0.5),
                clamp(config.getDouble("movement.max-speed", 3.0), 0.5, 8.0),
                (int) clamp(config.getInt("movement.momentum-ticks", 40), 0, 400),
                clamp(config.getDouble("gas.capacity", 100.0), 1.0, 100000.0),
                clamp(config.getDouble("gas.reel-cost", 0.5), 0.0, 1000.0),
                config.getBoolean("gas.burst-enabled", true),
                clamp(config.getDouble("gas.burst-cost", 20.0), 0.0, 100000.0),
                clamp(config.getDouble("gas.burst-power", 1.1), 0.0, 5.0),
                (int) clamp(config.getInt("gas.burst-cooldown-ticks", 10), 0, 1200),
                clamp(config.getDouble("gas.regen-per-tick", 1.0), 0.0, 100000.0),
                (int) clamp(config.getInt("gas.regen-delay-ticks", 20), 0, 72000),
                config.getBoolean("combat.momentum-damage", true),
                clamp(config.getDouble("combat.speed-threshold", 0.7), 0.0, 10.0),
                clamp(config.getDouble("combat.bonus-per-speed", 0.6), 0.0, 10.0),
                clamp(config.getDouble("combat.max-bonus", 1.0), 0.0, 10.0),
                clamp(config.getDouble("combat.fall-damage-multiplier", 0.5), 0.0, 1.0),
                config.getBoolean("effects.sounds", true),
                config.getBoolean("effects.particles", true));
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Material material(FileConfiguration config, String path, Material fallback, Logger logger,
                                     Predicate<Material> valid) {
        String name = config.getString(path, fallback.name());
        Material material = Material.matchMaterial(name);
        if (material == null || !valid.test(material)) {
            logger.warning("Invalid material '" + name + "' at " + path + "; using " + fallback.name());
            return fallback;
        }
        return material;
    }

    private static Color color(String hex, Logger logger) {
        String value = hex == null ? "" : hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            return Color.fromRGB(Integer.parseInt(value, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            logger.warning("Invalid harness.color '" + hex + "'; using #3B2F2F");
            return Color.fromRGB(0x3B2F2F);
        }
    }

    private static Map<Character, Material> ingredients(ConfigurationSection section, Logger logger,
                                                         Predicate<Material> isItem) {
        if (section == null) {
            return Map.of();
        }
        Map<Character, Material> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (key.length() != 1 || key.charAt(0) == ' ') {
                logger.warning("Recipe ingredient keys must be a single non-space character: '" + key + "'");
                continue;
            }
            String name = section.getString(key, "");
            Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (material == null || !isItem.test(material)) {
                logger.warning("Invalid recipe ingredient '" + name + "' for key " + key);
                continue;
            }
            result.put(key.charAt(0), material);
        }
        return Collections.unmodifiableMap(result);
    }
}
