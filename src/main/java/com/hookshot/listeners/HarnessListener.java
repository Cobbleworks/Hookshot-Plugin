package com.hookshot.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.hookshot.config.HookshotSettings;
import com.hookshot.grapple.GrappleManager;
import com.hookshot.harness.HarnessItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Maps player input and lifecycle events onto the grapple manager. */
public final class HarnessListener implements Listener {

    /** Teleports shorter than this (e.g. server position corrections) keep the hooks. */
    private static final double TELEPORT_RELEASE_DISTANCE = 4.0;

    private final GrappleManager grapple;
    private final HarnessItem harness;

    public HarnessListener(GrappleManager grapple, HarnessItem harness) {
        this.grapple = grapple;
        this.harness = harness;
    }

    // ------------------------------------------------------------------ controls

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (grapple.canUse(player)) {
            event.setCancelled(true);
            grapple.fire(player);
        }
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        grapple.handleInput(event.getPlayer(), event.getInput());
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        // Flight is only enabled to stop anti-fly kicks while hanging; never let it be used.
        if (event.isFlying() && grapple.isFlightManaged(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.LEGS) {
            return;
        }
        Player player = event.getPlayer();
        if (harness.isHarness(event.getNewItem()) && !harness.isHarness(event.getOldItem())
                && player.hasPermission("hookshot.use")) {
            player.sendMessage(Component.text("Harness on. ", NamedTextColor.GOLD)
                    .append(Component.text("F", NamedTextColor.YELLOW))
                    .append(Component.text(" fire hook  ", NamedTextColor.GRAY))
                    .append(Component.text("Sneak", NamedTextColor.YELLOW))
                    .append(Component.text(" reel  ", NamedTextColor.GRAY))
                    .append(Component.text("Jump", NamedTextColor.YELLOW))
                    .append(Component.text(" let go / burst  ", NamedTextColor.GRAY))
                    .append(Component.text("WASD", NamedTextColor.YELLOW))
                    .append(Component.text(" steer", NamedTextColor.GRAY)));
        }
    }

    // ------------------------------------------------------------------ combat

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)
                || !harness.isWearing(player)) {
            return;
        }
        double multiplier = grapple.settings().fallDamageMultiplier();
        if (multiplier <= 0) {
            event.setCancelled(true);
        } else {
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMomentumHit(EntityDamageByEntityEvent event) {
        HookshotSettings settings = grapple.settings();
        if (!settings.momentumDamage() || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || !(event.getDamager() instanceof Player player) || !grapple.canUse(player)) {
            return;
        }
        double excess = grapple.currentSpeed(player) - settings.momentumSpeedThreshold();
        if (excess <= 0) {
            return;
        }
        double bonus = Math.min(settings.momentumMaxBonus(), excess * settings.momentumBonusPerSpeed());
        event.setDamage(event.getDamage() * (1.0 + bonus));

        Entity victim = event.getEntity();
        Location at = victim.getLocation().add(0, victim.getHeight() * 0.5, 0);
        if (settings.sounds()) {
            at.getWorld().playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        }
        if (settings.particles()) {
            at.getWorld().spawnParticle(Particle.SWEEP_ATTACK, at, 1);
            at.getWorld().spawnParticle(Particle.CRIT, at, 14, 0.3, 0.3, 0.3, 0.3);
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        grapple.repairFlightOnJoin(player);
        if (harness.isRecipeRegistered()) {
            player.discoverRecipe(harness.recipeKey());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        grapple.remove(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        grapple.releaseAll(event.getPlayer());
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        grapple.releaseAll(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (!from.getWorld().equals(to.getWorld()) || from.distanceSquared(to) > TELEPORT_RELEASE_DISTANCE * TELEPORT_RELEASE_DISTANCE) {
            grapple.releaseAll(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        grapple.onGameModeChange(event.getPlayer(), event.getNewGameMode());
    }
}
