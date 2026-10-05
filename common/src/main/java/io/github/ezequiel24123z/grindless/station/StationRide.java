package io.github.ezequiel24123z.grindless.station;

import io.github.ezequiel24123z.grindless.centre.CentreCatalogue;
import io.github.ezequiel24123z.grindless.star.DriftCatalogue;
import io.github.ezequiel24123z.grindless.star.StarwardLinkLogic;

/**
 * Numbers for a ride on a supraluminal station (ADR-0098, ADR-0099). Independent of a world.
 *
 * <p>The station climbs one block a tick. Reaching the ceiling is the arrival: the
 * Drift when leaving any other world, the galactic centre when leaving the Drift, and
 * the berth they left when leaving the centre. There is no map and no link.
 */
public final class StationRide {

    /** The home body, used when a ride home was never recorded. */
    public static final String HOME = "minecraft:overworld";

    /**
     * The galactic centre. The rocket's checks already use this name for a hole, and a
     * rocket still does not go there (ADR-0099).
     */
    public static final String HOLE = CentreCatalogue.SAGITTARIUS;

    /** FU a departure that is not the ride home must accumulate. The array's buffer. */
    public static final long COST = StarwardLinkLogic.COST;

    /** Blocks of climb per tick. One block, so the ceiling is a ride and not a teleport. */
    public static final double ASCENT_PER_TICK = 1.0;

    /**
     * Blocks kept between the station and the exclusive build limit, so the rider's head
     * stays inside the world when the trip ends.
     */
    public static final int CEILING_CLEARANCE = 3;

    /** Starward links the berth's Assembler recipe consumes. The hop, replaced. */
    public static final int BERTH_LINKS = 1;

    /** Steel plates in the berth's Assembler recipe. A deck, not a ring. */
    public static final int BERTH_PLATES = 4;

    /** Machine casings in the station's Assembler recipe. The hull. */
    public static final int STATION_CASINGS = 1;

    /** Motors in the station's Assembler recipe. What climbs. */
    public static final int STATION_MOTORS = 1;

    /** Array casings in the station's Assembler recipe. A share of the ring, not half of it. */
    public static final int STATION_ARRAY_CASINGS = 2;

    /** Where the berth is placed on the deck, the column the return link used. */
    public static final int DRIFT_BERTH_X = StarwardLinkLogic.ARRIVAL_X;
    public static final int DRIFT_BERTH_Y = StarwardLinkLogic.ARRIVAL_Y;
    public static final int DRIFT_BERTH_Z = StarwardLinkLogic.ARRIVAL_Z;

    /** Where the rider stands on that landing, beside the berth, on the deck. */
    public static final int DRIFT_STAND_X = StarwardLinkLogic.STAND_X;
    public static final int DRIFT_STAND_Z = StarwardLinkLogic.STAND_Z;

    private StationRide() {
    }

    public static boolean isHole(String dimension) {
        return HOLE.equals(dimension);
    }

    public static boolean isDrift(String dimension) {
        return DriftCatalogue.isDrift(dimension);
    }

    /** A station may leave any loaded world. The centre is the ride home. */
    public static boolean canDepart(String dimension) {
        return dimension != null && !dimension.isEmpty();
    }

    /**
     * Where a departure from {@code dimension} arrives.
     *
     * @return the Drift, the centre, {@link #HOME} when the ride out of the centre has no
     *         saved berth, or empty when the station must not leave
     */
    public static String destination(String dimension) {
        if (!canDepart(dimension)) {
            return "";
        }
        if (isHole(dimension)) {
            return HOME;
        }
        if (isDrift(dimension)) {
            return HOLE;
        }
        return DriftCatalogue.DRIFT;
    }

    /**
     * FU the climb from {@code dimension} must hold before it leaves the berth.
     *
     * @return {@code -1} when a station must not leave
     */
    public static long toll(String dimension) {
        if (!canDepart(dimension)) {
            return -1L;
        }
        if (isDrift(dimension) || isHole(dimension)) {
            return 0L;
        }
        return COST;
    }

    /** Highest block Y the station is allowed to occupy in a dimension of that build limit. */
    public static int ceilingY(int maxBuildHeight) {
        return maxBuildHeight - CEILING_CLEARANCE;
    }

    /** The next Y, never past {@code ceiling}. */
    public static double climb(double y, int ceiling) {
        if (y >= ceiling) {
            return ceiling;
        }
        return Math.min(ceiling, y + ASCENT_PER_TICK);
    }

    public static boolean atCeiling(double y, int ceiling) {
        return y >= ceiling;
    }

    /** Charge after accepting {@code drawn} FU, never past {@code toll}. */
    public static long accept(long stored, long drawn, long toll) {
        long cap = Math.max(0L, toll);
        long held = Math.max(0L, stored);
        if (held >= cap || drawn <= 0L) {
            return Math.min(held, cap);
        }
        return Math.min(cap, held + drawn);
    }

    public static boolean ready(long stored, long toll) {
        return toll >= 0L && stored >= toll;
    }

    /** FU still to draw. Zero once {@link #ready(long, long)}, or when the toll is not a ride. */
    public static long remaining(long stored, long toll) {
        if (toll < 0L) {
            return 0L;
        }
        return Math.max(0L, toll - Math.max(0L, stored));
    }
}
