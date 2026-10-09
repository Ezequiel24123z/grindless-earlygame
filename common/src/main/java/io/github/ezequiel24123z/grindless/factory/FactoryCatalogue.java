package io.github.ezequiel24123z.grindless.factory;

/** Constants for the deliberately flat factory-building dimension. */
public final class FactoryCatalogue {

    /** Dimension id. Matches {@code data/grindless/dimension/factory.json}. */
    public static final String FACTORY = "grindless:factory";

    /** Bedrock, dirt and grass form a five-block-high, perfectly level floor. */
    public static final int BEDROCK_LAYERS = 1;
    public static final int DIRT_LAYERS = 3;
    public static final int GRASS_LAYERS = 1;
    public static final int SURFACE_Y = BEDROCK_LAYERS + DIRT_LAYERS + GRASS_LAYERS - 1;

    /** Fixed midday, so a factory layout never has to work around night. */
    public static final long MIDDAY = 6000L;

    private FactoryCatalogue() {
    }

    public static boolean isFactory(String dimension) {
        return FACTORY.equals(dimension);
    }
}
