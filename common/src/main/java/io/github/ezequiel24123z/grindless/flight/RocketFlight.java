package io.github.ezequiel24123z.grindless.flight;

import io.github.ezequiel24123z.grindless.planet.LunarLinkLogic;
import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
import io.github.ezequiel24123z.grindless.star.DriftCatalogue;

import java.util.List;

/**
 * Numbers for a local-system rocket flight (ADR-0097). Independent of a world.
 *
 * <p>The rocket climbs one block a tick until it reaches the build ceiling. The landing
 * map then lists this star's bodies and nothing past them. Leaving the home world costs
 * the same toll as the lunar placeholder. Leaving Luna does not, so the flight home is
 * not a second bill on a world that may have no pylon.
 */
public final class RocketFlight {

    /** The home body. Matches the overworld dimension id. */
    public static final String HOME = "minecraft:overworld";

    /** FU a departure from the home world must accumulate before the rocket climbs. */
    public static final long COST = LunarLinkLogic.COST;

    /** Blocks of climb per tick. One block, so the ceiling is a flight and not a teleport. */
    public static final double ASCENT_PER_TICK = 1.0;

    /**
     * Blocks kept between the rocket and the exclusive build limit, so the rider's head
     * stays inside the world when the map opens.
     */
    public static final int CEILING_CLEARANCE = 3;

    /** Machine casings in the rocket's Assembler recipe. */
    public static final int ROCKET_CASINGS = 1;

    /** Motors in the rocket's Assembler recipe. The engine, not a link. */
    public static final int ROCKET_MOTORS = 1;

    /** Steel plates in the rocket's Assembler recipe. */
    public static final int ROCKET_PLATES = 2;

    /** Machine casings in the launch pad's Assembler recipe. */
    public static final int PAD_CASINGS = 1;

    /** Steel plates in the launch pad's Assembler recipe. A deck, not a ring. */
    public static final int PAD_PLATES = 4;

    /** Where the first Luna landing places the pad, one block above the regolith. */
    public static final int LUNA_PAD_X = 0;
    public static final int LUNA_PAD_Y = PlanetCatalogue.SURFACE_Y + 1;
    public static final int LUNA_PAD_Z = 0;

    /** Where the rider stands on that landing, beside the pad, on the regolith. */
    public static final int LUNA_STAND_X = 1;
    public static final int LUNA_STAND_Z = 0;

    private static final List<String> SITES = List.of(HOME, PlanetCatalogue.LUNA);

    private RocketFlight() {
    }

    /** Bodies a landing map may offer. The Drift and the black hole are not among them. */
    public static List<String> sites() {
        return SITES;
    }

    public static boolean isSite(String dimension) {
        return SITES.contains(dimension);
    }

    /** A rocket flies only inside the local system. The Drift is the station's ride (ADR-0098). */
    public static boolean canLaunch(String dimension) {
        return isSite(dimension);
    }

    /**
     * FU the climb from {@code dimension} must hold before it leaves the pad.
     *
     * @return {@code -1} when a rocket must not launch there
     */
    public static long toll(String dimension) {
        if (PlanetCatalogue.isLuna(dimension)) {
            return 0L;
        }
        if (HOME.equals(dimension)) {
            return COST;
        }
        return -1L;
    }

    public static boolean isDrift(String dimension) {
        return DriftCatalogue.isDrift(dimension);
    }

    /** Translation key for one site id. */
    public static String siteKey(String dimension) {
        if (HOME.equals(dimension)) {
            return "site.grindless.home";
        }
        if (PlanetCatalogue.isLuna(dimension)) {
            return "site.grindless.luna";
        }
        return "site.grindless.unknown";
    }

    /** Highest block Y the rocket is allowed to occupy in a dimension of that build limit. */
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

    /** FU still to draw. Zero once {@link #ready(long, long)}, or when the toll is not a flight. */
    public static long remaining(long stored, long toll) {
        if (toll < 0L) {
            return 0L;
        }
        return Math.max(0L, toll - Math.max(0L, stored));
    }
}
