package io.github.ezequiel24123z.grindless.recipe;

import java.util.List;
import java.util.Map;

/**
 * The T1 crafting-table recipes, as data.
 *
 * <p>Hand-crafted and Voltaic-gated (ADR-0017, ADR-0057). They live as JSON of type
 * {@code grindless:gated_shaped}; this catalogue is what {@code VerifyRecipes} checks them
 * against. Iron is a tag. Carbon is {@code #grindless:carbon}.
 */
public final class T1Recipes {

    public static final String COBBLE = "tag:minecraft:stone_crafting_materials";
    public static final String IRON = "tag:forge:ingots/iron";
    public static final String REDSTONE = "item:minecraft:redstone";
    public static final String GLASS = "item:minecraft:glass";
    public static final String FLINT = "item:minecraft:flint";
    public static final String FURNACE = "item:minecraft:furnace";
    public static final String CARBON = "tag:grindless:carbon";
    public static final String VOLTAIC = "voltaic";

    private T1Recipes() {
    }

    public record Gated(String name, String blueprint, List<String> pattern,
                        Map<String, String> key, String result) {
    }

    public static List<Gated> gated() {
        return List.of(
                new Gated("thermal_generator", VOLTAIC,
                        List.of("CFC", "CGC", "CIC"),
                        Map.of("C", COBBLE, "F", FURNACE, "G", CARBON, "I", IRON),
                        "grindless:thermal_generator"),
                new Gated("pulverizer", VOLTAIC,
                        List.of("CFC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "F", FLINT, "I", IRON),
                        "grindless:pulverizer"),
                new Gated("arc_furnace", VOLTAIC,
                        List.of("CIC", "IRI", "CIC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE),
                        "grindless:arc_furnace"),
                new Gated("flux_pylon_mk1", VOLTAIC,
                        List.of("IGI", "IRI", "ICI"),
                        Map.of("I", IRON, "G", GLASS, "R", REDSTONE, "C", COBBLE),
                        "grindless:flux_pylon_mk1"));
    }
}
