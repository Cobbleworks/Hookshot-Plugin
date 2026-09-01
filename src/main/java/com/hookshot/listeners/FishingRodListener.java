package com.hookshot.listeners;

import com.hookshot.HookshotPlugin;
import com.hookshot.managers.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Converts valid fishing-hook impacts into bounded player or entity pulls.
 */
public class FishingRodListener implements Listener {

    private final HookshotPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Long> lastPullTime;
    private static final long PULL_COOLDOWN_MS = 250; // Minimum time between pulls

    public FishingRodListener(HookshotPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.lastPullTime = new HashMap<>();
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

        // Increase the velocity of the fishing hook using player's custom setting
        Vector direction = player.getLocation().getDirection();
        double velocityMultiplier = playerDataManager.getHookVelocity(player);
        hook.setVelocity(direction.multiply(velocityMultiplier));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // This event fires immediately when hook hits a block
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

        // Only pull if we hit a block (not an entity - entity handled by PlayerFishEvent)
        if (event.getHitBlock() == null) {
            return;
        }
        
        // Cooldown check to prevent double pulling
        if (!checkCooldown(player)) {
            return;
        }

        Location hookLocation = hook.getLocation();
        Location playerLocation = player.getLocation();
        
        // Check if within range
        double distance = hookLocation.distance(playerLocation);
        double maxRange = playerDataManager.getRange(player);
        
        if (distance > maxRange) {
            player.sendMessage(ChatColor.RED + "Hook too far! Maximum range: " + maxRange + " blocks");
            return;
        }

        // Pull player to the hook location immediately
        pullPlayerToHook(player, hookLocation);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        // This handles entity catching
        Player player = event.getPlayer();
        
        // Check if hookshot is enabled for this player
        if (!playerDataManager.isHookshotEnabled(player)) {
            return;
        }

        // Check permission
        if (!player.hasPermission("hookshot.use")) {
            return;
        }

        // Only handle CAUGHT_ENTITY state for pulling entities
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        
        // Cooldown check to prevent double pulling
        if (!checkCooldown(player)) {
            return;
        }

        Entity caughtEntity = event.getCaught();
        
        if (caughtEntity != null && caughtEntity instanceof LivingEntity && !(caughtEntity instanceof Player)) {
            // Check range first
            Location playerLocation = player.getLocation();
            double distance = caughtEntity.getLocation().distance(playerLocation);
            double maxRange = playerDataManager.getRange(player);
            
            if (distance > maxRange) {
                player.sendMessage(ChatColor.RED + "Entity too far! Maximum range: " + maxRange + " blocks");
                return;
            }
            
            // Pull the entity towards the player
            pullEntityToPlayer(player, (LivingEntity) caughtEntity);
        }
    }
    
    private boolean checkCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (lastPullTime.containsKey(playerId)) {
            if (currentTime - lastPullTime.get(playerId) < PULL_COOLDOWN_MS) {
                return false;
            }
        }
        lastPullTime.put(playerId, currentTime);
        return true;
    }
    
    private void pullEntityToPlayer(Player player, LivingEntity entity) {
        Location playerLocation = player.getLocation();
        Location entityLocation = entity.getLocation();
        
        // Calculate direction from entity to player
        Vector direction = playerLocation.toVector().subtract(entityLocation.toVector());
        
        // Get pull speed and apply enhanced force (1.5x multiplier for entities)
        double pullSpeed = playerDataManager.getPullSpeed(player) * 1.5;
        direction.normalize().multiply(pullSpeed);
        
        // Add some vertical boost to make it more dramatic
        double verticalBoost = playerDataManager.getVerticalBoost(player) * 1.2;
        direction.setY(direction.getY() + verticalBoost);
        
        // Apply velocity to the entity
        entity.setVelocity(direction);
        
        // Play sound at entity location
        if (playerDataManager.isSoundEnabled(player)) {
            entity.getWorld().playSound(entityLocation, org.bukkit.Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 0.8f);
        }
        
        // Show particles at entity location
        if (playerDataManager.isParticlesEnabled(player)) {
            entity.getWorld().spawnParticle(
                org.bukkit.Particle.CRIT,
                entityLocation.add(0, entity.getHeight() / 2, 0),
                15,
                0.3, 0.3, 0.3,
                0.1
            );
        }
    }
    
    private void pullPlayerToHook(Player player, Location hookLocation) {
        Location playerLocation = player.getLocation();
        
        // Calculate the direction vector from player to hook
        Vector direction = hookLocation.toVector().subtract(playerLocation.toVector());
        
        // Normalize and multiply by player's custom pull speed
        double pullSpeed = playerDataManager.getPullSpeed(player);
        direction.normalize().multiply(pullSpeed);
        
        // Add upward momentum using player's custom vertical boost
        double verticalBoost = playerDataManager.getVerticalBoost(player);
        direction.setY(direction.getY() + verticalBoost);
        
        // Apply velocity to player
        player.setVelocity(direction);
        
        // Optional: Play sound effect (per-player setting)
        if (playerDataManager.isSoundEnabled(player)) {
            player.getWorld().playSound(playerLocation, org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        }
        
        // Optional: Show particles (per-player setting)
        if (playerDataManager.isParticlesEnabled(player)) {
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
