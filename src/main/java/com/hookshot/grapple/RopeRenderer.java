package com.hookshot.grapple;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws one rope as a thin, stretched block display. The display sits at the hook end and
 * its transformation stretches it toward the player. Updates interpolate over one tick,
 * so the rope moves smoothly at 20 updates per second.
 * <p>
 * Displays are never saved with the chunk, so a crash cannot leave stray ropes behind.
 */
final class RopeRenderer {

    private final World world;
    private final BlockData blockData;
    private final float width;
    private BlockDisplay display;

    RopeRenderer(World world, BlockData blockData, float width) {
        this.world = world;
        this.blockData = blockData;
        this.width = width;
    }

    void update(Vector hookEnd, Vector playerEnd) {
        Transformation transformation = transformation(playerEnd.clone().subtract(hookEnd));
        Location origin = hookEnd.toLocation(world);
        if (display == null || !display.isValid()) {
            display = world.spawn(origin, BlockDisplay.class, d -> {
                d.setPersistent(false);
                d.setBlock(blockData);
                d.setViewRange(2.0f);
                d.setShadowRadius(0f);
                d.setTeleportDuration(1);
                d.setInterpolationDuration(1);
                d.setTransformation(transformation);
            });
            return;
        }
        if (display.getLocation().toVector().distanceSquared(hookEnd) > 1.0E-4) {
            display.teleport(origin);
        }
        display.setInterpolationDelay(0);
        display.setTransformation(transformation);
    }

    void remove() {
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    private Transformation transformation(Vector span) {
        float length = (float) Math.max(span.length(), 0.01);
        Vector3f dir = new Vector3f((float) span.getX(), (float) span.getY(), (float) span.getZ()).div(length);
        Quaternionf rotation = new Quaternionf().rotationTo(0f, 0f, 1f, dir.x, dir.y, dir.z);
        // A block model spans 0..1; shift by half the width so the rope is centred on the line.
        Vector3f translation = rotation.transform(new Vector3f(-width / 2f, -width / 2f, 0f));
        return new Transformation(translation, rotation, new Vector3f(width, width, length), new Quaternionf());
    }
}
