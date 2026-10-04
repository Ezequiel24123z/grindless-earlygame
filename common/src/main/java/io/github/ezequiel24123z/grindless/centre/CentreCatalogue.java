package io.github.ezequiel24123z.grindless.centre;

/**
 * The galactic centre this slice ships (ADR-0099).
 *
 * <p>One sealed chamber, not a planet and not a second factory. The dimension is a solid
 * mass. {@link CentreChamber} carves the only air. An extractor here finds nothing.
 */
public final class CentreCatalogue {

    /** Dimension id. Matches {@code data/grindless/dimension/sagittarius.json}. */
    public static final String SAGITTARIUS = "grindless:sagittarius";

    /** The mass. Unbreakable, and not an item. */
    public static final String SHELL = "grindless:horizon_shell";

    /** The one object in the room. Unbreakable, and not an item. */
    public static final String MARK = "grindless:arrival_mark";

    /**
     * Blocks of mass, from {@code min_y} 0. One section, so the chamber cannot grow
     * upward into a world.
     */
    public static final int HEIGHT = 16;

    /** Half-width of the room. Air runs from {@code -RADIUS} to {@code RADIUS}. */
    public static final int RADIUS = 3;

    /** First air layer. The shell course under it is the floor. */
    public static final int AIR_BOTTOM = 2;

    /** First layer above the room. The shaft starts here. */
    public static final int AIR_TOP = 6;

    /** Half-width of the shaft above the berth. */
    public static final int SHAFT = 1;

    /** The berth, on the floor, in the middle of the room. */
    public static final int BERTH_X = 0;
    public static final int BERTH_Y = AIR_BOTTOM;
    public static final int BERTH_Z = 0;

    /** The mark, on the floor, against the north wall. */
    public static final int MARK_X = 0;
    public static final int MARK_Y = AIR_BOTTOM;
    public static final int MARK_Z = -RADIUS;

    /** Where the rider stands, south of the berth, still inside the room. */
    public static final int STAND_X = 0;
    public static final int STAND_Y = AIR_BOTTOM;
    public static final int STAND_Z = 2;

    /** Light the mark emits, so the room can be seen with no skylight. */
    public static final int MARK_LIGHT = 15;

    private CentreCatalogue() {
    }

    public static boolean isCentre(String dimension) {
        return SAGITTARIUS.equals(dimension);
    }

    /** Air of the room, before the berth and the mark replace two of those cells. */
    public static boolean inRoom(int x, int y, int z) {
        return Math.abs(x) <= RADIUS && Math.abs(z) <= RADIUS
                && y >= AIR_BOTTOM && y < AIR_TOP;
    }

    /** Air above the berth, so the ride home climbs through the mass. */
    public static boolean inShaft(int x, int y, int z) {
        return Math.abs(x) <= SHAFT && Math.abs(z) <= SHAFT
                && y >= AIR_TOP && y < HEIGHT;
    }
}
