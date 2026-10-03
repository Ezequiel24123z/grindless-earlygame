package io.github.ezequiel24123z.grindless.belt;

/**
 * Conveyor numbers, independent of a world.
 *
 * <p>The first belt is unpowered and available the moment the first extractor is (ADR-0058).
 * Eight items a second across two lanes, four slots per tile, is one block per second — a
 * round number a player can see and a tick that costs one pass over the lane, not one pass
 * per item (ADR-0008).
 */
public final class BeltLogic {

    /** Total throughput of a Conveyor, both lanes together. */
    public static final int ITEMS_PER_SECOND = 8;

    public static final int LANES = 2;

    /** How many items fit on one tile of one lane when packed. */
    public static final int SLOTS_PER_LANE = 4;

    /** Length of one item along the belt, in tiles. */
    public static final double ITEM_LENGTH = 1.0 / SLOTS_PER_LANE;

    /**
     * Tiles per second. Throughput per lane divided by packing: {@code 4 / 4 = 1}.
     */
    public static final double SPEED = (ITEMS_PER_SECOND / (double) LANES) / SLOTS_PER_LANE;

    private BeltLogic() {
    }

    /** How far an item travels in {@code ticks} of game time. */
    public static double travel(int ticks) {
        return SPEED * (ticks / 20.0);
    }

    /**
     * Whether a new item can enter at the input end without overlapping the first occupant.
     */
    public static boolean canEnter(double firstItemPosition) {
        return firstItemPosition >= ITEM_LENGTH;
    }

    /**
     * The furthest {@code position} may move this step, given the item in front at
     * {@code frontPosition} ({@link Double#POSITIVE_INFINITY} if there is none).
     */
    public static double clamp(double position, double delta, double frontPosition) {
        double target = Math.min(1.0, position + delta);
        if (frontPosition != Double.POSITIVE_INFINITY) {
            target = Math.min(target, frontPosition - ITEM_LENGTH);
        }
        return Math.max(position, target);
    }

    /** Whether an item at {@code position} is ready to leave this tile. */
    public static boolean readyToLeave(double position) {
        return position >= 1.0 - 1e-9;
    }
}
