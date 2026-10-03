package io.github.ezequiel24123z.grindless.belt;

/**
 * One stack travelling on a lane.
 *
 * <p>Identity is a registry id string so this type stays Minecraft-free and the behaviour
 * checks can drive a belt without loading the game. The world layer materialises a real
 * {@code ItemStack} only at the endpoints (ADR-0008).
 */
public final class LaneItem {

    private final String id;
    private final int count;
    private double position;

    public LaneItem(String id, int count, double position) {
        this.id = id;
        this.count = Math.max(1, count);
        this.position = position;
    }

    public String id() {
        return id;
    }

    public int count() {
        return count;
    }

    /** 0 is the input end of the tile; 1 is the output end. */
    public double position() {
        return position;
    }

    public void setPosition(double position) {
        this.position = Math.max(0.0, Math.min(1.0, position));
    }
}
