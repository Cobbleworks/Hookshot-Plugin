package com.hookshot.grapple;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Per-player grapple state. Kept until the player quits, so gas is not refilled by re-equipping. */
final class GrappleState {

    enum Mode {
        /** Vanilla movement. */
        IDLE,
        /** At least one hook is anchored; the rope simulation drives velocity. */
        HOOKED,
        /** Just let go or burst; momentum is carried for a short time. */
        FREE_FLIGHT
    }

    final List<Hook> hooks = new ArrayList<>();
    Mode mode = Mode.IDLE;

    double gas;
    int gasRegenDelay;

    /** Velocity the server last sent while it was in control. */
    Vector simulatedVelocity = new Vector();
    /** Whether the server sent the player's velocity last tick. */
    boolean commanding;

    Vector measuredVelocity = new Vector();
    Vector previousMeasuredVelocity = new Vector();
    Vector lastPosition;
    UUID lastWorld;
    long lastTick = Long.MIN_VALUE;

    int groundTicks;
    int freeFlightTicks;
    int fireCooldown;
    int burstCooldown;
    int hudTicks;
    int effectTicker;

    boolean managedFlight;
    boolean lastJumpInput;
    boolean outOfGasWarned;

    GrappleState(double gas) {
        this.gas = gas;
    }

    /** Average of the last two measured position changes, which reduces packet-timing jitter. */
    Vector smoothedVelocity() {
        return measuredVelocity.clone().add(previousMeasuredVelocity).multiply(0.5);
    }

    /** Best estimate of the player's current velocity. */
    Vector currentVelocity() {
        return commanding ? simulatedVelocity.clone() : smoothedVelocity();
    }

    int hookCount() {
        int count = 0;
        for (Hook hook : hooks) {
            if (!hook.isMiss()) {
                count++;
            }
        }
        return count;
    }

    boolean hasHooks() {
        return hookCount() > 0;
    }

    boolean hasAttachedHook() {
        for (Hook hook : hooks) {
            if (hook.isAttached()) {
                return true;
            }
        }
        return false;
    }

    Hook oldestHook() {
        for (Hook hook : hooks) {
            if (!hook.isMiss()) {
                return hook;
            }
        }
        return null;
    }
}
