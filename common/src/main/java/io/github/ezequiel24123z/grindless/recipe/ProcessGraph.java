package io.github.ezequiel24123z.grindless.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates the T1 processing and fabrication line from a material set (ADR-0005, ADR-0063,
 * ADR-0065).
 *
 * <p>Ore line: B0×R1, dry B1×R1, wet B1, roast, R2 reduce. Forming: Press recipes keyed by die.
 * Fabrication: one Assembler recipe that manufactures Pylon MK2, which has no crafting-table
 * JSON. No Minecraft imports: {@code VerifyRecipes} dumps this graph without booting the game.
 */
public final class ProcessGraph {

    /**
     * The forms a material has that generation cares about.
     *
     * @param name    tag path, such as {@code iron}
     * @param raw     {@code forge:raw_materials/<name>}
     * @param ore     {@code forge:ores/<name>}
     * @param crushed {@code grindless:crushed_materials/<name>}
     * @param oxide   {@code grindless:oxides/<name>}
     * @param ingot   {@code forge:ingots/<name>}
     * @param plate   {@code forge:plates/<name>}
     * @param rod     {@code forge:rods/<name>}
     * @param gear    {@code forge:gears/<name>}
     */
    public record MaterialView(String name, boolean raw, boolean ore, boolean crushed, boolean oxide,
                               boolean ingot, boolean plate, boolean rod, boolean gear) {
    }

    private ProcessGraph() {
    }

    /**
     * Ore-line, roast, press and assembler recipes the given materials support.
     *
     * <p>A material without an ingot is skipped for reduction and forming. A material without
     * a raw or ore form is skipped for the ore line, not for the Press. Missing crushed drops
     * B1 and crushed roast, not B0. Missing oxide drops roast and R2, not R1.
     */
    public static List<ProcessRecipe> generate(List<MaterialView> materials) {
        List<ProcessRecipe> recipes = new ArrayList<>();
        for (MaterialView material : materials) {
            String feed = feedTag(material);
            if (feed != null && material.ingot()) {
                recipes.add(reduce(material.name(), "b0_r1", feed));
                if (material.crushed()) {
                    String crushed = crushedTag(material.name());
                    recipes.add(pulverize(material.name(), feed, crushed));
                    recipes.add(wetPulverize(material.name(), feed, crushed));
                    recipes.add(reduce(material.name(), "b1_r1", crushed));
                }
            }
            if (feed != null && material.oxide()) {
                recipes.add(roast(material.name(), "roast", feed));
                if (material.crushed()) {
                    recipes.add(roast(material.name(), "roast_crushed", crushedTag(material.name())));
                }
            }
            if (material.oxide() && material.ingot()) {
                recipes.add(reduceOxide(material.name()));
            }
            if (material.ingot() && material.plate()) {
                recipes.add(press(material.name(), "plate", "forge:plates/" + material.name(),
                        FabricationLogic.PLATE_DIE));
            }
            if (material.ingot() && material.rod()) {
                recipes.add(press(material.name(), "rod", "forge:rods/" + material.name(),
                        FabricationLogic.ROD_DIE));
            }
            if (material.ingot() && material.gear()) {
                recipes.add(press(material.name(), "gear", "forge:gears/" + material.name(),
                        FabricationLogic.GEAR_DIE));
            }
        }
        recipes.add(coilPress());
        recipes.add(pylonMk2());
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

    public static String oxideTag(String material) {
        return "grindless:oxides/" + material;
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

    /**
     * Same crushed yield as dry B1, with 0.5 B water. The byproduct step is the Chemical
     * Washer (B2, T2); this only proves the fluid slot (ADR-0062).
     */
    private static ProcessRecipe wetPulverize(String material, String feed, String crushed) {
        return new ProcessRecipe(
                "b1_wet/" + material,
                MachineFamily.PULVERIZER,
                List.of(
                        IngredientSpec.tag(feed, 1),
                        IngredientSpec.fluid(ProcessLogic.WATER, ProcessLogic.WATER_MB)),
                List.of(OutputSpec.tag(crushed, 2)),
                Double.NaN,
                null,
                ProcessLogic.PULVERIZE_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    private static ProcessRecipe roast(String material, String route, String feed) {
        return new ProcessRecipe(
                route + "/" + material,
                MachineFamily.KILN,
                List.of(IngredientSpec.tag(feed, 1)),
                List.of(
                        OutputSpec.tag(oxideTag(material), 1),
                        OutputSpec.ventedFluid(ProcessLogic.SULFUR_DIOXIDE, ProcessLogic.SO2_MB)),
                ProcessLogic.ROAST_TEMPERATURE,
                ProcessLogic.ROAST_ATMOSPHERE,
                ProcessLogic.ROAST_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /**
     * Oxide reduction. Same furnace as R1, shorter cycle, no CO: PROCESSES names SO₂ and slag
     * as the R2 byproducts (ADR-0065).
     */
    private static ProcessRecipe reduceOxide(String material) {
        return new ProcessRecipe(
                "r2/" + material,
                MachineFamily.ARC_FURNACE,
                List.of(
                        IngredientSpec.tag(oxideTag(material), 1),
                        IngredientSpec.tag(ProcessLogic.CARBON, 1)),
                List.of(
                        OutputSpec.tag(ingotTag(material), 1),
                        OutputSpec.item(ProcessLogic.SLAG, 1)),
                ProcessLogic.REDUCE_TEMPERATURE,
                ProcessLogic.REDUCE_ATMOSPHERE,
                ProcessLogic.OXIDE_REDUCE_TICKS,
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
                        OutputSpec.ventedFluid(ProcessLogic.CARBON_MONOXIDE, ProcessLogic.CO_MB)),
                ProcessLogic.REDUCE_TEMPERATURE,
                ProcessLogic.REDUCE_ATMOSPHERE,
                ProcessLogic.REDUCE_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    private static ProcessRecipe press(String material, String form, String outputTag, String die) {
        return new ProcessRecipe(
                "press/" + form + "/" + material,
                MachineFamily.PRESS,
                List.of(IngredientSpec.tag(ingotTag(material), 1)),
                List.of(OutputSpec.tag(outputTag, 1)),
                Double.NaN,
                null,
                FabricationLogic.PRESS_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(IngredientSpec.item(die, 1)));
    }

    /** T1 coil: copper ingot and a coil die. The Wire Mill is the T2 dedicated route. */
    private static ProcessRecipe coilPress() {
        return new ProcessRecipe(
                "press/coil/copper",
                MachineFamily.PRESS,
                List.of(IngredientSpec.tag("forge:ingots/copper", 1)),
                List.of(OutputSpec.item(FabricationLogic.COPPER_COIL, 1)),
                Double.NaN,
                null,
                FabricationLogic.PRESS_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(IngredientSpec.item(FabricationLogic.COIL_DIE, 1)));
    }

    /**
     * The fabrication gate (ADR-0017, ADR-0063). Pylon MK2 has no crafting-table recipe;
     * the Assembler is the only source.
     */
    private static ProcessRecipe pylonMk2() {
        return new ProcessRecipe(
                "assemble/pylon_mk2",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.tag("forge:plates/iron", 4),
                        IngredientSpec.tag("forge:gears/iron", 2)),
                List.of(OutputSpec.item(FabricationLogic.PYLON_MK2, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK);
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
