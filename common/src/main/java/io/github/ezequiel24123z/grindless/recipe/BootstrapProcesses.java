package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

import java.util.List;

/**
 * The connected F0 ceramic workshop (ADR-0129).
 *
 * <p>These rows join the same live graph as later industry. Quantities are defined here once;
 * the guide, audit graph and checks describe this catalogue rather than supplying recipes.
 */
public final class BootstrapProcesses {

    public static final String GROG = "grindless:ceramic_grog";
    public static final String UNFIRED = "grindless:unfired_ceramic";
    public static final String INSULATOR = "grindless:ceramic_insulator";
    public static final double FIRING_C = 850.0;

    private BootstrapProcesses() {
    }

    /** Returns the complete workshop routes, including recovery of both unfired and fired stock. */
    public static List<ProcessRecipe> recipes() {
        return List.of(
                milling("sand", "minecraft:gravel", 1,
                        List.of(OutputSpec.item("minecraft:sand", 1)), 100),
                milling("brick_grog", "minecraft:brick", 1,
                        List.of(OutputSpec.item(GROG, 1)), 60),
                milling("insulator_grog", INSULATOR, 1,
                        List.of(OutputSpec.item(GROG, 1)), 60),
                milling("unfired_recovery", UNFIRED, 3,
                        List.of(OutputSpec.item("minecraft:clay_ball", 2),
                                OutputSpec.item("minecraft:sand", 1)), 100),
                mixing("sand_body", "minecraft:sand", 120),
                mixing("grog_body", GROG, 80),
                firing("insulators", UNFIRED, 3,
                        List.of(OutputSpec.item(INSULATOR, 2), OutputSpec.item(GROG, 1))),
                firing("bricks", "minecraft:clay_ball", 4,
                        List.of(OutputSpec.item("minecraft:brick", 4))));
    }

    private static ProcessRecipe milling(String id, String input, int count,
                                         List<OutputSpec> outputs, int ticks) {
        return new ProcessRecipe("bootstrap/" + id, MachineFamily.QUERN,
                List.of(IngredientSpec.item(input, count)), outputs, Double.NaN, null,
                ticks, FluxTier.F0.nominal() / 2, List.of(), "bootstrap");
    }

    private static ProcessRecipe mixing(String id, String aggregate, int ticks) {
        return new ProcessRecipe("bootstrap/" + id, MachineFamily.CLAY_MIXER,
                List.of(IngredientSpec.item("minecraft:clay_ball", 2),
                        IngredientSpec.item(aggregate, 1), IngredientSpec.fluid("minecraft:water", 250)),
                List.of(OutputSpec.item(UNFIRED, 3)), Double.NaN, null, "STIRRED",
                ticks, FluxTier.F0.nominal(), List.of(), "bootstrap");
    }

    private static ProcessRecipe firing(String id, String input, int count, List<OutputSpec> outputs) {
        return new ProcessRecipe("bootstrap/" + id, MachineFamily.FIRING_CHAMBER,
                List.of(IngredientSpec.item(input, count), IngredientSpec.tag("grindless:carbon", 1)),
                outputs, FIRING_C, "OXIDISING", 200, FluxTier.F0.nominal(), List.of(), "bootstrap");
    }
}
