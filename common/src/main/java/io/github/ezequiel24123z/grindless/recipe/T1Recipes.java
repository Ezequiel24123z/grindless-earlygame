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
    public static final String BELT = "item:grindless:conveyor_belt";
    public static final String PLATE = "tag:forge:plates/iron";
    public static final String ROD = "tag:forge:rods/iron";
    public static final String GEAR = "tag:forge:gears/iron";
    public static final String CASING = "item:grindless:machine_casing";
    public static final String COIL = "item:grindless:copper_coil";
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
                        "grindless:flux_pylon_mk1"),
                new Gated("conveyor_belt", VOLTAIC,
                        List.of("CCC", "III", "CCC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:conveyor_belt"),
                new Gated("splitter", VOLTAIC,
                        List.of("IBI", "B B", "IBI"),
                        Map.of("I", IRON, "B", BELT),
                        "grindless:splitter"),
                new Gated("merger", VOLTAIC,
                        List.of("IBI", "BBB", "IBI"),
                        Map.of("I", IRON, "B", BELT),
                        "grindless:merger"),
                new Gated("tunnel_belt", VOLTAIC,
                        List.of(" B ", "BCB", " B "),
                        Map.of("B", BELT, "C", COBBLE),
                        "grindless:tunnel_belt"),
                new Gated("overflow_gate", VOLTAIC,
                        List.of(" B ", "BIB", " B "),
                        Map.of("B", BELT, "I", IRON),
                        "grindless:overflow_gate"),
                new Gated("crude_manipulator", VOLTAIC,
                        List.of(" I ", "CIC", " I "),
                        Map.of("I", IRON, "C", COBBLE),
                        "grindless:crude_manipulator"),
                new Gated("terrestrial_extractor", VOLTAIC,
                        List.of("CIC", "IRI", "CIC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE),
                        "grindless:terrestrial_extractor"),
                new Gated("prospectors_scanner", VOLTAIC,
                        List.of("GIG", "IRI", " C "),
                        Map.of("G", GLASS, "I", IRON, "R", REDSTONE, "C", COBBLE),
                        "grindless:prospectors_scanner"),
                new Gated("process_atlas", VOLTAIC,
                        List.of("G G", "GIG", " C "),
                        Map.of("G", GLASS, "I", IRON, "C", COBBLE),
                        "grindless:process_atlas"),
                new Gated("clay_conduit", VOLTAIC,
                        List.of("CCC", "LIL", "CCC"),
                        Map.of("C", COBBLE, "L", "item:minecraft:clay_ball", "I", IRON),
                        "grindless:clay_conduit"),
                new Gated("hand_pump", VOLTAIC,
                        List.of(" C ", "CIC", " C "),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:hand_pump"),
                new Gated("basic_tank", VOLTAIC,
                        List.of("CGC", "G G", "CIC"),
                        Map.of("C", COBBLE, "G", GLASS, "I", IRON),
                        "grindless:basic_tank"),
                new Gated("press", VOLTAIC,
                        List.of("CIC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:press"),
                new Gated("plate_die", VOLTAIC,
                        List.of("CCC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:plate_die"),
                new Gated("rod_die", VOLTAIC,
                        List.of(" C ", "CIC", " C "),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:rod_die"),
                new Gated("gear_die", VOLTAIC,
                        List.of("CIC", "I I", "CIC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:gear_die"),
                new Gated("coil_die", VOLTAIC,
                        List.of("CRC", "CIC", "CRC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE),
                        "grindless:coil_die"),
                new Gated("machine_casing", VOLTAIC,
                        List.of("P P", "R R", "P P"),
                        Map.of("P", PLATE, "R", ROD),
                        "grindless:machine_casing"),
                new Gated("assembler", VOLTAIC,
                        List.of(" G ", "CAC", " G "),
                        Map.of("G", GEAR, "C", CASING, "A", COIL),
                        "grindless:assembler"),
                new Gated("flux_conduit", VOLTAIC,
                        List.of(" I ", "IRI", " G "),
                        Map.of("I", IRON, "R", REDSTONE, "G", GLASS),
                        "grindless:flux_conduit"),
                new Gated("capacitor_bank", VOLTAIC,
                        List.of("IRI", "I I", "IRI"),
                        Map.of("I", IRON, "R", REDSTONE),
                        "grindless:capacitor_bank"),
                new Gated("flux_transformer", VOLTAIC,
                        List.of("IRI", "CIC", "IRI"),
                        Map.of("I", IRON, "R", REDSTONE, "C", COBBLE),
                        "grindless:flux_transformer"),
                new Gated("kiln", VOLTAIC,
                        List.of("CCC", "CIC", "CGC"),
                        Map.of("C", COBBLE, "I", IRON, "G", CARBON),
                        "grindless:kiln"));
    }
}
