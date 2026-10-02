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
    public static final String PLANKS = "tag:minecraft:planks";
    public static final String STICK = "item:minecraft:stick";
    /** Tag, not {@code minecraft:iron_ingot}: a pack's iron must craft the bootstrap too. */
    public static final String IRON = "tag:forge:ingots/iron";
    public static final String REDSTONE = "item:minecraft:redstone";
    public static final String GLASS = "item:minecraft:glass";

    private BootstrapRecipes() {
    }

    /**
     * A shaped recipe. {@code key} values are {@code tag:...} or {@code item:...}.
     *
     * @param iron how many iron ingots the pattern spends; the T0 loop spends two in total
     */
    public record Shaped(String name, List<String> pattern, Map<String, String> key,
                         String result, int iron) {
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
                        "grindless:research_terminal", 0));
    }

    public static List<Shapeless> shapeless() {
        return List.of(new Shapeless("data_core",
                List.of(COBBLE, REDSTONE), "grindless:data_core"));
    }

    /** Iron spent across the whole T0 loop. Dynamo and extractor take one each. */
    public static int ironBudget() {
        return shaped().stream().mapToInt(Shaped::iron).sum();
    }
}
