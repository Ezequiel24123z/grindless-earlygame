package io.github.ezequiel24123z.grindless.recipe;

import java.util.List;
import java.util.Map;

/**
 * The T1 crafting-table recipes, as data.
 *
 * <p>Hand-crafted physical recipes. They live as ordinary shaped JSON; this catalogue is what
 * {@code VerifyRecipes} checks them against. Iron is a tag. Carbon is {@code #grindless:carbon}.
 */
public final class T1Recipes {

    public static final String COBBLE = "tag:minecraft:stone_crafting_materials";
    public static final String IRON = "tag:forge:ingots/iron";
    public static final String REDSTONE = "item:minecraft:redstone";
    public static final String GLASS = "item:minecraft:glass";
    public static final String COPPER = "tag:forge:ingots/copper";
    public static final String FLINT = "item:minecraft:flint";
    public static final String FURNACE = "item:minecraft:furnace";
    public static final String CARBON = "tag:grindless:carbon";
    public static final String BELT = "item:grindless:conveyor_belt";
    public static final String HOPPER = "item:minecraft:hopper";
    public static final String DATA_CORE = "item:grindless:data_core";
    public static final String CALIBRATED_DATA_CORE = "item:grindless:calibrated_data_core";
    public static final String PLATE = "tag:forge:plates/iron";
    public static final String ROD = "tag:forge:rods/iron";
    public static final String GEAR = "tag:forge:gears/iron";
    public static final String CASING = "item:grindless:machine_casing";
    public static final String COIL = "item:grindless:copper_coil";

    private T1Recipes() {
    }

    public record Shaped(String name, List<String> pattern, Map<String, String> key, String result) {

        /** Craft counts are data too: transport is batched and the first controllers seed a factory. */
        public int resultCount() {
            return switch (name) {
                case "conveyor_belt", "clay_conduit" -> 8;
                case "tunnel_belt" -> 2;
                case "relay_matrix" -> 4;
                default -> 1;
            };
        }
    }

    public static List<Shaped> shaped() {
        return List.of(
                new Shaped("relay_matrix",
                        List.of("GCG", "RDR", "GCG"),
                        Map.of("G", GLASS, "C", COPPER, "R", REDSTONE, "D", CALIBRATED_DATA_CORE),
                        "grindless:relay_matrix"),
                new Shaped("thermal_generator",
                        List.of("CFC", "CGC", "CIC"),
                        Map.of("C", COBBLE, "F", FURNACE, "G", CARBON, "I", IRON),
                        "grindless:thermal_generator"),
                new Shaped("pulverizer",
                        List.of("CFC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "F", FLINT, "I", IRON),
                        "grindless:pulverizer"),
                new Shaped("arc_furnace",
                        List.of("CIC", "IRI", "CIC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE),
                        "grindless:arc_furnace"),
                new Shaped("flux_pylon_mk1",
                        List.of("IGI", "IRI", "ICI"),
                        Map.of("I", IRON, "G", GLASS, "R", REDSTONE, "C", COBBLE),
                        "grindless:flux_pylon_mk1"),
                new Shaped("conveyor_belt",
                        List.of("CCC", "III", "CCC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:conveyor_belt"),
                new Shaped("splitter",
                        List.of("IBI", "B B", "IBI"),
                        Map.of("I", IRON, "B", BELT),
                        "grindless:splitter"),
                new Shaped("merger",
                        List.of("IBI", "BBB", "IBI"),
                        Map.of("I", IRON, "B", BELT),
                        "grindless:merger"),
                new Shaped("tunnel_belt",
                        List.of(" B ", "BCB", " B "),
                        Map.of("B", BELT, "C", COBBLE),
                        "grindless:tunnel_belt"),
                new Shaped("overflow_gate",
                        List.of(" B ", "BIB", " B "),
                        Map.of("B", BELT, "I", IRON),
                        "grindless:overflow_gate"),
                new Shaped("sorter",
                        List.of("IBI", "BHB", "IBI"),
                        Map.of("I", IRON, "B", BELT, "H", HOPPER),
                        "grindless:sorter"),
                new Shaped("advanced_data_core",
                        List.of(" P ", "PDP", " P "),
                        Map.of("P", PLATE, "D", DATA_CORE),
                        "grindless:advanced_data_core"),
                new Shaped("crude_manipulator",
                        List.of(" I ", "CIC", " I "),
                        Map.of("I", IRON, "C", COBBLE),
                        "grindless:crude_manipulator"),
                new Shaped("terrestrial_extractor",
                        List.of("CIC", "IRI", "CIC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE),
                        "grindless:terrestrial_extractor"),
                new Shaped("prospectors_scanner",
                        List.of("GIG", "IRI", " C "),
                        Map.of("G", GLASS, "I", IRON, "R", REDSTONE, "C", COBBLE),
                        "grindless:prospectors_scanner"),
                new Shaped("process_atlas",
                        List.of("G G", "GIG", " C "),
                        Map.of("G", GLASS, "I", IRON, "C", COBBLE),
                        "grindless:process_atlas"),
                new Shaped("clay_conduit",
                        List.of("CCC", "LIL", "CCC"),
                        Map.of("C", COBBLE, "L", "item:minecraft:clay_ball", "I", IRON),
                        "grindless:clay_conduit"),
                new Shaped("hand_pump",
                        List.of(" C ", "CIC", " C "),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:hand_pump"),
                new Shaped("basic_tank",
                        List.of("CGC", "G G", "CIC"),
                        Map.of("C", COBBLE, "G", GLASS, "I", IRON),
                        "grindless:basic_tank"),
                new Shaped("press",
                        List.of("CIC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:press"),
                new Shaped("plate_die",
                        List.of("CCC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:plate_die"),
                new Shaped("rod_die",
                        List.of(" C ", "CIC", " C "),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:rod_die"),
                new Shaped("gear_die",
                        List.of("CIC", "I I", "CIC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:gear_die"),
                new Shaped("coil_die",
                        List.of("CRC", "CIC", "CRC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE),
                        "grindless:coil_die"),
                new Shaped("machine_casing",
                        List.of("P P", "R R", "P P"),
                        Map.of("P", PLATE, "R", ROD),
                        "grindless:machine_casing"),
                new Shaped("assembler",
                        List.of(" G ", "CAC", " G "),
                        Map.of("G", GEAR, "C", CASING, "A", COIL),
                        "grindless:assembler"),
                new Shaped("flux_conduit",
                        List.of(" I ", "IRI", " G "),
                        Map.of("I", IRON, "R", REDSTONE, "G", GLASS),
                        "grindless:flux_conduit"),
                new Shaped("capacitor_bank",
                        List.of("IRI", "I I", "IRI"),
                        Map.of("I", IRON, "R", REDSTONE),
                        "grindless:capacitor_bank"),
                new Shaped("flux_transformer",
                        List.of("IRI", "CIC", "IRI"),
                        Map.of("I", IRON, "R", REDSTONE, "C", COBBLE),
                        "grindless:flux_transformer"),
                new Shaped("kiln",
                        List.of("CCC", "CIC", "CGC"),
                        Map.of("C", COBBLE, "I", IRON, "G", CARBON),
                        "grindless:kiln"),
                new Shaped("vanadia_pellet",
                        List.of(" B ", "BOB", " B "),
                        Map.of("B", "item:minecraft:brick", "O", "tag:grindless:oxides/iron"),
                        "grindless:vanadia_pellet"));
    }
}
