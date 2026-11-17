package com.hookshot.grapple;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * One fired hook. It flies toward its target, then either anchors (block or entity) or,
 * for a miss, flies back to the player and disappears.
 */
final class Hook {

    enum Status { FLYING, ATTACHED_NOW, ATTACHED, FINISHED, LOST }

    private final World world;
    private final Block block;
    private final Entity entity;
    private final Vector entityOffset;
    private final Vector fixedTarget;
    private final boolean miss;
    private final RopeRenderer rope;
    private final Vector head;
    private boolean attached;
    private boolean returning;
    double ropeLength;

    private Hook(World world, Vector start, Block block, Entity entity, Vector entityOffset, Vector fixedTarget,
                 boolean miss, RopeRenderer rope) {
        this.world = world;
        this.head = start.clone();
        this.block = block;
        this.entity = entity;
        this.entityOffset = entityOffset;
        this.fixedTarget = fixedTarget;
        this.miss = miss;
        this.rope = rope;
    }

    static Hook toBlock(World world, Vector start, Vector hitPosition, Block block, RopeRenderer rope) {
        return new Hook(world, start, block, null, null, hitPosition.clone(), false, rope);
    }

    static Hook toEntity(World world, Vector start, Vector hitPosition, Entity entity, RopeRenderer rope) {
        Vector offset = hitPosition.clone().subtract(entity.getLocation().toVector());
        return new Hook(world, start, null, entity, offset, null, false, rope);
    }

    static Hook miss(World world, Vector start, Vector end, RopeRenderer rope) {
        return new Hook(world, start, null, null, null, end.clone(), true, rope);
    }

    World world() {
        return world;
    }

    boolean isMiss() {
        return miss;
    }

    boolean isAttached() {
        return attached;
    }

    /** Current anchor point, or the hook head while in flight. */
    Vector anchor() {
        return head;
    }

    Status tick(double travelSpeed, Vector playerHip) {
        if (!targetValid()) {
            return Status.LOST;
        }
        if (attached) {
            head.copy(target());
            return Status.ATTACHED;
        }
        Vector target = returning ? playerHip : target();
        Vector toTarget = target.clone().subtract(head);
        double distance = toTarget.length();
        if (distance > travelSpeed) {
            head.add(toTarget.multiply(travelSpeed / distance));
            return Status.FLYING;
        }
        head.copy(target);
        if (miss) {
            if (returning) {
                return Status.FINISHED;
            }
            returning = true;
            return Status.FLYING;
        }
        attached = true;
        return Status.ATTACHED_NOW;
    }

    void render(Vector playerEnd) {
        rope.update(head, playerEnd);
    }

    void remove() {
        rope.remove();
    }

    private Vector target() {
        if (entity != null) {
            return entity.getLocation().toVector().add(entityOffset);
        }
        return fixedTarget;
    }

    private boolean targetValid() {
        if (entity != null) {
            return entity.isValid() && entity.getWorld().equals(world);
        }
        if (block != null) {
            return !block.isPassable();
        }
        return true;
    }
}
