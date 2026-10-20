package io.github.ezequiel24123z.grindless.recipe;

import java.util.List;
import java.util.Map;

/**
 * The T0 crafting-table recipes, as data.
 *
 * <p>These are the only recipes authored by hand (ADR-0056). They do not depend on the pack's
 * material set, so they can live as JSON; everything past T1 is manufactured (ADR-0017) and
 * everything in the processing chain is generated from tags (ADR-0005). The catalogue exists so
 * {@code VerifyBootstrap} can check the JSON against the decision rather than against itself.
 *
 * <p>No Minecraft imports: the check compiler has none.
 */
public final class BootstrapRecipes {

    /** Vanilla cobble, blackstone, cobbled deepslate — the first stone a player has. */
    public static final String COBBLE = "tag:minecraft:stone_crafting_materials";
    public static final String PLANKS = "tag:grindless:boards";
    public static final String STICK = "item:minecraft:stick";
    /** Tag, not {@code minecraft:iron_ingot}: a pack's iron must craft the bootstrap too. */
    public static final String IRON = "tag:forge:ingots/iron";
    public static final String REDSTONE = "item:minecraft:redstone";
    public static final String GLASS = "item:minecraft:glass";
    public static final String COPPER = "tag:forge:ingots/copper";
    public static final String INSULATOR = "item:" + BootstrapProcesses.INSULATOR;

    private BootstrapRecipes() {
    }

    /**
     * A shaped recipe. {@code key} values are {@code tag:...} or {@code item:...}.
     *
     * @param iron how many iron ingots the pattern spends; the T0 loop spends two in total
     */
    public record Shaped(String name, List<String> pattern, Map<String, String> key,
                         String result, int iron) {
        /** Primitive transport is fabricated in useful batches. */
        public int resultCount() {
            return name.equals("conveyor_belt") || name.equals("clay_conduit") ? 8 : 1;
        }
    }

    public record Shapeless(String name, List<String> ingredients, String result) {
    }

    public static List<Shaped> shaped() {
        return List.of(
                new Shaped("multitool",
                        List.of("S", "S", "C"),
                        Map.of("S", STICK, "C", COBBLE),
                        "grindless:multitool", 0),
                new Shaped("hand_crank_dynamo",
                        List.of("CCC", "CSC", "CIC"),
                        Map.of("C", COBBLE, "S", STICK, "I", IRON),
                        "grindless:hand_crank_dynamo", 1),
                new Shaped("crude_extractor",
                        List.of("CCC", "CIC", "CCC"),
                        Map.of("C", COBBLE, "I", IRON),
                        "grindless:crude_extractor", 1),
                new Shaped("research_terminal",
                        List.of("PPP", "PGP", "PCP"),
                        Map.of("P", PLANKS, "G", GLASS, "C", COBBLE),
                        "grindless:research_terminal", 0),
                new Shaped("quern",
                        List.of("CCC", "CFC", "CSC"),
                        Map.of("C", COBBLE, "F", COBBLE, "S", STICK),
                        "grindless:quern", 0),
                new Shaped("clay_mixer",
                        List.of("P P", "PCP", "SSS"),
                        Map.of("P", PLANKS, "C", COBBLE, "S", STICK),
                        "grindless:clay_mixer", 0),
                new Shaped("firing_chamber",
                        List.of("BBB", "BFB", "CRC"),
                        Map.of("B", "item:minecraft:brick", "F", "item:minecraft:furnace",
                                "C", COBBLE, "R", STICK),
                        "grindless:firing_chamber", 0),
                new Shaped("settling_trough",
                        List.of("PIP", "C C", "PPP"),
                        Map.of("P", PLANKS, "I", INSULATOR, "C", COBBLE),
                        "grindless:settling_trough", 0),
                new Shaped("settling_channel",
                        List.of("P P", "PIP"),
                        Map.of("P", PLANKS, "I", INSULATOR),
                        "grindless:settling_channel", 0),
                new Shaped("settling_screen",
                        List.of("SIS", "SPS"),
                        Map.of("S", STICK, "I", INSULATOR, "P", PLANKS),
                        "grindless:settling_screen", 0),
                new Shaped("hand_pump",
                        List.of(" S ", "PCP", " P "),
                        Map.of("P", PLANKS, "C", COBBLE, "S", STICK),
                        "grindless:hand_pump", 0),
                new Shaped("basic_tank",
                        List.of("IGI", "G G", "ICI"),
                        Map.of("I", INSULATOR, "G", GLASS, "C", COPPER),
                        "grindless:basic_tank", 0),
                new Shaped("clay_conduit",
                        List.of("I I", "CCC"),
                        Map.of("I", INSULATOR, "C", COBBLE),
                        "grindless:clay_conduit", 0),
                new Shaped("conveyor_belt",
                        List.of("PPP", "ICI"),
                        Map.of("P", PLANKS, "I", INSULATOR, "C", COPPER),
                        "grindless:conveyor_belt", 0),
                new Shaped("crude_manipulator",
                        List.of(" S ", "ICI", " P "),
                        Map.of("S", STICK, "I", INSULATOR, "C", COPPER, "P", PLANKS),
                        "grindless:crude_manipulator", 0),
                new Shaped("process_atlas",
                        List.of(" P ", "PGP", " S "),
                        Map.of("P", PLANKS, "G", GLASS, "S", STICK),
                        "grindless:process_atlas", 0),
                new Shaped("quest_book",
                        List.of("P", "S", "S"),
                        Map.of("P", "item:minecraft:paper", "S", STICK),
                        "grindless:quest_book", 0),
                new Shaped("crank_handle",
                        List.of("SS", "C "),
                        Map.of("S", STICK, "C", COBBLE),
                        "grindless:crank_handle", 0),
                new Shaped("geological_collector",
                        List.of("PPP", "C C", "CCC"),
                        Map.of("P", PLANKS, "C", COBBLE),
                        "grindless:geological_collector", 0),
                new Shaped("biocultivator",
                        List.of("C C", "LPL", "PPP"),
                        Map.of("C", COBBLE, "L", "item:minecraft:clay_ball", "P", PLANKS),
                        "grindless:biocultivator", 0),
                new Shaped("assembly_bench",
                        List.of("PPP", "CSC", "C C"),
                        Map.of("P", PLANKS, "C", COBBLE, "S", STICK),
                        "grindless:assembly_bench", 0),
                new Shaped("biomass_dynamo",
                        List.of("CFC", "CIC", "CSC"),
                        Map.of("C", COBBLE, "F", "item:minecraft:furnace", "I", COPPER, "S", STICK),
                        "grindless:biomass_dynamo", 0));
    }

    public static List<Shapeless> shapeless() {
        return List.of(new Shapeless("data_core",
                List.of(INSULATOR, REDSTONE), "grindless:data_core"));
    }

    /** Iron spent across the whole T0 loop. Dynamo and extractor take one each. */
    public static int ironBudget() {
        return shaped().stream().mapToInt(Shaped::iron).sum();
    }
}
