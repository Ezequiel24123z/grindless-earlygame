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
    public static final String RELAY_MATRIX = "item:grindless:relay_matrix";
    public static final String MINIMUM_F1 = IngredientSpec.MINIMUM_CONTROL_PREFIX + "F1";
    public static final String PLATE = "tag:forge:plates/iron";
    public static final String ROD = "tag:forge:rods/iron";
    public static final String GEAR = "tag:forge:gears/iron";
    public static final String CASING = "item:grindless:machine_casing";
    public static final String COIL = "item:grindless:copper_coil";
    public static final String MOTOR = "item:grindless:motor";

    private T1Recipes() {
    }

    public record Shaped(String name, List<String> pattern, Map<String, String> key, String result) {

        /** Actual cell-order admission metadata, including physical minimum control. */
        public List<IngredientSpec> ingredients() {
            return pattern.stream().flatMap(row -> row.chars().filter(symbol -> symbol != ' ')
                    .mapToObj(symbol -> IngredientSpec.crafting(key.get(String.valueOf((char) symbol))))).toList();
        }

        /** Craft counts are data too: transport is batched and the first controllers seed a factory. */
        public int resultCount() {
            return switch (name) {
                case "conveyor_belt", "clay_conduit" -> 8;
                case "tunnel_belt" -> 2;
                case "relay_matrix" -> 5;
                default -> 1;
            };
        }
    }

    public static List<Shaped> shaped() {
        return List.of(
                new Shaped("relay_matrix",
                        List.of("G C", "RDR", "C G"),
                        Map.of("G", GLASS, "C", COPPER, "R", REDSTONE, "D", CALIBRATED_DATA_CORE),
                        "grindless:relay_matrix"),
                new Shaped("thermal_generator",
                        List.of("CFC", "CGC", "MIC"),
                        Map.of("C", COBBLE, "F", FURNACE, "G", CARBON, "I", IRON, "M", MINIMUM_F1),
                        "grindless:thermal_generator"),
                new Shaped("thermal_generator_from_core",
                        List.of("CCC", "CKC", " M "),
                        Map.of("C", COBBLE, "K", "item:" + FabricationLogic.THERMAL_GENERATOR_CORE,
                                "M", MINIMUM_F1),
                        "grindless:thermal_generator"),
                new Shaped("pulverizer",
                        List.of("MFC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "F", FLINT, "I", IRON, "M", MINIMUM_F1),
                        "grindless:pulverizer"),
                new Shaped("arc_furnace",
                        List.of("MIC", "IRI", "CIC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE, "M", MINIMUM_F1),
                        "grindless:arc_furnace"),
                new Shaped("flux_pylon_mk1",
                        List.of("IGI", "IRI", "ICI"),
                        Map.of("I", IRON, "G", GLASS, "R", REDSTONE, "C", COBBLE),
                        "grindless:flux_pylon_mk1"),
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
                new Shaped("terrestrial_extractor",
                        List.of("ICI", "IMI", "CRC"),
                        Map.of("C", COBBLE, "I", IRON, "R", REDSTONE, "M", MINIMUM_F1),
                        "grindless:terrestrial_extractor"),
                new Shaped("prospectors_scanner",
                        List.of("GIG", "IRI", " C "),
                        Map.of("G", GLASS, "I", IRON, "R", REDSTONE, "C", COBBLE),
                        "grindless:prospectors_scanner"),
                new Shaped("press",
                        List.of("MIC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "I", IRON, "M", MINIMUM_F1),
                        "grindless:press"),
                new Shaped("plate_die",
                        List.of("CIC", "CCC", "CCC"),
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
                        List.of("MG ", "CAC", " G "),
                        Map.of("G", GEAR, "C", CASING, "A", COIL, "M", MINIMUM_F1),
                        "grindless:assembler"),
                new Shaped("wire_mill",
                        List.of("POP", "PCM", "PO "),
                        Map.of("C", CASING, "O", COIL, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.WIRE_MILL),
                new Shaped("chemical_reactor",
                        List.of("POP", "MCP", "PO "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.CHEMICAL_REACTOR),
                new Shaped("chemical_washer",
                        List.of("POP", "PCP", "MO "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.CHEMICAL_WASHER),
                new Shaped("electrolysis_cell",
                        List.of("PMP", "OCO", "PP "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.ELECTROLYSIS_CELL),
                new Shaped("atmospheric_intake",
                        List.of("PMO", "PCP", "PO "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.ATMOSPHERIC_INTAKE),
                new Shaped("induction_furnace",
                        List.of("P P", "OCO", "PMP"),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.INDUCTION_FURNACE),
                new Shaped("caster",
                        List.of("MPP", "OCO", "PP "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.CASTER),
                new Shaped("flotation_cell",
                        List.of("OPP", "PCM", "PO "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.FLOTATION_CELL),
                new Shaped("boiler",
                        List.of("PPO", "PCM", "PO "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.BOILER),
                new Shaped("condenser",
                        List.of("PPM", "PCO", "PO "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE, "M", MINIMUM_F1),
                        FabricationLogic.CONDENSER),
                new Shaped("fluid_well",
                        List.of("POP", "PCP", " O "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE),
                        FabricationLogic.FLUID_WELL),
                new Shaped("solar_array",
                        List.of("PPP", "OCO", " P "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE),
                        FabricationLogic.SOLAR_ARRAY),
                new Shaped("electric_pump",
                        List.of("POP", "PCO", " P "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE),
                        FabricationLogic.ELECTRIC_PUMP),
                new Shaped("logic_controller",
                        List.of("P P", "OCO", "PP "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE),
                        FabricationLogic.LOGIC_CONTROLLER),
                new Shaped("pattern_scanner",
                        List.of("PPO", "PCO", "P  "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE),
                        FabricationLogic.PATTERN_SCANNER),
                new Shaped("deconstructor",
                        List.of("OPP", "PCO", "P  "),
                        Map.of("C", CASING, "O", MOTOR, "P", PLATE),
                        FabricationLogic.DECONSTRUCTOR),
                new Shaped("flux_conduit",
                        List.of(" I ", "IRI", " G "),
                        Map.of("I", IRON, "R", REDSTONE, "G", GLASS),
                        "grindless:flux_conduit"),
                new Shaped("flux_cell",
                        List.of("CCC", "CIC", "CCC"),
                        Map.of("C", COPPER, "I", IRON),
                        "grindless:flux_cell"),
                new Shaped("capacitor_bank",
                        List.of("IRI", "I I", "IRI"),
                        Map.of("I", IRON, "R", REDSTONE),
                        "grindless:capacitor_bank"),
                new Shaped("flux_transformer",
                        List.of("IRI", "CIC", "IRI"),
                        Map.of("I", IRON, "R", REDSTONE, "C", COBBLE),
                        "grindless:flux_transformer"),
                new Shaped("factory_portal",
                        List.of("GCG", "RMR", "GCG"),
                        Map.of("G", GLASS, "C", COPPER, "R", REDSTONE, "M", MINIMUM_F1),
                        "grindless:factory_portal"),
                new Shaped("kiln",
                        List.of("MCC", "CIC", "CGC"),
                        Map.of("C", COBBLE, "I", IRON, "G", CARBON, "M", MINIMUM_F1),
                        "grindless:kiln"),
                new Shaped("vanadia_pellet",
                        List.of(" B ", "BOB", " B "),
                        Map.of("B", "item:minecraft:brick", "O", "tag:grindless:oxides/iron"),
                        "grindless:vanadia_pellet"));
    }
}
