package io.github.ezequiel24123z.grindless.star;

import io.github.ezequiel24123z.grindless.structure.GroundArrayLogic;

/**
 * Numbers for the Starward Link (ADR-0096). Independent of a world.
 *
 * <p>Leaving the star costs the buffer a Ground Array adds: ten seconds of an MK3 pylon.
 * A moon trip is a capacitor (ADR-0095). This toll is the one that buffer was sized for.
 * The charge is pulled through whatever pylon covers the link. The return does not draw
 * again.
 */
public final class StarwardLinkLogic {

    /** FU a departure must accumulate before anyone moves. */
    public static final long COST = GroundArrayLogic.CAPACITY;

    /** How far, in blocks, the traveller may be from the link when it fires. */
    public static final int RANGE = 4;

    /** The planetary link the Assembler consumes. One, the step already built. */
    public static final int LINKS = 1;

    /** Array casings in the Assembler recipe. Half a ring, not the whole ring. */
    public static final int CASINGS = 4;

    /** Where the return pad is placed, one block above the deck. */
    public static final int ARRIVAL_X = 0;
    public static final int ARRIVAL_Y = DriftCatalogue.SURFACE_Y + 1;
    public static final int ARRIVAL_Z = 0;

    /** Where the traveller stands, beside the pad, on the deck. */
    public static final int STAND_X = 1;
    public static final int STAND_Z = 0;

    private StarwardLinkLogic() {
    }

    /** Charge after accepting {@code drawn} FU, never past {@link #COST}. */
    public static long accept(long stored, long drawn) {
        long held = Math.max(0L, stored);
        if (held >= COST || drawn <= 0L) {
            return Math.min(held, COST);
        }
        return Math.min(COST, held + drawn);
    }

    public static boolean ready(long stored) {
        return stored >= COST;
    }

    /** FU still to draw. Zero once {@link #ready(long)}. */
    public static long remaining(long stored) {
        return Math.max(0L, COST - Math.max(0L, stored));
    }

    /** Whether a player standing {@code dx, dy, dz} blocks from the link is still with it. */
    public static boolean inRange(int dx, int dy, int dz) {
        long distance = (long) dx * dx + (long) dy * dy + (long) dz * dz;
        return distance <= (long) RANGE * RANGE;
    }
}
