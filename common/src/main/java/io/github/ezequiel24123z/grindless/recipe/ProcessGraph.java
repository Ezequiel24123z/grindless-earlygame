package io.github.ezequiel24123z.grindless.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates the dry T1 processing line from a material set (ADR-0005, ADR-0058).
 *
 * <p>Only B0×R1 and B1×R1. Fluids stay empty except the vented CO that R1 names as its sink.
 * No Minecraft imports: {@code VerifyRecipes} dumps this graph without booting the game.
 */
public final class ProcessGraph {

    /**
     * The forms a material has that generation cares about.
     *
     * @param name    tag path, such as {@code iron}
     * @param raw     {@code forge:raw_materials/<name>}
     * @param ore     {@code forge:ores/<name>}
     * @param crushed {@code grindless:crushed_materials/<name>}
     * @param ingot   {@code forge:ingots/<name>}
     */
    public record MaterialView(String name, boolean raw, boolean ore, boolean crushed, boolean ingot) {
    }

    private ProcessGraph() {
    }

    /**
     * Every B0×R1 and B1×R1 recipe the given materials support, in name then route order.
     *
     * <p>A material without an ingot is skipped (nothing to reduce to). A material without a
     * raw or ore form is skipped (nothing to feed). Missing crushed drops B1, not B0.
     */
    public static List<ProcessRecipe> generate(List<MaterialView> materials) {
        List<ProcessRecipe> recipes = new ArrayList<>();
        for (MaterialView material : materials) {
            String feed = feedTag(material);
            if (feed == null || !material.ingot()) {
                continue;
            }
            recipes.add(reduce(material.name(), "b0_r1", feed));
            if (material.crushed()) {
                String crushed = crushedTag(material.name());
                recipes.add(pulverize(material.name(), feed, crushed));
                recipes.add(reduce(material.name(), "b1_r1", crushed));
            }
        }
        return List.copyOf(recipes);
    }

    /** The tag a vein product of this material would use, or {@code null} if it has none. */
    public static String feedTag(MaterialView material) {
        if (material.raw()) {
            return "forge:raw_materials/" + material.name();
        }
        if (material.ore()) {
            return "forge:ores/" + material.name();
        }
        return null;
    }

    public static String crushedTag(String material) {
        return "grindless:crushed_materials/" + material;
    }

    public static String ingotTag(String material) {
        return "forge:ingots/" + material;
    }

    private static ProcessRecipe pulverize(String material, String feed, String crushed) {
        return new ProcessRecipe(
                "b1/" + material,
                MachineFamily.PULVERIZER,
                List.of(IngredientSpec.tag(feed, 1)),
                List.of(OutputSpec.tag(crushed, 2)),
                Double.NaN,
                null,
                ProcessLogic.PULVERIZE_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    private static ProcessRecipe reduce(String material, String route, String feed) {
        return new ProcessRecipe(
                route + "/" + material,
                MachineFamily.ARC_FURNACE,
                List.of(
                        IngredientSpec.tag(feed, 1),
                        IngredientSpec.tag(ProcessLogic.CARBON, 1)),
                List.of(
                        OutputSpec.tag(ingotTag(material), 1),
                        OutputSpec.item(ProcessLogic.SLAG, 1),
                        OutputSpec.ventedFluid(ProcessLogic.CARBON_MONOXIDE, 1)),
                ProcessLogic.REDUCE_TEMPERATURE,
                ProcessLogic.REDUCE_ATMOSPHERE,
                ProcessLogic.REDUCE_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /** The material path of a generated recipe id such as {@code b0_r1/iron}. */
    public static String materialOf(ProcessRecipe recipe) {
        String id = recipe.id();
        int slash = id.lastIndexOf('/');
        return slash < 0 ? id : id.substring(slash + 1);
    }

    public static String routeOf(ProcessRecipe recipe) {
        String id = recipe.id();
        int slash = id.lastIndexOf('/');
        return slash < 0 ? id : id.substring(0, slash);
    }

    public static String describe(ProcessRecipe recipe) {
        return recipe.id() + " " + recipe.family().name().toLowerCase(Locale.ROOT)
                + " " + recipe.itemInputs() + " -> " + recipe.itemOutputs()
                + " vent=" + recipe.ventedOutputs();
    }
}
