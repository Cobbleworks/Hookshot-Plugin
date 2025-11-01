package com.hookshot.listeners;

import com.hookshot.HookshotPlugin;
import com.hookshot.managers.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

public class FishingRodListener implements Listener {

    private final HookshotPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public FishingRodListener(HookshotPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof FishHook)) {
            return;
        }

        FishHook hook = (FishHook) event.getEntity();
        
        if (!(hook.getShooter() instanceof Player)) {
            return;
        }

        Player player = (Player) hook.getShooter();
        
        // Check if hookshot is enabled for this player
        if (!playerDataManager.isHookshotEnabled(player)) {
            return;
        }

        // Check permission
        if (!player.hasPermission("hookshot.use")) {
            return;
        }

        // Get player's custom range
        double range = playerDataManager.getRange(player);
        
        // Increase the velocity of the fishing hook for extended range
        Vector direction = player.getLocation().getDirection();
        double velocityMultiplier = plugin.getConfig().getDouble("hook-velocity-multiplier", 2.0);
        hook.setVelocity(direction.multiply(velocityMultiplier));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishHook)) {
            return;
        }

        FishHook hook = (FishHook) event.getEntity();
        
        if (!(hook.getShooter() instanceof Player)) {
            return;
        }

        Player player = (Player) hook.getShooter();
        
        // Check if hookshot is enabled for this player
        if (!playerDataManager.isHookshotEnabled(player)) {
            return;
        }

        // Check permission
        if (!player.hasPermission("hookshot.use")) {
            return;
        }

        // Get the location where the hook landed
        Location hookLocation = hook.getLocation();
        Location playerLocation = player.getLocation();
        
        // Check if within range
        double distance = hookLocation.distance(playerLocation);
        double maxRange = playerDataManager.getRange(player);
        
        if (distance > maxRange) {
            player.sendMessage(ChatColor.RED + "Hook too far! Maximum range: " + maxRange + " blocks");
            return;
        }

        // Calculate the direction vector from player to hook
        Vector direction = hookLocation.toVector().subtract(playerLocation.toVector());
        
        // Normalize and multiply by pull speed
        double pullSpeed = plugin.getConfig().getDouble("pull-speed", 2.0);
        direction.normalize().multiply(pullSpeed);
        
        // Add upward momentum to prevent getting stuck in ground
        double verticalBoost = plugin.getConfig().getDouble("vertical-boost", 0.3);
        direction.setY(direction.getY() + verticalBoost);
        
        // Apply velocity to player
        player.setVelocity(direction);
        
        // Optional: Play sound effect
        if (plugin.getConfig().getBoolean("play-sound", true)) {
            player.getWorld().playSound(playerLocation, org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        }
        
        // Optional: Show particles
        if (plugin.getConfig().getBoolean("show-particles", true)) {
            player.getWorld().spawnParticle(
                org.bukkit.Particle.CLOUD,
                playerLocation,
                20,
                0.5, 0.5, 0.5,
                0.05
            );
        }
    }
}
