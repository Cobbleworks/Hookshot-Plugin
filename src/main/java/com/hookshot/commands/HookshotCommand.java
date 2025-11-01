package com.hookshot.commands;

import com.hookshot.HookshotPlugin;
import com.hookshot.managers.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HookshotCommand implements CommandExecutor {

    private final HookshotPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public HookshotCommand(HookshotPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("hookshot.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            // Toggle by default
            return toggleHookshot(player);
        }

        switch (args[0].toLowerCase()) {
            case "toggle":
                return toggleHookshot(player);
                
            case "range":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /hookshot range <value>");
                    return true;
                }
                return setRange(player, args[1]);
                
            case "info":
                return showInfo(player);
                
            default:
                player.sendMessage(ChatColor.RED + "Usage: /hookshot [toggle|range <value>|info]");
                return true;
        }
    }

    private boolean toggleHookshot(Player player) {
        boolean enabled = playerDataManager.toggleHookshot(player);
        
        if (enabled) {
            player.sendMessage(ChatColor.GREEN + "✓ Grappling hook enabled!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "✗ Grappling hook disabled!");
        }
        
        return true;
    }

    private boolean setRange(Player player, String rangeStr) {
        if (!player.hasPermission("hookshot.range")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to change your hook range!");
            return true;
        }

        try {
            double range = Double.parseDouble(rangeStr);
            
            double minRange = plugin.getConfig().getDouble("min-range", 10.0);
            double maxRange = plugin.getConfig().getDouble("max-range", 100.0);
            
            if (range < minRange || range > maxRange) {
                player.sendMessage(ChatColor.RED + "Range must be between " + minRange + " and " + maxRange + "!");
                return true;
            }
            
            playerDataManager.setRange(player, range);
            player.sendMessage(ChatColor.GREEN + "Hook range set to " + range + " blocks!");
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid range value! Please enter a number.");
        }
        
        return true;
    }

    private boolean showInfo(Player player) {
        boolean enabled = playerDataManager.isHookshotEnabled(player);
        double range = playerDataManager.getRange(player);
        
        player.sendMessage(ChatColor.GOLD + "=== Hookshot Info ===");
        player.sendMessage(ChatColor.YELLOW + "Status: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        player.sendMessage(ChatColor.YELLOW + "Range: " + ChatColor.WHITE + range + " blocks");
        player.sendMessage(ChatColor.YELLOW + "Pull Speed: " + ChatColor.WHITE + plugin.getConfig().getDouble("pull-speed", 2.0));
        
        return true;
    }
}
