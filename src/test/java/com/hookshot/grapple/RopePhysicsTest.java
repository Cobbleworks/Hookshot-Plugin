package com.hookshot.grapple;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RopePhysicsTest {

    private static final double EPS = 1.0E-9;

    @Test
    void tautRopeHoldsAHangingPlayerAgainstGravity() {
        Vector anchor = new Vector(0, 10, 0);
        Vector position = new Vector(0, 0, 0);
        Vector velocity = new Vector(0, -RopePhysics.GRAVITY, 0);

        assertTrue(RopePhysics.constrain(position, anchor, 10.0, velocity, RopePhysics.MAX_CORRECTION));
        assertEquals(0.0, velocity.length(), EPS);
    }

    @Test
    void slackRopeLeavesVelocityUntouched() {
        Vector velocity = new Vector(0.3, -0.5, 0.1);

        assertFalse(RopePhysics.constrain(new Vector(0, 0, 0), new Vector(0, 10, 0), 20.0, velocity, 1.0));
        assertEquals(new Vector(0.3, -0.5, 0.1), velocity);
    }

    @Test
    void sidewaysMotionAtTheBottomSwingsUpward() {
        Vector anchor = new Vector(0, 10, 0);
        Vector position = new Vector(0, 0, 0);
        Vector velocity = new Vector(1, 0, 0);

        RopePhysics.constrain(position, anchor, 10.0, velocity, RopePhysics.MAX_CORRECTION);

        assertTrue(velocity.getY() > 0, "swing should curve upward around the anchor");
        assertTrue(velocity.length() > 0.99, "a swing should keep almost all of its speed");
        assertEquals(10.0, position.clone().add(velocity).distance(anchor), 1.0E-6);
    }

    @Test
    void correctionIsCapped() {
        Vector anchor = new Vector(0, 0, 0);
        Vector position = new Vector(30, 0, 0);
        Vector velocity = new Vector();

        RopePhysics.constrain(position, anchor, 10.0, velocity, 0.5);

        assertEquals(0.5, velocity.length(), EPS);
        assertTrue(velocity.getX() < 0, "correction pulls toward the anchor");
    }

    @Test
    void steeringFollowsMinecraftYaw() {
        // yaw 0 faces +Z (south); right-hand side is -X (west)
        Vector forward = RopePhysics.steering(true, false, false, false, 0f, 1.0);
        assertEquals(0.0, forward.getX(), EPS);
        assertEquals(1.0, forward.getZ(), EPS);

        Vector right = RopePhysics.steering(false, false, false, true, 0f, 1.0);
        assertEquals(-1.0, right.getX(), EPS);
        assertEquals(0.0, right.getZ(), EPS);

        // yaw 90 faces -X (west)
        Vector west = RopePhysics.steering(true, false, false, false, 90f, 1.0);
        assertEquals(-1.0, west.getX(), EPS);
        assertEquals(0.0, west.getZ(), 1.0E-9);
    }

    @Test
    void diagonalSteeringIsNotFaster() {
        Vector diagonal = RopePhysics.steering(true, false, true, false, 37f, 0.04);
        assertEquals(0.04, diagonal.length(), EPS);
        assertEquals(0.0, diagonal.getY(), EPS);
    }

    @Test
    void opposingKeysCancel() {
        assertEquals(0.0, RopePhysics.steering(true, true, true, true, 12f, 1.0).length(), EPS);
    }

    @Test
    void clampSpeedOnlyReducesFastVectors() {
        Vector fast = new Vector(3, 4, 0);
        RopePhysics.clampSpeed(fast, 2.5);
        assertEquals(2.5, fast.length(), EPS);

        Vector slow = new Vector(0.1, 0, 0);
        RopePhysics.clampSpeed(slow, 2.5);
        assertEquals(0.1, slow.length(), EPS);
    }

    @Test
    void gravityAndDragMatchVanillaVertically() {
        Vector velocity = new Vector(1, 0, 1);
        RopePhysics.applyGravityAndDrag(velocity, 0.99);
        assertEquals(0.99, velocity.getX(), EPS);
        assertEquals(-RopePhysics.GRAVITY * RopePhysics.VERTICAL_DRAG, velocity.getY(), EPS);
    }

    @Test
    void directionIsZeroForCoincidentPoints() {
        assertEquals(0.0, RopePhysics.direction(new Vector(1, 2, 3), new Vector(1, 2, 3)).length(), EPS);
        assertEquals(1.0, RopePhysics.direction(new Vector(), new Vector(0, 5, 0)).length(), EPS);
    }
}
