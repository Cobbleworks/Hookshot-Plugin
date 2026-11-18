package com.hookshot.grapple;

import com.hookshot.config.HookshotSettings;
import com.hookshot.harness.HarnessItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the grapple simulation. Every tick, for each player wearing a harness:
 * <ol>
 *   <li>measure the player's real movement from position changes,</li>
 *   <li>advance flying hooks and validate anchors,</li>
 *   <li>while hooked or carrying momentum, integrate velocity on the server (gravity,
 *       low drag, steering, reeling, rope constraint, collisions) and send it,</li>
 *   <li>render ropes and the gas HUD.</li>
 * </ol>
 * Sending the velocity every tick, with low drag, lets swings keep their speed instead
 * of the strong vanilla air friction slowing them down.
 */
public final class GrappleManager {

    /** Extra distance an anchored entity may pull away before the rope snaps. */
    private static final double SNAP_SLACK = 8.0;
    private static final int HUD_LINGER_TICKS = 40;
    private static final int HUD_SEGMENTS = 20;

    private final Plugin plugin;
    private final HarnessItem harness;
    private final NamespacedKey managedFlightKey;
    private final Map<UUID, GrappleState> states = new HashMap<>();
    private HookshotSettings settings;
    private BlockData ropeBlockData;
    private BukkitTask task;
    private long tickCounter;

    public GrappleManager(Plugin plugin, HarnessItem harness, HookshotSettings settings) {
        this.plugin = plugin;
        this.harness = harness;
        this.managedFlightKey = new NamespacedKey(plugin, "managed_flight");
        applySettings(settings);
    }

    public void applySettings(HookshotSettings newSettings) {
        this.settings = newSettings;
        this.ropeBlockData = newSettings.ropeBlock().createBlockData();
        for (GrappleState state : states.values()) {
            state.gas = Math.min(state.gas, newSettings.gasCapacity());
        }
    }

