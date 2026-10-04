package io.github.ezequiel24123z.grindless.star;

/**
 * The one interstellar stop this slice ships (ADR-0096).
 *
 * <p>The Drift is the dark between the home star and the galactic centre. It is not a
 * planet: there is no vein, no ore and no signature reagent. An extractor standing here
 * finds nothing, which is what keeps the hop from becoming a mining dimension.
 */
public final class DriftCatalogue {

    /** Dimension id. Matches {@code data/grindless/dimension/drift.json}. */
    public static final String DRIFT = "grindless:drift";

    /** Bedrock under the deck. */
    public static final int BEDROCK_LAYERS = 1;

    /** One course of plating. A floor, not a crust. */
    public static final int DECK_LAYERS = 1;

    /** Y of the top solid block when {@code min_y} is 0. */
    public static final int SURFACE_Y = BEDROCK_LAYERS + DECK_LAYERS - 1;

    private DriftCatalogue() {
    }

    public static boolean isDrift(String dimension) {
        return DRIFT.equals(dimension);
    }
}
