package com.hookshot.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HookshotSettingsTest {

    private static final Logger LOGGER = Logger.getLogger("HookshotSettingsTest");

    @Test
    void bundledConfigLoadsWithExpectedDefaults() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        HookshotSettings settings = HookshotSettings.load(yaml, LOGGER, m -> true, m -> true);

        assertEquals(40.0, settings.range());
        assertEquals(2, settings.maxHooks());
        assertEquals(100.0, settings.gasCapacity());
        assertTrue(settings.releaseOnSneakRelease());
        assertEquals(0.8, settings.reelSpeed());
        assertEquals(0.3, settings.reelAcceleration());
        assertEquals(1.25, settings.groundSlackDrop());
        assertEquals(1.0, settings.burstRedirect());
        assertTrue(settings.recipeEnabled());
        assertEquals(3, settings.recipeShape().size());
        // every character used in the shape has an ingredient
        settings.recipeShape().forEach(row -> row.chars().filter(c -> c != ' ')
                .forEach(c -> assertTrue(settings.recipeIngredients().containsKey((char) c),
                        "missing ingredient for " + (char) c)));
    }

    @Test
    void outOfRangeValuesAreClamped() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("hooks.range", 9999.0);
        yaml.set("hooks.max-hooks", 0);
        yaml.set("movement.max-speed", 100.0);
        yaml.set("combat.fall-damage-multiplier", -3.0);
        HookshotSettings settings = HookshotSettings.load(yaml, LOGGER, m -> true, m -> true);

        assertEquals(128.0, settings.range());
        assertEquals(1, settings.maxHooks());
        assertEquals(8.0, settings.maxSpeed());
        assertEquals(0.0, settings.fallDamageMultiplier());
    }
}
