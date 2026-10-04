package io.github.ezequiel24123z.grindless.planet;

import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;

import java.util.EnumSet;
import java.util.List;

/**
 * The one original planet this slice ships (ADR-0095).
 *
 * <p>Luna is a regolith world. Its vein pool is helium-3 and nothing else, so an extractor
 * there cannot print the overworld's ore list. The overworld pool is returned unchanged:
 * helium-3 is not a supply-catalogue material and must not enter that list.
 */
public final class PlanetCatalogue {

    /** Dimension id. Matches {@code data/grindless/dimension/luna.json}. */
    public static final String LUNA = "grindless:luna";

    /** Vein material name. Not a {@code forge:} ore tag. */
    public static final String HELIUM_3 = "helium_3";

    /** Reagent the extractor emits. Not {@code raw_helium_3}. */
    public static final String HELIUM_3_ITEM = "grindless:helium_3";

    /** Bedrock at the bottom of the flat column. */
    public static final int BEDROCK_LAYERS = 1;

    /** Regolith above the bedrock. No stone, no ore. */
    public static final int REGOLITH_LAYERS = 64;

    /** Y of the top solid block when {@code min_y} is 0. */
    public static final int SURFACE_Y = BEDROCK_LAYERS + REGOLITH_LAYERS - 1;

    private static final List<Material> LUNA_VEINS = List.of(
            new Material(HELIUM_3, EnumSet.of(MaterialForm.RAW), Material.DEFAULT_WEIGHT));

    private PlanetCatalogue() {
    }

    public static boolean isLuna(String dimension) {
        return LUNA.equals(dimension);
    }

    /**
     * Materials a vein in {@code dimension} may roll.
     *
     * <p>Luna ignores {@code pack}. Every other dimension, including ones this mod does not
     * know, keeps the pack list so a Nether vein does not become helium-3.
     */
    public static List<Material> veins(String dimension, List<Material> pack) {
        return isLuna(dimension) ? LUNA_VEINS : pack;
    }

    /**
     * The item id an extractor should emit for this vein, or {@code null} when the pack's
     * own material resolution applies.
     */
    public static String extractable(String dimension, String material) {
        if (isLuna(dimension) && HELIUM_3.equals(material)) {
            return HELIUM_3_ITEM;
        }
        return null;
    }
}
