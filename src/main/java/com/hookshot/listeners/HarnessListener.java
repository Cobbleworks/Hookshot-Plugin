package com.hookshot.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.hookshot.config.HookshotSettings;
import com.hookshot.grapple.GrappleManager;
import com.hookshot.harness.HarnessItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Maps player input and lifecycle events onto the grapple manager. */
public final class HarnessListener implements Listener {

    /** Teleports shorter than this (e.g. server position corrections) keep the hooks. */
    private static final double TELEPORT_RELEASE_DISTANCE = 4.0;

    /** Right-clicks closer together than this are treated as the button being held. */
    private static final int HOLD_REPEAT_TICKS = 5;

    private final GrappleManager grapple;
    private final HarnessItem harness;
    private final Map<UUID, Integer> lastRightClick = new HashMap<>();

    public HarnessListener(GrappleManager grapple, HarnessItem harness) {
        this.grapple = grapple;
        this.harness = harness;
    }

    // ------------------------------------------------------------------ controls

    /**
     * Right-click with a sword fires a hook. Not ignoreCancelled: Bukkit fires RIGHT_CLICK_AIR
     * already cancelled. Holding the button auto-repeats every 4 ticks, so a click that follows the
     * previous one within {@link #HOLD_REPEAT_TICKS} counts as a hold and is ignored.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!Tag.ITEMS_SWORDS.isTagged(player.getInventory().getItemInMainHand().getType())
                || !grapple.canUse(player)) {
            return;
        }
        // Block the click from opening/using the block or the off-hand item.
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        int now = Bukkit.getCurrentTick();
        Integer last = lastRightClick.put(player.getUniqueId(), now);
        if (last != null && now - last <= HOLD_REPEAT_TICKS) {
            return;
        }
        grapple.fire(player);
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
                    .append(Component.text("Right-click with sword", NamedTextColor.YELLOW))
                    .append(Component.text(" fire hook  ", NamedTextColor.GRAY))
                    .append(Component.text("Sneak", NamedTextColor.YELLOW))
                    .append(Component.text(grapple.settings().releaseOnSneakRelease()
                            ? " hold to reel, release to let go  " : " reel  ", NamedTextColor.GRAY))
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
            if (!Tag.ITEMS_SWORDS.isTagged(player.getInventory().getItemInMainHand().getType())) {
                at.getWorld().spawnParticle(Particle.SWEEP_ATTACK, at, 1);
            }
            at.getWorld().spawnParticle(Particle.CRIT, at, 14, 0.3, 0.3, 0.3, 0.3);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwordHit(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || event.getFinalDamage() <= 0
                || !(event.getDamager() instanceof Player player) || !grapple.canUse(player)
                || !Tag.ITEMS_SWORDS.isTagged(player.getInventory().getItemInMainHand().getType())) {
            return;
        }
        HookshotSettings settings = grapple.settings();
        Entity victim = event.getEntity();
        Location at = victim.getLocation().add(0, victim.getHeight() * 0.5, 0);
        if (settings.particles()) {
            at.getWorld().spawnParticle(Particle.SWEEP_ATTACK, at, 1);
        }
        if (settings.sounds()) {
            at.getWorld().playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.35f);
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
        lastRightClick.remove(event.getPlayer().getUniqueId());
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
