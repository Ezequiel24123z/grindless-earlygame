package io.github.ezequiel24123z.grindless.planet;

import io.github.ezequiel24123z.grindless.network.CapacitorLogic;

/**
 * Numbers for the Lunar Link (ADR-0095). Independent of a world.
 *
 * <p>One departure costs the same buffer a capacitor bank adds: ten seconds of an MK1
 * pylon. The charge is pulled through whatever pylon covers the link, so a larger pylon
 * finishes sooner and an empty grid does not finish at all. The return trip is the other
 * end of that payment. It does not draw again.
 */
public final class LunarLinkLogic {

    /** FU a departure must accumulate before anyone moves. */
    public static final long COST = CapacitorLogic.CAPACITY;

    /** How far, in blocks, the traveller may be from the link when it fires. */
    public static final int RANGE = 4;

    /** Array casings in the Assembler recipe. Two parts of a ring, not the whole ring. */
    public static final int CASINGS = 2;

    /** Machine casings in the Assembler recipe. */
    public static final int MACHINE_CASINGS = 1;

    /** Where the return pad is placed on Luna, one block above the regolith. */
    public static final int ARRIVAL_X = 0;
    public static final int ARRIVAL_Y = PlanetCatalogue.SURFACE_Y + 1;
    public static final int ARRIVAL_Z = 0;

    /** Where the traveller stands, beside the pad, on the regolith. */
    public static final int STAND_X = 1;
    public static final int STAND_Z = 0;

    private LunarLinkLogic() {
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
