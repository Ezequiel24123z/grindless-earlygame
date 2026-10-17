package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.source.SourceLogic;

import java.util.List;

/** Original geological/biological feeds and their practical T0 material routes (ADR-0133). */
public final class RenewableProcesses {
    private RenewableProcesses() {
    }

    public static List<ProcessRecipe> recipes() {
        return List.of(
                route("source/geological_aggregate", MachineFamily.GEOLOGICAL_COLLECTOR,
                        List.of(IngredientSpec.fluid("minecraft:water", SourceLogic.SOURCE_WATER)),
                        List.of(OutputSpec.item(SourceLogic.AGGREGATE, 1), OutputSpec.item("minecraft:clay_ball", 1)),
                        SourceLogic.GEOLOGY_TICKS, 4),
                route("source/biomass", MachineFamily.BIOCULTIVATOR,
                        List.of(IngredientSpec.item(SourceLogic.AGGREGATE, 1),
                                IngredientSpec.fluid("minecraft:water", SourceLogic.SOURCE_WATER)),
                        List.of(OutputSpec.item(SourceLogic.BIOMASS, SourceLogic.BIOMASS_PER_CYCLE)),
                        SourceLogic.CULTURE_TICKS, 4),
                route("bootstrap/dry_aggregate", MachineFamily.QUERN,
                        List.of(IngredientSpec.item(SourceLogic.AGGREGATE, 1)),
                        List.of(OutputSpec.item("minecraft:cobblestone", 4), OutputSpec.item("minecraft:gravel", 2)),
                        120, 4),
                route("bootstrap/wet_aggregate", MachineFamily.SETTLING_TROUGH,
                        List.of(IngredientSpec.item(SourceLogic.AGGREGATE, 1), IngredientSpec.fluid("minecraft:water", 1000)),
                        List.of(OutputSpec.tag("forge:raw_materials/iron", 1),
                                OutputSpec.tag("forge:raw_materials/copper", 1), OutputSpec.item("minecraft:redstone", 2),
                                OutputSpec.item("minecraft:dirt", 2), OutputSpec.item("minecraft:gravel", 2),
                                OutputSpec.fluid("minecraft:water", 750)), 200, 4),
                route("bootstrap/flint_separation", MachineFamily.SETTLING_TROUGH,
                        List.of(IngredientSpec.item("minecraft:gravel", 1), IngredientSpec.fluid("minecraft:water", 250)),
                        List.of(OutputSpec.item("minecraft:sand", 1), OutputSpec.item("minecraft:flint", 1)), 100, 4),
                route("bootstrap/fibreboard", MachineFamily.CLAY_MIXER,
                        List.of(IngredientSpec.item(SourceLogic.BIOMASS, 2), IngredientSpec.fluid("minecraft:water", 250)),
                        List.of(OutputSpec.item(SourceLogic.FIBREBOARD, 4)), 200, 8),
                route("bootstrap/paper", MachineFamily.QUERN,
                        List.of(IngredientSpec.item(SourceLogic.BIOMASS, 1)),
                        List.of(OutputSpec.item("minecraft:paper", 2)), 100, 4),
                route("bootstrap/sticks", MachineFamily.QUERN,
                        List.of(IngredientSpec.item(SourceLogic.FIBREBOARD, 1)),
                        List.of(OutputSpec.item("minecraft:stick", 4)), 80, 4),
                fired("biocarbon", List.of(IngredientSpec.item(SourceLogic.BIOMASS, 4)),
                        List.of(OutputSpec.item("minecraft:charcoal", 2)), 500, "REDUCING", 200),
                fired("glass", List.of(IngredientSpec.item("minecraft:sand", 1),
                                IngredientSpec.tag("grindless:carbon", 1)),
                        List.of(OutputSpec.item("minecraft:glass", 1)), 1400, "OXIDISING", 400),
                metal("iron", 1000),
                metal("copper", 1100));
    }

    private static ProcessRecipe metal(String material, double temperature) {
        return fired("reduce_" + material, List.of(
                        IngredientSpec.tag("forge:raw_materials/" + material, 1),
                        IngredientSpec.tag("grindless:carbon", 1)),
                List.of(OutputSpec.tag("forge:ingots/" + material, 1),
                        OutputSpec.item("grindless:slag", 1)), temperature, "REDUCING", 400);
    }

    private static ProcessRecipe fired(String id, List<IngredientSpec> inputs, List<OutputSpec> outputs,
                                       double temperature, String atmosphere, int ticks) {
        return new ProcessRecipe("bootstrap/" + id, MachineFamily.FIRING_CHAMBER, inputs, outputs,
                temperature, atmosphere, ticks, 8, List.of(), "bootstrap");
    }

    private static ProcessRecipe route(String id, MachineFamily family, List<IngredientSpec> inputs,
                                       List<OutputSpec> outputs, int ticks, long power) {
        return new ProcessRecipe(id, family, inputs, outputs, Double.NaN, null, ticks, power, List.of(), "bootstrap");
    }
}
