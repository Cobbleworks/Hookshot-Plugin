package com.hookshot;

import com.hookshot.commands.HookshotCommand;
import com.hookshot.config.HookshotSettings;
import com.hookshot.grapple.GrappleManager;
import com.hookshot.harness.HarnessItem;
import com.hookshot.listeners.HarnessListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hookshot: a wearable grapple harness with rope-swing physics.
 */
public final class HookshotPlugin extends JavaPlugin {

    private HookshotSettings settings;
    private HarnessItem harness;
    private GrappleManager grapple;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = HookshotSettings.load(getConfig(), getLogger());

        harness = new HarnessItem(this, settings);
        harness.apply(settings);

        grapple = new GrappleManager(this, harness, settings);
        grapple.start();

        getServer().getPluginManager().registerEvents(new HarnessListener(grapple, harness), this);

        HookshotCommand command = new HookshotCommand(this, harness);
        PluginCommand pluginCommand = getCommand("hookshot");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // Players already online after a reload still need flight repairs and the recipe.
        getServer().getOnlinePlayers().forEach(player -> {
            grapple.repairFlightOnJoin(player);
            if (harness.isRecipeRegistered()) {
                player.discoverRecipe(harness.recipeKey());
            }
        });
    }

    @Override
    public void onDisable() {
        if (grapple != null) {
            grapple.shutdown();
        }
        if (harness != null) {
            harness.unregisterRecipe();
        }
    }

    /** Re-reads config.yml and applies it without a restart. */
    public void reload() {
        reloadConfig();
        settings = HookshotSettings.load(getConfig(), getLogger());
        harness.apply(settings);
        grapple.applySettings(settings);
        if (harness.isRecipeRegistered()) {
            getServer().getOnlinePlayers().forEach(player -> player.discoverRecipe(harness.recipeKey()));
        }
    }

    public HookshotSettings settings() {
        return settings;
    }
}
