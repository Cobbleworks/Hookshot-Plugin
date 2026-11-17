package com.hookshot.grapple;

import org.bukkit.util.Vector;

/**
 * Server-independent rope math. All values are in blocks and blocks per tick.
 */
public final class RopePhysics {

    /** Vanilla living-entity gravity per tick. */
    public static final double GRAVITY = 0.08;
    /** Vanilla vertical air drag per tick. */
    public static final double VERTICAL_DRAG = 0.98;
    /** Largest velocity change a single rope correction may apply per tick. */
    public static final double MAX_CORRECTION = 1.0;

    private RopePhysics() {
    }

    /** Applies one tick of gravity plus the given horizontal drag. */
    public static void applyGravityAndDrag(Vector velocity, double airDrag) {
        velocity.setX(velocity.getX() * airDrag);
        velocity.setY((velocity.getY() - GRAVITY) * VERTICAL_DRAG);
        velocity.setZ(velocity.getZ() * airDrag);
    }

    /**
     * Keeps a point within {@code length} of {@code anchor}. If moving by {@code velocity}
     * would overstretch the rope, the predicted position is projected back onto the rope
     * sphere and the velocity is adjusted to reach it. Motion around the anchor is kept,
     * which is what produces a pendulum swing.
     *
     * @return true when the rope was taut and changed the velocity
     */
    public static boolean constrain(Vector position, Vector anchor, double length, Vector velocity,
                                    double maxCorrection) {
        Vector offset = position.clone().add(velocity).subtract(anchor);
        double distance = offset.length();
        if (distance <= length || distance < 1.0E-6) {
            return false;
        }
        Vector corrected = anchor.clone().add(offset.multiply(length / distance));
        Vector delta = corrected.subtract(position).subtract(velocity);
        double deltaLength = delta.length();
        if (deltaLength > maxCorrection) {
            delta.multiply(maxCorrection / deltaLength);
        }
        velocity.add(delta);
        return true;
    }

    /**
     * Converts movement keys into a horizontal acceleration relative to the player's yaw.
     * Minecraft yaw 0 faces +Z (south) and yaw 90 faces -X (west).
     */
    public static Vector steering(boolean forward, boolean backward, boolean left, boolean right,
                                  float yawDegrees, double acceleration) {
        double forwardAmount = (forward ? 1 : 0) - (backward ? 1 : 0);
        double rightAmount = (right ? 1 : 0) - (left ? 1 : 0);
        if (forwardAmount == 0 && rightAmount == 0) {
            return new Vector();
        }
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        Vector forwardDir = new Vector(-sin, 0, cos);
        Vector rightDir = new Vector(-cos, 0, -sin);
        Vector result = forwardDir.multiply(forwardAmount).add(rightDir.multiply(rightAmount));
        return result.normalize().multiply(acceleration);
    }

    /** Scales the vector down to {@code maxSpeed} if it is faster. */
    public static void clampSpeed(Vector velocity, double maxSpeed) {
        double speedSquared = velocity.lengthSquared();
        if (speedSquared > maxSpeed * maxSpeed) {
            velocity.multiply(maxSpeed / Math.sqrt(speedSquared));
        }
    }

    /** Unit vector from {@code from} to {@code to}, or zero when they coincide. */
    public static Vector direction(Vector from, Vector to) {
        Vector dir = to.clone().subtract(from);
        double length = dir.length();
        return length < 1.0E-6 ? new Vector() : dir.multiply(1.0 / length);
    }
}