    public HookshotSettings settings() {
        return settings;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            remove(player);
        }
        states.clear();
    }

    // ------------------------------------------------------------------ public hooks for listeners

    public boolean canUse(Player player) {
        return player.getGameMode() != GameMode.SPECTATOR
                && !player.isDead()
                && !player.isInsideVehicle()
                && player.hasPermission("hookshot.use")
                && harness.isWearing(player);
    }

    /** Fires a hook where the player is aiming (Swap Hands key). */
    public void fire(Player player) {
        GrappleState state = state(player);
        if (state.fireCooldown > 0) {
            return;
        }
        state.fireCooldown = settings.fireCooldownTicks();

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        Vector hip = hip(player);
        RayTraceResult hit = world.rayTrace(eye, direction, settings.range(), FluidCollisionMode.NEVER, true, 0.2,
                entity -> canHookEntity(player, entity));
        RopeRenderer rope = new RopeRenderer(world, ropeBlockData, settings.ropeWidth());
        sound(eye, Sound.ITEM_CROSSBOW_SHOOT, 0.7f, 1.5f);

        if (hit == null) {
            Vector end = eye.toVector().add(direction.multiply(settings.range()));
            state.hooks.add(Hook.miss(world, hip, end, rope));
            return;
        }
        while (state.hookCount() >= settings.maxHooks()) {
            Hook oldest = state.oldestHook();
            oldest.remove();
            state.hooks.remove(oldest);
        }
        Vector hitPosition = hit.getHitPosition();
        Hook hook = hit.getHitEntity() != null
                ? Hook.toEntity(world, hip, hitPosition, hit.getHitEntity(), rope)
                : Hook.toBlock(world, hip, hitPosition, hit.getHitBlock(), rope);
        state.hooks.add(hook);
    }

    /**
     * Called for every input change. Releasing Sneak after reeling lets go; pressing Jump lets go
     * (on the ground too), or bursts in mid-air when no hooks are out.
     */
    public void handleInput(Player player, Input input) {
        GrappleState state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        boolean jump = input.isJump();
        boolean jumpPressed = jump && !state.lastJumpInput;
        state.lastJumpInput = jump;
        boolean sneak = input.isSneak();
        boolean sneakPressed = sneak && !state.lastSneakInput;
        boolean sneakReleased = !sneak && state.lastSneakInput;
        state.lastSneakInput = sneak;
        if (!canUse(player)) {
            state.sneakArmed = false;
            return;
        }

        if (sneakPressed && state.hasAttachedHook()) {
            state.sneakArmed = true;
        }
        if (sneakReleased) {
            boolean armed = state.sneakArmed;
            state.sneakArmed = false;
            if (armed && settings.releaseOnSneakRelease() && state.hasHooks()) {
                letGo(player, state);
                return;
            }
        }

        if (!jumpPressed || player.isFlying()) {
            return;
        }
        if (state.hasHooks()) {
            letGo(player, state);
        } else if (!isGrounded(player)) {
            burst(player, state);
        }
    }

    /**
     * Player-initiated release. In mid-air the swing's momentum carries on; from the ground or a
     * slack rope the player just keeps vanilla movement, so a normal jump stays a normal jump.
     */
    private void letGo(Player player, GrappleState state) {
        boolean slack = state.ropeSlack || isGrounded(player);
        releaseAll(player, state, true);
        if (slack && state.mode == GrappleState.Mode.HOOKED) {
            state.mode = GrappleState.Mode.IDLE;
            state.commanding = false;
        }
    }

    /** Releases all hooks, e.g. on death or teleport. */
    public void releaseAll(Player player) {
        GrappleState state = states.get(player.getUniqueId());
        if (state != null) {
            releaseAll(player, state, false);
            state.mode = GrappleState.Mode.IDLE;
            state.commanding = false;
        }
    }

    /** Forgets the player completely and restores any flight ability changes. */
    public void remove(Player player) {
        GrappleState state = states.remove(player.getUniqueId());
        if (state != null) {
            releaseAll(player, state, false);
            restoreFlight(player, state);
        }
    }

    public boolean isFlightManaged(Player player) {
        GrappleState state = states.get(player.getUniqueId());
        return state != null && state.managedFlight;
    }

    /** A game mode change sets flight abilities itself; stop managing them. */
    public void onGameModeChange(Player player, GameMode newMode) {
        GrappleState state = states.get(player.getUniqueId());
        if (state != null && state.managedFlight) {
            state.managedFlight = false;
            player.getPersistentDataContainer().remove(managedFlightKey);
        }
        if (newMode == GameMode.SPECTATOR) {
            releaseAll(player);
        }
    }

    /** Undoes flight that was left enabled if the server stopped while the player was hooked. */
    public void repairFlightOnJoin(Player player) {
        if (!player.getPersistentDataContainer().has(managedFlightKey)) {
            return;
        }
        player.getPersistentDataContainer().remove(managedFlightKey);
        if (isSurvivalLike(player.getGameMode())) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    /** Speed in blocks per tick, as used for momentum damage. */
    public double currentSpeed(Player player) {
        GrappleState state = states.get(player.getUniqueId());
        return state == null ? 0.0 : state.currentVelocity().length();
    }

    // ------------------------------------------------------------------ simulation

    private void tick() {
        tickCounter++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            GrappleState state = states.get(player.getUniqueId());
            if (!canUse(player)) {
                if (state != null && (!state.hooks.isEmpty() || state.mode != GrappleState.Mode.IDLE)) {
                    releaseAll(player, state, false);
                    state.mode = GrappleState.Mode.IDLE;
                    state.commanding = false;
                    restoreFlight(player, state);
                }
                continue;
            }
            if (state == null) {
                state = state(player);
            }
            tickPlayer(player, state);
        }
    }

    private void tickPlayer(Player player, GrappleState state) {
        Location location = player.getLocation();
        World world = location.getWorld();
        Vector position = location.toVector();
        boolean continuous = state.lastPosition != null && state.lastTick == tickCounter - 1
                && world.getUID().equals(state.lastWorld);
        state.previousMeasuredVelocity = continuous ? state.measuredVelocity : new Vector();
        state.measuredVelocity = continuous ? position.clone().subtract(state.lastPosition) : new Vector();
        state.lastPosition = position;
        state.lastWorld = world.getUID();
        state.lastTick = tickCounter;

        boolean onGround = isGrounded(player);
        state.groundTicks = onGround ? state.groundTicks + 1 : 0;
        if (state.fireCooldown > 0) {
            state.fireCooldown--;
        }
        if (state.burstCooldown > 0) {
            state.burstCooldown--;
        }
        state.effectTicker++;

        Vector hip = hip(player);
        updateHooks(player, state, hip);

        Input input = player.getCurrentInput();
        boolean attached = state.hasAttachedHook();
        boolean wantsReel = attached && input.isSneak();
        boolean reeling = wantsReel && state.gas > 0;
        if (!input.isSneak()) {
            state.outOfGasWarned = false;
        } else if (wantsReel && !reeling && !state.outOfGasWarned) {
            state.outOfGasWarned = true;
            player.playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.8f);
        }
        if (wantsReel) {
            // Covers a hook landing while Sneak is already held: that reel also ends on release.
            state.sneakArmed = true;
        }
        updateSlack(state, position, onGround, reeling);

        if (attached) {
            state.mode = GrappleState.Mode.HOOKED;
            player.setFallDistance(0f);
            if (!onGround) {
                enableManagedFlight(player, state);
            }
            if (state.ropeSlack) {
                // The spool pays out freely, so walking around (and over steps) is never blocked.
                for (Hook hook : state.hooks) {
                    if (hook.isAttached()) {
                        hook.ropeLength = Math.max(settings.minRopeLength(), hip.distance(hook.anchor()));
                    }
                }
                state.commanding = false;
                state.groundedHookTicks++;
                if (settings.autoRetractGroundTicks() > 0
                        && state.groundedHookTicks >= settings.autoRetractGroundTicks()) {
                    releaseAll(player, state, true);
                    state.mode = GrappleState.Mode.IDLE;
                }
            } else {
                state.groundedHookTicks = 0;
                simulateHooked(player, state, hip, input, onGround, reeling);
            }
        } else {
            restoreFlight(player, state);
            if (state.mode == GrappleState.Mode.HOOKED) {
                // Hooks gone (released, replaced, or snapped): carry momentum if airborne.
                // A slack rope was never swinging the player, so there is no momentum to carry.
                state.mode = onGround || state.ropeSlack ? GrappleState.Mode.IDLE : GrappleState.Mode.FREE_FLIGHT;
                state.freeFlightTicks = 0;
                if (!state.commanding) {
                    state.simulatedVelocity = state.smoothedVelocity();
                }
            }
            if (state.mode == GrappleState.Mode.FREE_FLIGHT) {
                simulateFreeFlight(player, state, input, onGround);
            } else {
                state.commanding = false;
            }
        }

        regenerateGas(state, onGround, reeling);
        renderHooks(state, hip);
        updateHud(player, state);
    }

    /**
     * The rope is slack on the ground and stays slack after stepping off until the player drops
     * {@code rope.ground-slack-drop} below the takeoff height. Steps, stairs, and short hops
     * therefore stay vanilla movement, while running off a real ledge still starts a swing.
     * Reeling always tightens the rope.
     */
    private void updateSlack(GrappleState state, Vector position, boolean onGround, boolean reeling) {
        if (reeling) {
            state.ropeSlack = false;
        } else if (onGround) {
            state.ropeSlack = true;
            state.slackTakeoffY = position.getY();
        } else if (state.ropeSlack && position.getY() < state.slackTakeoffY - settings.groundSlackDrop()) {
            state.ropeSlack = false;
        }
    }

    /** Every anchored rope is at minimum length and the player is hanging at it. */
    private boolean isFullyReeled(GrappleState state, Vector hip) {
        boolean any = false;
        for (Hook hook : state.hooks) {
            if (!hook.isAttached()) {
                continue;
            }
            if (hook.ropeLength > settings.minRopeLength() + 1.0E-3
                    || hip.distance(hook.anchor()) > settings.minRopeLength() + 0.4) {
                return false;
            }
            any = true;
        }
        return any;
    }

    private void simulateHooked(Player player, GrappleState state, Vector hip, Input input, boolean onGround,
                                boolean reeling) {
        Vector velocity = state.commanding ? state.simulatedVelocity.clone() : state.smoothedVelocity();
        RopePhysics.applyGravityAndDrag(velocity, settings.airDrag());
        if (!onGround) {
            velocity.add(steering(player, input, settings.steerAcceleration()));
        }

        if (reeling) {
            // Holding Sneak to hang at full reel is free; only actually shortening a rope uses gas.
            boolean fullyReeled = isFullyReeled(state, hip);
            Vector pull = new Vector();
            for (Hook hook : state.hooks) {
                if (hook.isAttached()) {
                    pull.add(RopePhysics.direction(hip, hook.anchor()));
                    double distance = hip.distance(hook.anchor());
                    hook.ropeLength = Math.max(settings.minRopeLength(),
                            Math.min(hook.ropeLength - settings.reelSpeed(), distance));
                }
            }
            if (pull.lengthSquared() > 1.0E-6) {
                velocity.add(pull.normalize().multiply(settings.reelAcceleration()));
            }
            if (!fullyReeled) {
                state.gas = Math.max(0.0, state.gas - settings.reelGasCost());
                state.gasRegenDelay = settings.gasRegenDelayTicks();
                reelEffects(player, state, hip, velocity);
            }
        }

        boolean perched = false;
        for (Hook hook : state.hooks) {
            if (!hook.isAttached()) {
                continue;
            }
            RopePhysics.constrain(hip, hook.anchor(), hook.ropeLength, velocity, RopePhysics.MAX_CORRECTION);
            if (hook.ropeLength <= settings.minRopeLength() + 1.0E-3
                    && hip.distance(hook.anchor()) <= settings.minRopeLength() + 0.4) {
                perched = true;
            }
        }
        if (perched) {
            // Fully reeled in: damp motion so the player can hang on a wall or ceiling.
            velocity.multiply(0.6);
        }
        RopePhysics.clampSpeed(velocity, settings.maxSpeed());
        resolveCollisions(player, velocity);
        sendVelocity(player, state, velocity);
    }

    private void simulateFreeFlight(Player player, GrappleState state, Input input, boolean onGround) {
        state.freeFlightTicks++;
        boolean finished = onGround
                || state.freeFlightTicks > settings.momentumTicks()
                || player.isInWater() || player.isInLava()
                || player.isGliding() || player.isClimbing() || player.isFlying();
        if (finished) {
            state.mode = GrappleState.Mode.IDLE;
            state.commanding = false;
            return;
        }
        Vector velocity = state.commanding ? state.simulatedVelocity.clone() : state.smoothedVelocity();
        RopePhysics.applyGravityAndDrag(velocity, settings.airDrag());
        velocity.add(steering(player, input, settings.steerAcceleration() * 0.5));
        RopePhysics.clampSpeed(velocity, settings.maxSpeed());
        resolveCollisions(player, velocity);
        sendVelocity(player, state, velocity);
    }

    private void sendVelocity(Player player, GrappleState state, Vector velocity) {
        player.setVelocity(velocity);
        state.simulatedVelocity = velocity;
        state.commanding = true;
    }

    /** Zeroes each velocity axis that would move the player into a block next tick. */
    private void resolveCollisions(Player player, Vector velocity) {
        World world = player.getWorld();
        BoundingBox box = player.getBoundingBox().expand(-0.01);
        if (velocity.getY() != 0 && world.hasCollisionsIn(box.clone().shift(0, velocity.getY(), 0))) {
            velocity.setY(0);
        }
        if (velocity.getX() != 0 && world.hasCollisionsIn(box.clone().shift(velocity.getX(), 0, 0))) {
            velocity.setX(0);
        }
        if (velocity.getZ() != 0 && world.hasCollisionsIn(box.clone().shift(0, 0, velocity.getZ()))) {
            velocity.setZ(0);
        }
    }

    private Vector steering(Player player, Input input, double acceleration) {
        return RopePhysics.steering(input.isForward(), input.isBackward(), input.isLeft(), input.isRight(),
                player.getLocation().getYaw(), acceleration);
    }

    private void burst(Player player, GrappleState state) {
        if (!settings.burstEnabled() || state.burstCooldown > 0 || state.gas < settings.burstGasCost()
                || player.isGliding() || player.isInWater() || player.isInLava()) {
            return;
        }
        Vector direction = player.getLocation().getDirection();
        Vector velocity = state.currentVelocity().add(direction.clone().multiply(settings.burstPower()));
        RopePhysics.clampSpeed(velocity, settings.maxSpeed());
        sendVelocity(player, state, velocity);
        state.mode = GrappleState.Mode.FREE_FLIGHT;
        state.freeFlightTicks = 0;
        state.gas -= settings.burstGasCost();
        state.gasRegenDelay = settings.gasRegenDelayTicks();
        state.burstCooldown = settings.burstCooldownTicks();
        state.hudTicks = HUD_LINGER_TICKS;

        Location at = player.getLocation().add(0, player.getHeight() * 0.5, 0);
        sound(at, Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 1.3f);
        Vector behind = direction.multiply(-0.6);
        particle(Particle.CLOUD, at.add(behind), 12, 0.2, 0.05);
    }

    private void updateHooks(Player player, GrappleState state, Vector hip) {
        Iterator<Hook> iterator = state.hooks.iterator();
        while (iterator.hasNext()) {
            Hook hook = iterator.next();
            if (!hook.world().equals(player.getWorld())) {
                hook.remove();
                iterator.remove();
                continue;
            }
            switch (hook.tick(settings.hookTravelSpeed(), hip)) {
                case FINISHED -> {
                    hook.remove();
                    iterator.remove();
                }
                case LOST -> {
                    snap(player, hook);
                    iterator.remove();
                }
                case ATTACHED_NOW -> {
                    hook.ropeLength = Math.clamp(hip.distance(hook.anchor()), settings.minRopeLength(),
                            settings.maxRopeLength());
                    state.groundedHookTicks = 0;
                    // A hook landing mid-air starts taut; standing on the ground slackens it again.
                    state.ropeSlack = false;
                    attachEffects(player, hook);
                }
                case ATTACHED -> {
                    double distance = hip.distance(hook.anchor());
                    if (distance > hook.ropeLength + SNAP_SLACK || distance > settings.maxRopeLength() + SNAP_SLACK) {
                        snap(player, hook);
                        iterator.remove();
                    }
                }
                case FLYING -> {
                }
            }
        }
    }

    private void releaseAll(Player player, GrappleState state, boolean playSound) {
        if (state.hooks.isEmpty()) {
            return;
        }
        boolean hadHooks = state.hasHooks();
        for (Hook hook : state.hooks) {
            hook.remove();
        }
        state.hooks.clear();
        state.sneakArmed = false;
        restoreFlight(player, state);
        if (playSound && hadHooks) {
            sound(player.getLocation(), Sound.ITEM_LEAD_UNTIED, 0.8f, 1.2f);
        }
    }

    private void snap(Player player, Hook hook) {
        hook.remove();
        sound(hook.anchor().toLocation(hook.world()), Sound.ITEM_LEAD_BREAK, 0.8f, 1.0f);
        player.playSound(player, Sound.ITEM_LEAD_BREAK, 0.6f, 1.0f);
    }

    private void renderHooks(GrappleState state, Vector hip) {
        // Aim the rope one tick ahead, since the client draws the update one tick later.
        Vector playerEnd = hip.clone().add(state.currentVelocity());
        for (Hook hook : state.hooks) {
            hook.render(playerEnd);
        }
    }

    private void regenerateGas(GrappleState state, boolean onGround, boolean reeling) {
        if (reeling || !onGround || state.gas >= settings.gasCapacity()) {
            return;
        }
        if (state.gasRegenDelay > 0) {
            state.gasRegenDelay--;
            return;
        }
        state.gas = Math.min(settings.gasCapacity(), state.gas + settings.gasRegenPerTick());
    }

    // ------------------------------------------------------------------ flight-kick protection

    /**
     * Hanging still on a rope looks like flying to the server's anti-fly check, which kicks
     * after a few seconds. Allowing flight while hooked prevents that; toggling real flight
     * is cancelled by the listener. A persistent marker undoes it if the server stops mid-swing.
     */
    private void enableManagedFlight(Player player, GrappleState state) {
        if (state.managedFlight || player.getAllowFlight()) {
            return;
        }
        player.getPersistentDataContainer().set(managedFlightKey, PersistentDataType.BYTE, (byte) 1);
        player.setAllowFlight(true);
        state.managedFlight = true;
    }

    private void restoreFlight(Player player, GrappleState state) {
        if (!state.managedFlight) {
            return;
        }
        state.managedFlight = false;
        player.getPersistentDataContainer().remove(managedFlightKey);
        if (isSurvivalLike(player.getGameMode())) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    private static boolean isSurvivalLike(GameMode mode) {
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    // ------------------------------------------------------------------ helpers

    private GrappleState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), id -> new GrappleState(settings.gasCapacity()));
    }

    /**
     * Server-side ground check. The client-reported on-ground flag can be spoofed, which
     * would let a modified client refill gas in mid-air.
     */
    private static boolean isGrounded(Player player) {
        BoundingBox feet = player.getBoundingBox().expand(-0.01, 0, -0.01);
        feet.resize(feet.getMinX(), feet.getMinY() - 0.06, feet.getMinZ(), feet.getMaxX(), feet.getMinY(), feet.getMaxZ());
        return player.getWorld().hasCollisionsIn(feet);
    }

    private static Vector hip(Player player) {
        return player.getLocation().toVector().add(new Vector(0, player.getHeight() * 0.5, 0));
    }

    private boolean canHookEntity(Player shooter, Entity entity) {
        if (!settings.hookEntities() || entity.equals(shooter) || !entity.isValid()) {
            return false;
        }
        if (entity instanceof Player target) {
            return settings.hookPlayers() && target.getGameMode() != GameMode.SPECTATOR;
        }
        return entity instanceof LivingEntity || entity instanceof Vehicle || entity instanceof Hanging;
    }

    private void attachEffects(Player player, Hook hook) {
        Location at = hook.anchor().toLocation(hook.world());
        sound(at, Sound.ITEM_CROSSBOW_HIT, 0.9f, 1.2f);
        player.playSound(player, Sound.ITEM_CROSSBOW_HIT, 0.5f, 1.4f);
        if (settings.particles()) {
            Block block = at.getBlock();
            if (!block.isPassable()) {
                at.getWorld().spawnParticle(Particle.BLOCK, at, 8, 0.1, 0.1, 0.1, 0, block.getBlockData());
            } else {
                at.getWorld().spawnParticle(Particle.CRIT, at, 8, 0.1, 0.1, 0.1, 0.2);
            }
        }
    }

    private void reelEffects(Player player, GrappleState state, Vector hip, Vector velocity) {
        if (state.effectTicker % 3 == 0) {
            sound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.35f, 1.7f);
        }
        if (state.effectTicker % 2 == 0) {
            Vector exhaust = velocity.clone();
            if (exhaust.lengthSquared() > 1.0E-6) {
                exhaust.normalize().multiply(-0.5);
            }
            particle(Particle.CLOUD, hip.clone().add(exhaust).toLocation(player.getWorld()), 1, 0.05, 0.01);
        }
    }

    private void sound(Location at, Sound sound, float volume, float pitch) {
        if (settings.sounds()) {
            at.getWorld().playSound(at, sound, volume, pitch);
        }
    }

    private void particle(Particle particle, Location at, int count, double spread, double speed) {
        if (settings.particles()) {
            at.getWorld().spawnParticle(particle, at, count, spread, spread, spread, speed);
        }
    }

    private void updateHud(Player player, GrappleState state) {
        if (!state.hooks.isEmpty() || state.mode != GrappleState.Mode.IDLE || state.gas < settings.gasCapacity()) {
            state.hudTicks = HUD_LINGER_TICKS;
        }
        if (state.hudTicks <= 0) {
            return;
        }
        state.hudTicks--;
        if (tickCounter % 4 != 0) {
            return;
        }
        double fraction = state.gas / settings.gasCapacity();
        int filled = (int) Math.round(fraction * HUD_SEGMENTS);
        NamedTextColor gasColor = fraction < 0.2 ? NamedTextColor.RED : NamedTextColor.AQUA;
        TextComponent.Builder hud = Component.text()
                .append(Component.text("Gas ", NamedTextColor.GRAY))
                .append(Component.text("|".repeat(filled), gasColor))
                .append(Component.text("|".repeat(HUD_SEGMENTS - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text("   Hooks ", NamedTextColor.GRAY));
        int index = 0;
        for (Hook hook : state.hooks) {
            if (hook.isMiss()) {
                continue;
            }
            hud.append(Component.text("◆ ", hook.isAttached() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            index++;
        }
        for (; index < settings.maxHooks(); index++) {
            hud.append(Component.text("◆ ", NamedTextColor.DARK_GRAY));
        }
        player.sendActionBar(hud.build());
    }
}
