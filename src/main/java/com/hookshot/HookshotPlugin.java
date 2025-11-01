package com.hookshot;

import com.hookshot.commands.HookshotCommand;
import com.hookshot.listeners.FishingRodListener;
import com.hookshot.managers.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HookshotPlugin extends JavaPlugin {

    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        
        // Register commands
        getCommand("hookshot").setExecutor(new HookshotCommand(this, playerDataManager));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new FishingRodListener(this, playerDataManager), this);
        
        getLogger().info("Hookshot plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save player data
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        
        getLogger().info("Hookshot plugin has been disabled!");
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
