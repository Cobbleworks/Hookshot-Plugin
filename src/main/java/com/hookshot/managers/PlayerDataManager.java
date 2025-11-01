package com.hookshot.managers;

import com.hookshot.HookshotPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final HookshotPlugin plugin;
    private final Map<UUID, Boolean> hookshotEnabled;
    private final Map<UUID, Double> playerRanges;
    private File dataFile;
    private FileConfiguration dataConfig;

    public PlayerDataManager(HookshotPlugin plugin) {
        this.plugin = plugin;
        this.hookshotEnabled = new HashMap<>();
        this.playerRanges = new HashMap<>();
        
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create playerdata.yml!");
                e.printStackTrace();
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load player data from file
        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                boolean enabled = dataConfig.getBoolean("players." + uuidStr + ".enabled", false);
                double range = dataConfig.getDouble("players." + uuidStr + ".range", 
                    plugin.getConfig().getDouble("default-range", 30.0));
                
                hookshotEnabled.put(uuid, enabled);
                playerRanges.put(uuid, range);
            }
        }
    }

    public void saveAllData() {
        for (UUID uuid : hookshotEnabled.keySet()) {
            String uuidStr = uuid.toString();
            dataConfig.set("players." + uuidStr + ".enabled", hookshotEnabled.get(uuid));
            dataConfig.set("players." + uuidStr + ".range", playerRanges.get(uuid));
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }

    public boolean isHookshotEnabled(Player player) {
        return hookshotEnabled.getOrDefault(player.getUniqueId(), false);
    }

    public boolean toggleHookshot(Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentState = hookshotEnabled.getOrDefault(uuid, false);
        boolean newState = !currentState;
        
        hookshotEnabled.put(uuid, newState);
        
        // Initialize default range if not set
        if (!playerRanges.containsKey(uuid)) {
            playerRanges.put(uuid, plugin.getConfig().getDouble("default-range", 30.0));
        }
        
        saveAllData();
        return newState;
    }

    public double getRange(Player player) {
        return playerRanges.getOrDefault(player.getUniqueId(), 
            plugin.getConfig().getDouble("default-range", 30.0));
    }

    public void setRange(Player player, double range) {
        playerRanges.put(player.getUniqueId(), range);
        saveAllData();
    }
}
