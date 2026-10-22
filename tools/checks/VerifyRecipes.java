package io.github.ezequiel24123z.grindless.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.menu.MachineMenuKind;
import io.github.ezequiel24123z.grindless.process.ConditionBand;
import io.github.ezequiel24123z.grindless.process.MachineEnvelopes;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Behaviour checks for generated ore-line, press and assembler recipes and the Voltaic-gated T1 crafts.
 * Not part of the mod.
 */
public final class VerifyRecipes {

    private static final Path RECIPES = Path.of("common/src/main/resources/data/grindless/recipes");
    private static final List<String> PROTOTYPE_RECIPE_IDS = List.of(
            "assemble/array_casing",
            "assemble/ground_array",
            "assemble/lunar_link",
            "assemble/starward_link",
            "assemble/launch_pad",
            "assemble/survey_rocket",
            "assemble/station_berth",
            "assemble/station");
    private static final List<String> PROTOTYPE_ITEMS = List.of(
            "array_casing",
            "ground_array",
            "lunar_link",
            "starward_link",
            "launch_pad",
            "survey_rocket",
            "station_berth",
            "supraluminal_station");

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        indexedLookupBeforeBootstrap();
        graph();
        pressure();
        logic();
        crafts();

        System.out.println(failures == 0
                ? "ALL RECIPE CHECKS PASSED"
                : failures + " RECIPE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void indexedLookupBeforeBootstrap() throws ReflectiveOperationException {
        ProcessLookup.Generation original = ProcessLookup.generation();
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        List<ProcessRecipe> first = ProcessLookup.recipes();
        yes("rebuilding before native bootstrap retains all four minimum-primary machine routes",
                first.stream().filter(row -> !row.itemInputs().isEmpty()
                                && row.itemInputs().get(0).isMinimumControl())
                        .map(ProcessRecipe::id).collect(java.util.stream.Collectors.toSet()).equals(Set.of(
                                "assemble/pulverizer", "assemble/arc_furnace", "assemble/press", "assemble/assembler")));
        yes("a pure pre-bootstrap rebuild publishes a complete new generation",
                original != ProcessLookup.generation());
        ProcessLookup.Generation published = ProcessLookup.generation();
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        yes("a repeated pre-bootstrap rebuild preserves the generated descriptors", first.equals(ProcessLookup.recipes()));
        yes("a repeated pure rebuild still replaces the generation identity", published != ProcessLookup.generation());

        List<IngredientSpec> wetItem = List.of(IngredientSpec.item("fixture:stock", 1),
                IngredientSpec.fluid("minecraft:water", 250));
        List<IngredientSpec> wetControl = List.of(
                IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1),
                IngredientSpec.fluid("minecraft:water", 250));
        List<OutputSpec> output = List.of(OutputSpec.item("fixture:product", 1));
        ProcessRecipe early = new ProcessRecipe("fixture/index/early", MachineFamily.ASSEMBLER,
                wetItem, output, Double.NaN, null, 20, 1);
        ProcessRecipe control = new ProcessRecipe("fixture/index/minimum", MachineFamily.ASSEMBLER,
                wetControl, output, Double.NaN, null, 20, 1);
        ProcessRecipe late = new ProcessRecipe("fixture/index/late", MachineFamily.ASSEMBLER,
                List.of(IngredientSpec.item("fixture:stock", 1)), output, Double.NaN, null, 20, 1);
        ProcessRecipe otherFamily = new ProcessRecipe("fixture/index/other_family", MachineFamily.PRESS,
                wetControl, output, Double.NaN, null, 20, 1);
        // Inspect only pure candidate descriptors; no fake item registration or global graph replacement.
        Class<?> graphType = Class.forName(ProcessLookup.class.getName() + "$Graph");
        var index = graphType.getDeclaredMethod("index", List.class);
        var candidates = graphType.getDeclaredMethod("candidates", MachineFamily.class, List.class);
        index.setAccessible(true);
        candidates.setAccessible(true);
        Object graph = index.invoke(null, List.of(early, control, late, otherFamily));
        List<ProcessRecipe> expected = List.of(early, control, late);
        yes("a nonempty ordinary item index cannot exclude minimum-control candidates for another ID",
                expected.equals(candidates.invoke(graph, MachineFamily.ASSEMBLER, List.of("item:fixture:stock"))));
        yes("item, fluid and role hits deduplicate in authoritative graph order",
                expected.equals(candidates.invoke(graph, MachineFamily.ASSEMBLER,
                        List.of("item:fixture:stock", "fluid:minecraft:water", "item:fixture:stock"))));
        yes("candidate priority cannot depend on runtime tag/key enumeration order",
                expected.equals(candidates.invoke(graph, MachineFamily.ASSEMBLER,
                        List.of("fluid:minecraft:water", "item:fixture:stock"))));
        yes("an empty index hit retains the existing complete family fallback",
                expected.equals(candidates.invoke(graph, MachineFamily.ASSEMBLER, List.of("item:fixture:missing"))));
        yes("minimum-primary candidates never cross machine families",
                List.of(otherFamily).equals(candidates.invoke(graph, MachineFamily.PRESS,
                        List.of("fluid:minecraft:water"))));
    }

    private static void graph() throws Exception {
        List<ProcessGraph.MaterialView> materials = List.of(
                new ProcessGraph.MaterialView("iron", true, true, true, true, true, true, true, true, true),
                new ProcessGraph.MaterialView("gold", true, false, false, true, true, true, true, true, false),
                new ProcessGraph.MaterialView("steel", false, false, false, false, true, true, true, true, false),
                new ProcessGraph.MaterialView("mythril", false, true, true, true, true, false, false, false, true));

        List<ProcessRecipe> recipes = ProcessGraph.generate(materials);
        yes("no existing production recipe acquires a pressure requirement",
                recipes.stream().noneMatch(ProcessRecipe::namesPressure));
        eq("the reachable ore, chemistry, fabrication, logistics, tools, armour, renewable sources, primitive assembly, glass, redstone, steel, silicon and Relay Matrix graph",
                211, recipes.size());
        c3Routes(recipes);
        legacyGuideWork(recipes);
        assemblerCrafts(recipes);
        thermalGeneratorAssembly(recipes);
        thermalInputSelection(recipes);

        ProcessRecipe ironB0 = recipe(recipes, "b0_r1/iron");
        ProcessRecipe ironB1 = recipe(recipes, "b1/iron");
        ProcessRecipe ironWet = recipe(recipes, "b1_wet/iron");
        ProcessRecipe ironR1 = recipe(recipes, "b1_r1/iron");
        ProcessRecipe goldB0 = recipe(recipes, "b0_r1/gold");
        ProcessRecipe mythrilB0 = recipe(recipes, "b0_r1/mythril");
        ProcessRecipe renewableSand = recipe(recipes, "renew/sand");
        ProcessRecipe renewableGlass = recipe(recipes, "renew/glass");
        ProcessRecipe renewableRedstone = recipe(recipes, "synthesise/redstone");
        ProcessRecipe relayMatrix = recipe(recipes, "assemble/relay_matrix");
        ProcessRecipe industrialRelayMatrix = recipe(recipes, "assemble/relay_matrix_f2");

        eq("the Relay Matrix batch is assembled", MachineFamily.ASSEMBLER, relayMatrix.family());
        eq("the Relay Matrix batch retains glass", "item:minecraft:glass",
                relayMatrix.itemInputs().get(0).qualified());
        eq("the Relay Matrix batch retains copper", "tag:forge:ingots/copper",
                relayMatrix.itemInputs().get(1).qualified());
        eq("the Relay Matrix batch retains redstone", "item:minecraft:redstone",
                relayMatrix.itemInputs().get(2).qualified());
        eq("the Relay Matrix batch consumes the calibrated core", "item:grindless:calibrated_data_core",
                relayMatrix.itemInputs().get(3).qualified());
        eq("the Relay Matrix batch has four inputs", 4, relayMatrix.itemInputs().size());
        yes("the Relay Matrix batch keeps five ordinary outputs for the legacy F1 rating",
                relayMatrix.outputs().equals(List.of(OutputSpec.item(FabricationLogic.RELAY_MATRIX, 5))));
        eq("the Relay Matrix batch is twenty seconds", FabricationLogic.ASSEMBLE_TICKS,
                relayMatrix.durationTicks());
        yes("the Relay Matrix batch keeps its complete physical bill", relayMatrix.inputs().equals(List.of(
                IngredientSpec.item("minecraft:glass", 2),
                IngredientSpec.tag("forge:ingots/copper", 2),
                IngredientSpec.item("minecraft:redstone", 2),
                IngredientSpec.item(FabricationLogic.CALIBRATED_DATA_CORE, 1))));
        eq("the Industrial Matrix is assembled", MachineFamily.ASSEMBLER, industrialRelayMatrix.family());
        eq("the Industrial Matrix consumes an F1 physical Matrix", 1,
                industrialRelayMatrix.itemInputs().get(0).matrixRating());
        yes("the Industrial Matrix still needs an exact predecessor",
                industrialRelayMatrix.itemInputs().get(0).matrixRole() == IngredientSpec.MatrixRole.EXACT);
        eq("the Industrial Matrix consumes a casing", "item:grindless:machine_casing",
                industrialRelayMatrix.itemInputs().get(1).qualified());
        eq("the Industrial Matrix consumes renewable dielectric glass", "item:minecraft:glass",
                industrialRelayMatrix.itemInputs().get(2).qualified());
        eq("the Industrial Matrix consumes a Flux Cell", "item:grindless:flux_cell",
                industrialRelayMatrix.itemInputs().get(3).qualified());
        eq("the Industrial Matrix produces F2", 2, industrialRelayMatrix.itemOutputs().get(0).matrixRating());
        eq("the Industrial Matrix takes forty seconds", FabricationLogic.ASSEMBLE_TICKS * 2,
                industrialRelayMatrix.durationTicks());
        yes("the Industrial Matrix keeps its complete physical bill", industrialRelayMatrix.inputs().equals(List.of(
                IngredientSpec.matrix(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1),
                IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                IngredientSpec.item("minecraft:glass", 4),
                IngredientSpec.item(FabricationLogic.FLUX_CELL, 1))));
        yes("the Industrial Matrix keeps its exact rated output batch", industrialRelayMatrix.outputs().equals(
                List.of(OutputSpec.matrix(FabricationLogic.RELAY_MATRIX, FluxTier.F2, 1))));
        eq("the Industrial Matrix keeps its F1 draw", FabricationLogic.FU_PER_TICK,
                industrialRelayMatrix.fuPerTick());
        yes("the Industrial Matrix has no new catalyst", industrialRelayMatrix.catalysts().isEmpty());

        eq("renewable sand uses the pulverizer", MachineFamily.PULVERIZER, renewableSand.family());
        eq("renewable sand consumes cobblestone", "item:minecraft:cobblestone",
                renewableSand.itemInputs().get(0).qualified());
        eq("renewable sand produces sand", "item:minecraft:sand",
                renewableSand.itemOutputs().get(0).qualified());
        eq("renewable sand is six seconds", ProcessLogic.RENEWABLE_SAND_TICKS,
                renewableSand.durationTicks());
        eq("renewable glass uses the arc furnace", MachineFamily.ARC_FURNACE, renewableGlass.family());
        eq("renewable glass consumes sand", "item:minecraft:sand",
                renewableGlass.itemInputs().get(0).qualified());
        eq("renewable glass produces glass", "item:minecraft:glass",
                renewableGlass.itemOutputs().get(0).qualified());
        eq("renewable glass is 1500 C", ProcessLogic.VITRIFY_GLASS_TEMPERATURE,
                renewableGlass.temperatureC());
        eq("renewable glass is ten seconds", ProcessLogic.VITRIFY_GLASS_TICKS,
                renewableGlass.durationTicks());
        eq("renewable redstone uses the chemical reactor", MachineFamily.CHEMICAL_REACTOR,
                renewableRedstone.family());
        eq("renewable redstone consumes metallurgical silicon", "item:grindless:metallurgical_silicon",
                renewableRedstone.itemInputs().get(0).qualified());
        eq("renewable redstone consumes pumped water", "fluid:minecraft:water",
                renewableRedstone.fluidInputs().get(0).qualified());
        eq("renewable redstone uses half a bucket", ProcessLogic.REDSTONE_WATER_MB,
                renewableRedstone.fluidInputs().get(0).count());
        eq("renewable redstone produces four dust", "item:minecraft:redstone",
                renewableRedstone.itemOutputs().get(0).qualified());
        eq("renewable redstone batch size", ProcessLogic.REDSTONE_OUT,
                renewableRedstone.itemOutputs().get(0).count());
        eq("renewable redstone is held at 450 C", ProcessLogic.REDSTONE_TEMPERATURE,
                renewableRedstone.temperatureC());
        eq("renewable redstone is twenty seconds", ProcessLogic.REDSTONE_TICKS,
                renewableRedstone.durationTicks());

        eq("B0×R1 is the arc furnace", MachineFamily.ARC_FURNACE, ironB0.family());
        eq("B0×R1 takes one raw", "tag:forge:raw_materials/iron", ironB0.itemInputs().get(0).qualified());
        eq("B0×R1 takes one carbon", "tag:grindless:carbon", ironB0.itemInputs().get(1).qualified());
        eq("B0×R1 makes one ingot", "tag:forge:ingots/iron", ironB0.itemOutputs().get(0).qualified());
        eq("B0×R1 makes one slag", "item:grindless:slag", ironB0.itemOutputs().get(1).qualified());
        eq("B0×R1 vents one CO", "fluid:grindless:carbon_monoxide", ironB0.ventedOutputs().get(0).qualified());
        yes("CO is marked vented", ironB0.ventedOutputs().get(0).vented());
        eq("CO is one bucket", 1000, ironB0.ventedOutputs().get(0).count());
        eq("R1 is 1500 C", 1500.0, ironB0.temperatureC());
        eq("R1 is reducing", "REDUCING", ironB0.atmosphere());
        eq("R1 is twelve seconds", 20 * 12, ironB0.durationTicks());
        eq("R1 draws F1", 32L, ironB0.fuPerTick());
        yes("B0×R1 has no fluid inputs", ironB0.fluidInputs().isEmpty());

        eq("B1 is the pulverizer", MachineFamily.PULVERIZER, ironB1.family());
        eq("B1 takes one raw", "tag:forge:raw_materials/iron", ironB1.itemInputs().get(0).qualified());
        eq("B1 makes two crushed", 2, ironB1.itemOutputs().get(0).count());
        eq("B1 crushed is the grindless tag", "tag:grindless:crushed_materials/iron",
                ironB1.itemOutputs().get(0).qualified());
        eq("B1 is six seconds", 20 * 6, ironB1.durationTicks());
        yes("B1 names no temperature", !ironB1.namesTemperature());
        yes("B1 names no atmosphere", !ironB1.namesAtmosphere());
        yes("B1 has empty fluid slots", ironB1.fluidInputs().isEmpty() && ironB1.fluidOutputs().isEmpty());
        eq("wet B1 takes water", "fluid:minecraft:water", ironWet.fluidInputs().get(0).qualified());
        eq("wet B1 takes half a bucket", 500, ironWet.fluidInputs().get(0).count());
        eq("wet B1 still makes two crushed", 2, ironWet.itemOutputs().get(0).count());
        eq("wet B1 is still the pulverizer", MachineFamily.PULVERIZER, ironWet.family());
        yes("wet B1 is not a slurry step", ironWet.itemOutputs().size() == 1 && ironWet.ventedOutputs().isEmpty());

        eq("B1×R1 feeds crushed", "tag:grindless:crushed_materials/iron",
                ironR1.itemInputs().get(0).qualified());
        eq("B1×R1 still makes one ingot per crushed", 1, ironR1.itemOutputs().get(0).count());

        ProcessRecipe ironRoast = recipe(recipes, "roast/iron");
        ProcessRecipe ironRoastCrushed = recipe(recipes, "roast_crushed/iron");
        ProcessRecipe ironR2 = recipe(recipes, "r2/iron");
        ProcessRecipe goldRoast = recipe(recipes, "roast/gold");

        eq("roast is the kiln", MachineFamily.KILN, ironRoast.family());
        eq("roast takes one raw", "tag:forge:raw_materials/iron", ironRoast.itemInputs().get(0).qualified());
        eq("roast makes one oxide", "tag:grindless:oxides/iron", ironRoast.itemOutputs().get(0).qualified());
        eq("roast vents one SO2", "fluid:grindless:sulfur_dioxide", ironRoast.ventedOutputs().get(0).qualified());
        eq("SO2 is one bucket", 1000, ironRoast.ventedOutputs().get(0).count());
        eq("roast is 700 C", 700.0, ironRoast.temperatureC());
        eq("roast is oxidising", "OXIDISING", ironRoast.atmosphere());
        eq("roast is eight seconds", 20 * 8, ironRoast.durationTicks());
        eq("roast draws F1", 32L, ironRoast.fuPerTick());
        yes("roast has no fluid inputs", ironRoast.fluidInputs().isEmpty());
        eq("crushed roast feeds crushed", "tag:grindless:crushed_materials/iron",
                ironRoastCrushed.itemInputs().get(0).qualified());
        eq("crushed roast still makes one oxide", 1, ironRoastCrushed.itemOutputs().get(0).count());

        eq("R2 is the arc furnace", MachineFamily.ARC_FURNACE, ironR2.family());
        eq("R2 takes oxide", "tag:grindless:oxides/iron", ironR2.itemInputs().get(0).qualified());
        eq("R2 takes carbon", "tag:grindless:carbon", ironR2.itemInputs().get(1).qualified());
        eq("R2 makes one ingot", 1, ironR2.itemOutputs().get(0).count());
        eq("R2 makes slag", "item:grindless:slag", ironR2.itemOutputs().get(1).qualified());
        yes("R2 does not vent CO", ironR2.ventedOutputs().isEmpty());
        eq("R2 is ten seconds", 20 * 10, ironR2.durationTicks());
        eq("R2 reduce is the locked furnace temperature", 1500.0, ironR2.temperatureC());
        eq("R2 is reducing", "REDUCING", ironR2.atmosphere());

        eq("gold without crushed still roasts the raw", "roast/gold", goldRoast.id());
        no("gold has no crushed roast", recipes.stream().anyMatch(recipe -> recipe.id().equals("roast_crushed/gold")));
        yes("gold still reduces oxide", recipes.stream().anyMatch(recipe -> recipe.id().equals("r2/gold")));

        eq("gold without crushed is B0 only", "b0_r1/gold", goldB0.id());
        no("gold has no B1", recipes.stream().anyMatch(recipe -> recipe.id().equals("b1/gold")));
        eq("mythril without raw uses the ore tag", "tag:forge:ores/mythril",
                mythrilB0.itemInputs().get(0).qualified());

        no("no generated recipe names an item id for a material",
                recipes.stream().anyMatch(VerifyRecipes::namesMaterialItem));
        yes("every recipe id is unique",
                recipes.stream().map(ProcessRecipe::id).distinct().count() == recipes.size());
        for (String id : PROTOTYPE_RECIPE_IDS) {
            no(id + " is not in the survival graph",
                    recipes.stream().anyMatch(recipe -> recipe.id().equals(id)));
        }
        for (String item : PROTOTYPE_ITEMS) {
            no(item + " has no generated survival route",
                    recipes.stream().flatMap(recipe -> recipe.itemOutputs().stream())
                            .anyMatch(output -> output.qualified().equals("item:grindless:" + item)));
        }

        ProcessRecipe ironPlate = recipe(recipes, "press/plate/iron");
        ProcessRecipe ironRod = recipe(recipes, "press/rod/iron");
        ProcessRecipe ironGear = recipe(recipes, "press/gear/iron");
        ProcessRecipe coil = recipe(recipes, "press/coil/copper");
        ProcessRecipe mk2 = recipe(recipes, "assemble/pylon_mk2");

        eq("plate is the press", MachineFamily.PRESS, ironPlate.family());
        eq("plate takes one ingot", "tag:forge:ingots/iron", ironPlate.itemInputs().get(0).qualified());
        eq("plate uses the plate die", "item:grindless:plate_die", ironPlate.catalysts().get(0).qualified());
        eq("plate makes one plate", "tag:forge:plates/iron", ironPlate.itemOutputs().get(0).qualified());
        eq("press is four seconds", 20 * 4, ironPlate.durationTicks());
        eq("press draws F1", 32L, ironPlate.fuPerTick());
        yes("the die is not an input", ironPlate.itemInputs().size() == 1);
        eq("rod uses the rod die", "item:grindless:rod_die", ironRod.catalysts().get(0).qualified());
        eq("gear uses the gear die", "item:grindless:gear_die", ironGear.catalysts().get(0).qualified());

        eq("coil is the press", MachineFamily.PRESS, coil.family());
        yes("the T1 coil keeps one copper ingot without a Matrix or fluid",
                coil.inputs().equals(List.of(IngredientSpec.tag("forge:ingots/copper", 1))));
        yes("the T1 coil keeps its one unconsumed coil die",
                coil.catalysts().equals(List.of(IngredientSpec.item(FabricationLogic.COIL_DIE, 1))));
        yes("the T1 coil keeps its one-coil output",
                coil.outputs().equals(List.of(OutputSpec.item(FabricationLogic.COPPER_COIL, 1))));
        eq("the T1 coil keeps its four-second cycle", 80, coil.durationTicks());
        eq("the T1 coil keeps its F1 draw", 32L, coil.fuPerTick());
        yes("the T1 coil keeps its unlabelled route", coil.milestone() == null);

        eq("MK2 is the assembler", MachineFamily.ASSEMBLER, mk2.family());
        eq("MK2 takes a casing", "item:grindless:machine_casing", mk2.itemInputs().get(0).qualified());
        eq("MK2 takes four plates", 4, mk2.itemInputs().get(1).count());
        eq("MK2 plates are iron", "tag:forge:plates/iron", mk2.itemInputs().get(1).qualified());
        eq("MK2 takes two gears", 2, mk2.itemInputs().get(2).count());
        eq("MK2 is twenty seconds", 20 * 20, mk2.durationTicks());
        eq("MK2 draws F1", 32L, mk2.fuPerTick());
        yes("MK2 has no catalyst", mk2.catalysts().isEmpty());
        eq("MK2 makes the pylon", "item:grindless:flux_pylon_mk2", mk2.itemOutputs().get(0).qualified());
        eq("MK2 carries the Industrial milestone", "industrial", mk2.milestone());

        ProcessRecipe ironWire = recipe(recipes, "mill/wire/iron");
        ProcessRecipe millCoil = recipe(recipes, "mill/coil/copper");
        ProcessRecipe mill = recipe(recipes, "assemble/wire_mill");
        ProcessRecipe motor = recipe(recipes, "assemble/motor");

        eq("wire is the mill", MachineFamily.WIRE_MILL, ironWire.family());
        eq("wire takes one ingot", "tag:forge:ingots/iron", ironWire.itemInputs().get(0).qualified());
        eq("wire makes two", 2, ironWire.itemOutputs().get(0).count());
        eq("wire is the grindless tag", "tag:grindless:wires/iron", ironWire.itemOutputs().get(0).qualified());
        eq("wire is eight seconds", 20 * 8, ironWire.durationTicks());
        eq("wire draws F1", 32L, ironWire.fuPerTick());
        yes("wire has no catalyst", ironWire.catalysts().isEmpty());
        yes("wire has no milestone", ironWire.milestone() == null);
        yes("gold still mills", recipes.stream().anyMatch(recipe -> recipe.id().equals("mill/wire/gold")));
        yes("steel still mills", recipes.stream().anyMatch(recipe -> recipe.id().equals("mill/wire/steel")));
        yes("mythril with an ingot still mills",
                recipes.stream().anyMatch(recipe -> recipe.id().equals("mill/wire/mythril")));

        eq("mill coil is the mill", MachineFamily.WIRE_MILL, millCoil.family());
        eq("mill coil takes two copper wire", 2, millCoil.itemInputs().get(0).count());
        eq("mill coil feeds wire", "tag:grindless:wires/copper", millCoil.itemInputs().get(0).qualified());
        eq("mill coil is a reagent", "item:grindless:copper_coil", millCoil.itemOutputs().get(0).qualified());
        yes("mill coil has no die", millCoil.catalysts().isEmpty());

        eq("the mill is the assembler", MachineFamily.ASSEMBLER, mill.family());
        yes("the base mill retains its bill with one minimum-F1 control operand as the fourth group",
                mill.inputs().equals(List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.COPPER_COIL, 2),
                        IngredientSpec.tag("forge:plates/iron", 4),
                        IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1))));
        eq("only the five selected machine operands and the existing Wire Mill opt into minimum control", 6L,
                recipes.stream()
                .flatMap(row -> java.util.stream.Stream.concat(row.inputs().stream(), row.catalysts().stream()))
                .filter(IngredientSpec::isMinimumControl).count());
        yes("minimum control stays limited to the six named recipe identities",
                List.of("assemble/pulverizer", "assemble/arc_furnace", "assemble/press", "assemble/assembler",
                        "assemble/thermal_generator", "assemble/wire_mill")
                        .equals(recipes.stream()
                                .filter(row -> row.inputs().stream().anyMatch(IngredientSpec::isMinimumControl))
                                .map(ProcessRecipe::id).toList()));
        eq("the minimum role preserves the canonical baseline Matrix ID", "item:grindless:relay_matrix",
                mill.itemInputs().get(3).qualified());
        JsonObject serialized = new com.google.gson.Gson().toJsonTree(mill.itemInputs().get(3)).getAsJsonObject();
        eq("ingredient serialization carries the role, not a fabricated resource ID", "MINIMUM_CONTROL",
                serialized.get("matrixRole").getAsString());
        eq("ingredient serialization keeps the actual registry ID", FabricationLogic.RELAY_MATRIX,
                serialized.get("id").getAsString());
        eq("the mill has exactly four grouped item inputs", 4, mill.itemInputs().size());
        eq("the mill consumes eight total input units", 8,
                mill.itemInputs().stream().mapToInt(IngredientSpec::count).sum());
        eq("the mill fits the existing four-slot Assembler", MachineMenuKind.ASSEMBLER.inputs(),
                mill.itemInputs().size());
        eq("the mill is twenty seconds", 20 * 20, mill.durationTicks());
        eq("the mill still draws F1", 32L, mill.fuPerTick());
        eq("the mill carries the Industrial milestone", "industrial", mill.milestone());
        yes("the mill keeps its single ordinary block output",
                mill.outputs().equals(List.of(OutputSpec.item(FabricationLogic.WIRE_MILL, 1))));
        yes("the mill remains acid-free", mill.fluidInputs().isEmpty() && mill.fluidOutputs().isEmpty());
        yes("the mill consumes every group rather than retaining a catalyst", mill.catalysts().isEmpty());
        no("the mill adds no operating condition",
                mill.namesTemperature() || mill.namesAtmosphere() || mill.namesAgitation());

        eq("motor is the assembler", MachineFamily.ASSEMBLER, motor.family());
        yes("motor remains a component with its complete three-group bill and no Matrix",
                motor.inputs().equals(List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.COPPER_COIL, 2),
                        IngredientSpec.tag("forge:rods/iron", 1))));
        eq("motor is ten seconds", 20 * 10, motor.durationTicks());
        eq("motor still draws F1", 32L, motor.fuPerTick());
        eq("motor carries the Industrial milestone", "industrial", motor.milestone());
        yes("motor keeps its single ordinary component output",
                motor.outputs().equals(List.of(OutputSpec.item(FabricationLogic.MOTOR, 1))));
        yes("motor still has no catalyst", motor.catalysts().isEmpty());
        no("motor adds no operating condition",
                motor.namesTemperature() || motor.namesAtmosphere() || motor.namesAgitation());

        ProcessRecipe so3 = recipe(recipes, "contact/so3");
        ProcessRecipe acid = recipe(recipes, "contact/acid");
        ProcessRecipe pickle = recipe(recipes, "pickle/plate/iron");
        ProcessRecipe reactor = recipe(recipes, "assemble/chemical_reactor");

        eq("oxidation is the reactor", MachineFamily.CHEMICAL_REACTOR, so3.family());
        eq("oxidation takes one bucket of SO2", "fluid:grindless:sulfur_dioxide",
                so3.fluidInputs().get(0).qualified());
        eq("oxidation makes one bucket of SO3", "fluid:grindless:sulfur_trioxide",
                so3.fluidOutputs().get(0).qualified());
        yes("SO3 is stored, not vented", !so3.fluidOutputs().get(0).vented());
        eq("oxidation is six seconds", 20 * 6, so3.durationTicks());
        eq("oxidation draws F1", 32L, so3.fuPerTick());
        yes("oxidation names no temperature", !so3.namesTemperature());
        eq("oxidation is oxidising", "OXIDISING", so3.atmosphere());
        eq("vanadia is the catalyst", "item:grindless:vanadia_pellet", so3.catalysts().get(0).qualified());
        yes("oxidation has no item input", so3.itemInputs().isEmpty());

        eq("absorption is the reactor", MachineFamily.CHEMICAL_REACTOR, acid.family());
        eq("absorption takes SO3", "fluid:grindless:sulfur_trioxide", acid.fluidInputs().get(0).qualified());
        eq("absorption takes 0.2 B water", 200, acid.fluidInputs().get(1).count());
        eq("absorption water is water", "fluid:minecraft:water", acid.fluidInputs().get(1).qualified());
        eq("absorption makes one bucket of acid", "fluid:grindless:sulfuric_acid",
                acid.fluidOutputs().get(0).qualified());
        yes("acid is stored, not vented", !acid.fluidOutputs().get(0).vented());
        eq("absorption is four seconds", 20 * 4, acid.durationTicks());
        yes("absorption names no temperature", !acid.namesTemperature());
        yes("absorption names no atmosphere", !acid.namesAtmosphere());
        yes("absorption has no catalyst", acid.catalysts().isEmpty());

        eq("pickle is the reactor", MachineFamily.CHEMICAL_REACTOR, pickle.family());
        eq("pickle takes an iron ingot", "tag:forge:ingots/iron", pickle.itemInputs().get(0).qualified());
        eq("pickle takes 0.1 B acid", 100, pickle.fluidInputs().get(0).count());
        eq("pickle makes a plate", "tag:forge:plates/iron", pickle.itemOutputs().get(0).qualified());
        eq("pickle is four seconds", 20 * 4, pickle.durationTicks());
        yes("pickle has no die", pickle.catalysts().isEmpty());

        eq("the reactor is the assembler", MachineFamily.ASSEMBLER, reactor.family());
        eq("the reactor takes a casing", "item:grindless:machine_casing",
                reactor.itemInputs().get(0).qualified());
        eq("the reactor takes two motors", 2, reactor.itemInputs().get(1).count());
        eq("the reactor motors are the reagent", "item:grindless:motor",
                reactor.itemInputs().get(1).qualified());
        eq("the reactor takes four plates", 4, reactor.itemInputs().get(2).count());
        eq("the reactor is twenty seconds", 20 * 20, reactor.durationTicks());
        eq("the reactor carries the Industrial milestone", "industrial", reactor.milestone());
        eq("the reactor makes the block", "item:grindless:chemical_reactor",
                reactor.itemOutputs().get(0).qualified());

        ProcessRecipe ironWash = recipe(recipes, "b2/iron");
        ProcessRecipe mythrilWash = recipe(recipes, "b2/mythril");
        ProcessRecipe ironWashedReduce = recipe(recipes, "b2_r1/iron");
        ProcessRecipe ironWashedRoast = recipe(recipes, "roast_washed/iron");
        ProcessRecipe washer = recipe(recipes, "assemble/chemical_washer");

        eq("wash is the washer", MachineFamily.CHEMICAL_WASHER, ironWash.family());
        eq("wash takes eight crushed", 8, ironWash.itemInputs().get(0).count());
        eq("wash feeds crushed iron", "tag:grindless:crushed_materials/iron",
                ironWash.itemInputs().get(0).qualified());
        eq("wash takes two buckets of water", 2000, ironWash.fluidInputs().get(0).count());
        eq("wash water is water", "fluid:minecraft:water", ironWash.fluidInputs().get(0).qualified());
        eq("wash makes eight washed", 8, ironWash.itemOutputs().get(0).count());
        eq("washed is the grindless tag", "tag:grindless:washed_crushed_materials/iron",
                ironWash.itemOutputs().get(0).qualified());
        eq("wash byproduct is the next metal", "tag:forge:ingots/mythril",
                ironWash.itemOutputs().get(1).qualified());
        eq("wash byproduct is one", 1, ironWash.itemOutputs().get(1).count());
        eq("the stable name order selects the next secondary", "tag:forge:ingots/gold",
                mythrilWash.itemOutputs().get(1).qualified());
        yes("the byproduct has a material sink",
                recipes.stream().anyMatch(candidate -> candidate.id().equals("mill/wire/mythril")
                        && candidate.itemInputs().get(0).id().equals("forge:ingots/mythril")));
        eq("wash is twenty seconds", 20 * 20, ironWash.durationTicks());
        eq("wash draws F1", 32L, ironWash.fuPerTick());
        yes("wash names no temperature", !ironWash.namesTemperature());
        yes("wash names no atmosphere", !ironWash.namesAtmosphere());
        yes("gold without washed does not wash",
                recipes.stream().noneMatch(candidate -> candidate.id().equals("b2/gold")));

        eq("washed reduction is the arc furnace", MachineFamily.ARC_FURNACE, ironWashedReduce.family());
        eq("washed reduction feeds washed", "tag:grindless:washed_crushed_materials/iron",
                ironWashedReduce.itemInputs().get(0).qualified());
        eq("washed reduction makes one ingot", 1, ironWashedReduce.itemOutputs().get(0).count());

        eq("washed roast is the kiln", MachineFamily.KILN, ironWashedRoast.family());
        eq("washed roast feeds washed", "tag:grindless:washed_crushed_materials/iron",
                ironWashedRoast.itemInputs().get(0).qualified());
        eq("washed roast vents SO2", "fluid:grindless:sulfur_dioxide",
                ironWashedRoast.ventedOutputs().get(0).qualified());

        eq("the washer is the assembler", MachineFamily.ASSEMBLER, washer.family());
        eq("the washer takes a casing", "item:grindless:machine_casing",
                washer.itemInputs().get(0).qualified());
        eq("the washer takes two motors", 2, washer.itemInputs().get(1).count());
        eq("the washer motors are the reagent", "item:grindless:motor",
                washer.itemInputs().get(1).qualified());
        eq("the washer takes four plates", 4, washer.itemInputs().get(2).count());
        eq("the washer is twenty seconds", 20 * 20, washer.durationTicks());
        eq("the washer carries the Industrial milestone", "industrial", washer.milestone());
        eq("the washer makes the block", "item:grindless:chemical_washer",
                washer.itemOutputs().get(0).qualified());

        ProcessRecipe split = recipe(recipes, "electrolysis/water");
        ProcessRecipe recombine = recipe(recipes, "recombine/water");
        ProcessRecipe air = recipe(recipes, "air/oxygen");
        ProcessRecipe cell = recipe(recipes, "assemble/electrolysis_cell");
        ProcessRecipe intake = recipe(recipes, "assemble/atmospheric_intake");

        eq("electrolysis is the cell", MachineFamily.ELECTROLYSIS_CELL, split.family());
        yes("electrolysis has no items", split.itemInputs().isEmpty() && split.itemOutputs().isEmpty());
        eq("electrolysis takes two buckets of water", 2000, split.fluidInputs().get(0).count());
        eq("electrolysis water is water", "fluid:minecraft:water", split.fluidInputs().get(0).qualified());
        eq("electrolysis stores two buckets of hydrogen", "fluid:grindless:hydrogen",
                split.fluidOutputs().get(0).qualified());
        eq("hydrogen is two buckets", 2000, split.fluidOutputs().get(0).count());
        yes("hydrogen is stored", !split.fluidOutputs().get(0).vented());
        eq("electrolysis vents one bucket of oxygen", "fluid:grindless:oxygen",
                split.ventedOutputs().get(0).qualified());
        eq("vented oxygen is one bucket", 1000, split.ventedOutputs().get(0).count());
        eq("electrolysis is ten seconds", 20 * 10, split.durationTicks());
        yes("electrolysis names no temperature", !split.namesTemperature());
        yes("electrolysis names no atmosphere", !split.namesAtmosphere());

        eq("recombination is the reactor", MachineFamily.CHEMICAL_REACTOR, recombine.family());
        eq("recombination takes two buckets of hydrogen", "fluid:grindless:hydrogen",
                recombine.fluidInputs().get(0).qualified());
        eq("recombination hydrogen is two buckets", 2000, recombine.fluidInputs().get(0).count());
        eq("recombination takes neighbour oxygen", "fluid:grindless:oxygen",
                recombine.fluidInputs().get(1).qualified());
        eq("recombination oxygen is one bucket", 1000, recombine.fluidInputs().get(1).count());
        eq("recombination makes two buckets of water", "fluid:minecraft:water",
                recombine.fluidOutputs().get(0).qualified());
        eq("recombination water is two buckets", 2000, recombine.fluidOutputs().get(0).count());
        yes("recombination water is stored", !recombine.fluidOutputs().get(0).vented());
        eq("recombination is eight seconds", 20 * 8, recombine.durationTicks());
        yes("recombination names no temperature", !recombine.namesTemperature());

        eq("air is the intake", MachineFamily.ATMOSPHERIC_INTAKE, air.family());
        yes("air has no inputs", air.itemInputs().isEmpty() && air.fluidInputs().isEmpty());
        eq("air stores two buckets of oxygen", "fluid:grindless:oxygen",
                air.fluidOutputs().get(0).qualified());
        eq("air oxygen is two buckets", 2000, air.fluidOutputs().get(0).count());
        yes("air oxygen is stored", !air.fluidOutputs().get(0).vented());
        yes("air does not vent", air.ventedOutputs().isEmpty());
        eq("air is ten seconds", 20 * 10, air.durationTicks());
        yes("air names no temperature", !air.namesTemperature());
        no("nitrogen is not emitted",
                recipes.stream().anyMatch(candidate -> candidate.outputs().stream()
                        .anyMatch(spec -> spec.id().contains("nitrogen"))));
        no("argon is not emitted",
                recipes.stream().anyMatch(candidate -> candidate.outputs().stream()
                        .anyMatch(spec -> spec.id().contains("argon"))));
        yes("roast still does not take oxygen",
                recipes.stream().filter(candidate -> candidate.id().equals("roast/iron"))
                        .allMatch(candidate -> candidate.fluidInputs().isEmpty()));

        eq("the cell is the assembler", MachineFamily.ASSEMBLER, cell.family());
        eq("the cell takes a casing", "item:grindless:machine_casing",
                cell.itemInputs().get(0).qualified());
        eq("the cell takes two motors", 2, cell.itemInputs().get(1).count());
        eq("the cell takes four plates", 4, cell.itemInputs().get(2).count());
        eq("the cell is twenty seconds", 20 * 20, cell.durationTicks());
        eq("the cell carries the Industrial milestone", "industrial", cell.milestone());
        eq("the cell makes the block", "item:grindless:electrolysis_cell",
                cell.itemOutputs().get(0).qualified());
        eq("the intake is the assembler", MachineFamily.ASSEMBLER, intake.family());
        eq("the intake makes the block", "item:grindless:atmospheric_intake",
                intake.itemOutputs().get(0).qualified());
        eq("the intake carries the Industrial milestone", "industrial", intake.milestone());

        ProcessRecipe well = recipe(recipes, "assemble/fluid_well");
        eq("the well is the assembler", MachineFamily.ASSEMBLER, well.family());
        eq("the well takes a casing", "item:grindless:machine_casing",
                well.itemInputs().get(0).qualified());
        eq("the well takes two motors", 2, well.itemInputs().get(1).count());
        eq("the well takes four plates", 4, well.itemInputs().get(2).count());
        eq("the well is twenty seconds", 20 * 20, well.durationTicks());
        eq("the well carries the Industrial milestone", "industrial", well.milestone());
        eq("the well makes the block", "item:grindless:fluid_well",
                well.itemOutputs().get(0).qualified());
        no("brine is not emitted",
                recipes.stream().anyMatch(candidate -> candidate.outputs().stream()
                        .anyMatch(spec -> spec.id().contains("brine"))));
        no("geothermal is not emitted",
                recipes.stream().anyMatch(candidate -> candidate.outputs().stream()
                        .anyMatch(spec -> spec.id().contains("geothermal"))));

        ProcessRecipe melt = recipe(recipes, "melt/iron");
        ProcessRecipe castIngot = recipe(recipes, "cast/ingot/iron");
        ProcessRecipe castPlate = recipe(recipes, "cast/plate/iron");
        ProcessRecipe induction = recipe(recipes, "assemble/induction_furnace");
        ProcessRecipe caster = recipe(recipes, "assemble/caster");
        ProcessRecipe ingotMould = recipe(recipes, "assemble/ingot_mould");
        ProcessRecipe plateMould = recipe(recipes, "assemble/plate_mould");

        eq("melt is the induction furnace", MachineFamily.INDUCTION_FURNACE, melt.family());
        eq("melt takes one iron ingot", "tag:forge:ingots/iron", melt.itemInputs().get(0).qualified());
        eq("melt makes 144 mB", "fluid:grindless:molten/iron", melt.fluidOutputs().get(0).qualified());
        eq("melt is one unit", 144, melt.fluidOutputs().get(0).count());
        yes("melt is stored", !melt.fluidOutputs().get(0).vented());
        eq("melt is 1000 C", 1000.0, melt.temperatureC());
        eq("melt is inert", "INERT", melt.atmosphere());
        eq("melt is eight seconds", 20 * 8, melt.durationTicks());
        yes("melt makes no slag", melt.itemOutputs().isEmpty());
        no("crushed is not melted",
                recipes.stream().anyMatch(candidate -> candidate.id().startsWith("melt/")
                        && candidate.itemInputs().stream().anyMatch(spec -> spec.id().contains("crushed"))));

        eq("ingot cast is the caster", MachineFamily.CASTER, castIngot.family());
        eq("ingot cast takes 144 mB", 144, castIngot.fluidInputs().get(0).count());
        eq("ingot cast makes one ingot", "tag:forge:ingots/iron", castIngot.itemOutputs().get(0).qualified());
        eq("ingot cast keeps the mould", "item:grindless:ingot_mould",
                castIngot.catalysts().get(0).qualified());
        yes("ingot cast names no temperature", !castIngot.namesTemperature());
        eq("ingot cast is four seconds", 20 * 4, castIngot.durationTicks());

        eq("plate cast is the caster", MachineFamily.CASTER, castPlate.family());
        eq("plate cast takes molten iron", "fluid:grindless:molten/iron",
                castPlate.fluidInputs().get(0).qualified());
        eq("plate cast takes 144 mB", 144, castPlate.fluidInputs().get(0).count());
        eq("plate cast keeps the plate mould", "item:grindless:plate_mould",
                castPlate.catalysts().get(0).qualified());
        eq("plate cast makes one plate", "tag:forge:plates/iron", castPlate.itemOutputs().get(0).qualified());
        yes("plate cast names no temperature", !castPlate.namesTemperature());
        no("mythril has no plate cast",
                recipes.stream().anyMatch(candidate -> candidate.id().equals("cast/plate/mythril")));

        eq("the induction furnace is the assembler", MachineFamily.ASSEMBLER, induction.family());
        eq("the induction furnace takes a casing", "item:grindless:machine_casing",
                induction.itemInputs().get(0).qualified());
        eq("the induction furnace takes two motors", 2, induction.itemInputs().get(1).count());
        eq("the induction furnace takes four plates", 4, induction.itemInputs().get(2).count());
        eq("the induction furnace is twenty seconds", 20 * 20, induction.durationTicks());
        eq("the induction furnace carries the Industrial milestone", "industrial", induction.milestone());
        eq("the induction furnace makes the block", "item:grindless:induction_furnace",
                induction.itemOutputs().get(0).qualified());
        eq("the caster is the assembler", MachineFamily.ASSEMBLER, caster.family());
        eq("the caster makes the block", "item:grindless:caster",
                caster.itemOutputs().get(0).qualified());
        eq("the ingot mould is the assembler", MachineFamily.ASSEMBLER, ingotMould.family());
        eq("the ingot mould takes four plates", 4, ingotMould.itemInputs().get(0).count());
        eq("the ingot mould is four seconds", 20 * 4, ingotMould.durationTicks());
        eq("the ingot mould carries the Industrial milestone", "industrial", ingotMould.milestone());
        eq("the ingot mould makes the item", "item:grindless:ingot_mould",
                ingotMould.itemOutputs().get(0).qualified());
        eq("the plate mould makes the item", "item:grindless:plate_mould",
                plateMould.itemOutputs().get(0).qualified());

        ProcessRecipe b3 = recipe(recipes, "b3/iron");
        ProcessRecipe b3Reduce = recipe(recipes, "b3_r1/iron");
        ProcessRecipe tailings = recipe(recipes, "tailings/r1/iron");
        ProcessRecipe soap = recipe(recipes, "reagent/surfactant");
        ProcessRecipe flotationCraft = recipe(recipes, "assemble/flotation_cell");
        ProcessRecipe magnet = recipe(recipes, "assemble/magnetic_separator");

        eq("flotation is the cell", MachineFamily.FLOTATION, b3.family());
        eq("flotation takes twenty crushed", 20, b3.itemInputs().get(0).count());
        eq("flotation crushed is the grindless tag", "tag:grindless:crushed_materials/iron",
                b3.itemInputs().get(0).qualified());
        eq("flotation takes 500 mB of surfactant", "fluid:grindless:surfactant",
                b3.fluidInputs().get(0).qualified());
        eq("flotation surfactant is half a bucket", 500, b3.fluidInputs().get(0).count());
        eq("flotation makes 24 concentrate", 24, b3.itemOutputs().get(0).count());
        eq("concentrate is the grindless tag", "tag:grindless:concentrates/iron",
                b3.itemOutputs().get(0).qualified());
        eq("flotation makes 3 tailings", 3, b3.itemOutputs().get(1).count());
        eq("tailings are the grindless tag", "tag:grindless:tailings/iron",
                b3.itemOutputs().get(1).qualified());
        eq("flotation is eighty seconds", 20 * 80, b3.durationTicks());
        yes("flotation names no temperature", !b3.namesTemperature());
        no("gold without crushed does not float",
                recipes.stream().anyMatch(candidate -> candidate.id().equals("b3/gold")));
        no("mythril concentrate does not roast",
                recipes.stream().anyMatch(candidate -> candidate.id().startsWith("roast")
                        && candidate.itemInputs().stream().anyMatch(spec -> spec.id().contains("concentrates"))));

        eq("concentrate reduction is the arc", MachineFamily.ARC_FURNACE, b3Reduce.family());
        eq("concentrate reduction feeds concentrate", "tag:grindless:concentrates/iron",
                b3Reduce.itemInputs().get(0).qualified());
        eq("concentrate reduction makes one ingot", 1, b3Reduce.itemOutputs().get(0).count());

        eq("tailings reduction takes ten", 10, tailings.itemInputs().get(0).count());
        eq("tailings reduction feeds tailings", "tag:grindless:tailings/iron",
                tailings.itemInputs().get(0).qualified());
        eq("tailings reduction makes one ingot", 1, tailings.itemOutputs().get(0).count());
        yes("mythril floats",
                recipes.stream().anyMatch(candidate -> candidate.id().equals("b3/mythril")));

        eq("surfactant is the reactor", MachineFamily.CHEMICAL_REACTOR, soap.family());
        eq("surfactant takes carbon", "tag:grindless:carbon", soap.itemInputs().get(0).qualified());
        eq("surfactant takes a bucket of water", 1000, soap.fluidInputs().get(0).count());
        eq("surfactant makes a bucket", "fluid:grindless:surfactant",
                soap.fluidOutputs().get(0).qualified());
        eq("surfactant is eight seconds", 20 * 8, soap.durationTicks());
        yes("surfactant names no temperature", !soap.namesTemperature());

        eq("the flotation cell is the assembler", MachineFamily.ASSEMBLER, flotationCraft.family());
        eq("the flotation cell takes a casing", "item:grindless:machine_casing",
                flotationCraft.itemInputs().get(0).qualified());
        eq("the flotation cell carries the Industrial milestone", "industrial", flotationCraft.milestone());
        eq("the flotation cell makes the block", "item:grindless:flotation_cell",
                flotationCraft.itemOutputs().get(0).qualified());
        eq("the magnet is the assembler", MachineFamily.ASSEMBLER, magnet.family());
        eq("the magnet makes the block", "item:grindless:magnetic_separator",
                magnet.itemOutputs().get(0).qualified());
        no("the magnet is not a process that changes an item",
                recipes.stream().anyMatch(candidate -> candidate.family() != MachineFamily.ASSEMBLER
                        && candidate.id().contains("magnetic")));

        ProcessRecipe boil = recipe(recipes, "boiler/steam");
        ProcessRecipe condense = recipe(recipes, "condense/water");
        ProcessRecipe solar = recipe(recipes, "assemble/solar_array");
        eq("the boiler turns water to steam", MachineFamily.BOILER, boil.family());
        eq("the boiler takes one bucket of water", 1000, boil.fluidInputs().get(0).count());
        eq("the boiler makes steam", "fluid:grindless:steam", boil.fluidOutputs().get(0).qualified());
        eq("the boiler is ten seconds", 20 * 10, boil.durationTicks());
        yes("the boiler names no temperature", !boil.namesTemperature());
        eq("the condenser is the steam sink", MachineFamily.CONDENSER, condense.family());
        eq("the condenser takes steam", "fluid:grindless:steam", condense.fluidInputs().get(0).qualified());
        eq("the condenser makes water", "fluid:minecraft:water", condense.fluidOutputs().get(0).qualified());
        eq("the condenser is four seconds", 20 * 4, condense.durationTicks());
        no("superheated steam is not emitted",
                recipes.stream().anyMatch(candidate -> candidate.outputs().stream()
                        .anyMatch(spec -> spec.id().contains("superheat"))));
        eq("the solar array is the assembler", MachineFamily.ASSEMBLER, solar.family());
        eq("the solar array makes the block", "item:grindless:solar_array",
                solar.itemOutputs().get(0).qualified());
        eq("daylight is F1", 32L, SolarLogic.generate(true));
        ProcessRecipe pipe = recipe(recipes, "assemble/pressure_pipe");
        ProcessRecipe pump = recipe(recipes, "assemble/electric_pump");
        ProcessRecipe tank = recipe(recipes, "assemble/industrial_tank");
        ProcessRecipe fluidArm = recipe(recipes, "assemble/fluid_manipulator");
        ProcessRecipe flux = recipe(recipes, "assemble/flux_belt");
        ProcessRecipe stackArm = recipe(recipes, "assemble/stack_manipulator");
        ProcessRecipe filterArm = recipe(recipes, "assemble/filter_manipulator");
        eq("the pressure pipe is the assembler", MachineFamily.ASSEMBLER, pipe.family());
        eq("the pressure pipe makes the block", "item:grindless:pressure_pipe",
                pipe.itemOutputs().get(0).qualified());
        eq("the electric pump is the assembler", MachineFamily.ASSEMBLER, pump.family());
        eq("the industrial tank is the assembler", MachineFamily.ASSEMBLER, tank.family());
        eq("the fluid arm is the assembler", MachineFamily.ASSEMBLER, fluidArm.family());
        eq("the flux belt is the assembler", MachineFamily.ASSEMBLER, flux.family());
        eq("the stack arm is the assembler", MachineFamily.ASSEMBLER, stackArm.family());
        eq("the filter arm is the assembler", MachineFamily.ASSEMBLER, filterArm.family());
        ProcessRecipe cable = recipe(recipes, "assemble/signal_cable");
        ProcessRecipe controller = recipe(recipes, "assemble/logic_controller");
        ProcessRecipe face = recipe(recipes, "assemble/redstone_interface");
        eq("the signal cable is the assembler", MachineFamily.ASSEMBLER, cable.family());
        eq("the signal cable makes the block", "item:grindless:signal_cable",
                cable.itemOutputs().get(0).qualified());
        eq("the logic controller is the assembler", MachineFamily.ASSEMBLER, controller.family());
        eq("the logic controller makes the block", "item:grindless:logic_controller",
                controller.itemOutputs().get(0).qualified());
        eq("the redstone interface is the assembler", MachineFamily.ASSEMBLER, face.family());
        eq("the redstone interface makes the block", "item:grindless:redstone_interface",
                face.itemOutputs().get(0).qualified());
        ProcessRecipe drill = recipe(recipes, "assemble/flux_drill");
        ProcessRecipe drillCell = recipe(recipes, "assemble/drill_cell");
        eq("the flux drill is the assembler", MachineFamily.ASSEMBLER, drill.family());
        eq("the flux drill makes the item", "item:grindless:flux_drill",
                drill.itemOutputs().get(0).qualified());
        eq("the drill is twenty seconds", 20 * 20, drill.durationTicks());
        eq("the drill cell is the assembler", MachineFamily.ASSEMBLER, drillCell.family());
        eq("the drill cell makes the item", "item:grindless:drill_cell",
                drillCell.itemOutputs().get(0).qualified());
        eq("the drill cell is four seconds", 20 * 4, drillCell.durationTicks());
        eq("the drill cell takes one coil", "item:grindless:copper_coil",
                drillCell.itemInputs().get(0).qualified());
        ProcessRecipe blueprintTool = recipe(recipes, "assemble/blueprint_tool");
        eq("the blueprint tool is the assembler", MachineFamily.ASSEMBLER, blueprintTool.family());
        eq("the blueprint tool makes the item", "item:grindless:blueprint_tool",
                blueprintTool.itemOutputs().get(0).qualified());
        no("a filled blueprint is not a recipe",
                recipes.stream().anyMatch(candidate -> candidate.id().equals("assemble/blueprint")));
        ProcessRecipe planner = recipe(recipes, "assemble/deconstruction_planner");
        eq("the planner is the assembler", MachineFamily.ASSEMBLER, planner.family());
        eq("the planner makes the item", "item:grindless:deconstruction_planner",
                planner.itemOutputs().get(0).qualified());
        ProcessRecipe scanner = recipe(recipes, "assemble/pattern_scanner");
        ProcessRecipe deconstructor = recipe(recipes, "assemble/deconstructor");
        eq("the scanner is the assembler", MachineFamily.ASSEMBLER, scanner.family());
        eq("the scanner makes the block", "item:grindless:pattern_scanner",
                scanner.itemOutputs().get(0).qualified());
        eq("the deconstructor is the assembler", MachineFamily.ASSEMBLER, deconstructor.family());
        eq("the deconstructor makes the block", "item:grindless:deconstructor",
                deconstructor.itemOutputs().get(0).qualified());
        eq("the exosuit helmet is the assembler", MachineFamily.ASSEMBLER,
                recipe(recipes, "assemble/flux_exosuit_helmet").family());
        eq("the network tap is an assembler item", "item:grindless:network_tap",
                recipe(recipes, "assemble/network_tap").itemOutputs().get(0).qualified());
        eq("the exoskeleton is an assembler item", "item:grindless:exoskeleton_legs",
                recipe(recipes, "assemble/exoskeleton_legs").itemOutputs().get(0).qualified());
        no("matter is not a recipe output",
                recipes.stream().anyMatch(candidate -> candidate.itemOutputs().stream()
                        .anyMatch(output -> "grindless:matter".equals(output.id()))));
        eq("night is nothing", 0L, SolarLogic.generate(false));

        yes("steel with an ingot still presses",
                recipes.stream().anyMatch(recipe -> recipe.id().equals("press/plate/steel")));
        ProcessRecipe steel = recipe(recipes, "alloy/steel");
        eq("steel is the arc furnace", MachineFamily.ARC_FURNACE, steel.family());
        eq("steel takes ten iron", 10, steel.itemInputs().get(0).count());
        eq("steel iron is ingots", "tag:forge:ingots/iron", steel.itemInputs().get(0).qualified());
        eq("steel takes one carbon", 1, steel.itemInputs().get(1).count());
        eq("steel carbon is the tag", "tag:grindless:carbon", steel.itemInputs().get(1).qualified());
        eq("steel makes ten ingots", 10, steel.itemOutputs().get(0).count());
        eq("steel output is the ingot tag", "tag:forge:ingots/steel", steel.itemOutputs().get(0).qualified());
        eq("steel is 140 seconds", 20 * 140, steel.durationTicks());
        eq("steel draws F1", 32L, steel.fuPerTick());
        eq("steel is 1600 C", 1600.0, steel.temperatureC());
        yes("steel names no atmosphere", !steel.namesAtmosphere());
        yes("steel has no catalyst", steel.catalysts().isEmpty());
        yes("steel has no fluid", steel.fluidInputs().isEmpty() && steel.fluidOutputs().isEmpty());
        yes("steel has no milestone", steel.milestone() == null);
        yes("furnace hold is optimal for 1600 C",
                ConditionBand.relative(ProcessLogic.STEEL_TEMPERATURE)
                        .isOptimal(ProcessLogic.REDUCE_TEMPERATURE));
        ProcessRecipe brick = recipe(recipes, "ceramic/refractory_brick");
        eq("refractory brick is the arc furnace", MachineFamily.ARC_FURNACE, brick.family());
        eq("refractory brick takes one slag", 1, brick.itemInputs().get(0).count());
        eq("refractory brick slag is the reagent", "item:grindless:slag",
                brick.itemInputs().get(0).qualified());
        eq("refractory brick makes one brick", 1, brick.itemOutputs().get(0).count());
        eq("refractory brick output is the reagent", "item:grindless:refractory_brick",
                brick.itemOutputs().get(0).qualified());
        eq("refractory brick is twenty seconds", 20 * 20, brick.durationTicks());
        eq("refractory brick draws F1", 32L, brick.fuPerTick());
        eq("refractory brick is 1400 C", 1400.0, brick.temperatureC());
        yes("refractory brick names no atmosphere", !brick.namesAtmosphere());
        yes("refractory brick has no catalyst", brick.catalysts().isEmpty());
        yes("refractory brick has no fluid",
                brick.fluidInputs().isEmpty() && brick.fluidOutputs().isEmpty());
        yes("refractory brick has no milestone", brick.milestone() == null);
        yes("furnace hold is optimal for 1400 C",
                ConditionBand.relative(ProcessLogic.REFRACTORY_TEMPERATURE)
                        .isOptimal(ProcessLogic.REDUCE_TEMPERATURE));
        yes("1400 C is inside the arc furnace envelope",
                ConditionBand.relative(ProcessLogic.REFRACTORY_TEMPERATURE).reachableWithin(
                        MachineEnvelopes.ARC_FURNACE.minTemperature(),
                        MachineEnvelopes.ARC_FURNACE.maxTemperature()));
        no("1400 C is outside the kiln",
                ConditionBand.relative(ProcessLogic.REFRACTORY_TEMPERATURE).reachableWithin(
                        MachineEnvelopes.KILN.minTemperature(),
                        MachineEnvelopes.KILN.maxTemperature()));
        no("alumina is not this slice",
                recipes.stream().anyMatch(recipe -> recipe.id().contains("alumina")
                        || recipe.itemInputs().stream().anyMatch(input -> input.id().contains("alumina"))));
        no("the brick does not take silica",
                brick.itemInputs().stream().anyMatch(input -> input.id().contains("silica")));
        ProcessRecipe silicon = recipe(recipes, "silicon/metallurgical");
        eq("metallurgical silicon is the arc furnace", MachineFamily.ARC_FURNACE, silicon.family());
        eq("silicon takes one silica", 1, silicon.itemInputs().get(0).count());
        eq("silicon silica is the tag", "tag:grindless:silica", silicon.itemInputs().get(0).qualified());
        eq("silicon takes two carbon", 2, silicon.itemInputs().get(1).count());
        eq("silicon carbon is the tag", "tag:grindless:carbon", silicon.itemInputs().get(1).qualified());
        eq("silicon makes one item", 1, silicon.itemOutputs().get(0).count());
        eq("silicon output is the reagent", "item:grindless:metallurgical_silicon",
                silicon.itemOutputs().get(0).qualified());
        eq("silicon vents two buckets of CO", 2000, silicon.ventedOutputs().get(0).count());
        eq("silicon CO is the named gas", "fluid:grindless:carbon_monoxide",
                silicon.ventedOutputs().get(0).qualified());
        yes("silicon CO is marked vented", silicon.ventedOutputs().get(0).vented());
        eq("silicon is fourteen seconds", 20 * 14, silicon.durationTicks());
        eq("silicon draws F1", 32L, silicon.fuPerTick());
        eq("silicon is 1900 C", 1900.0, silicon.temperatureC());
        eq("silicon names reducing", "REDUCING", silicon.atmosphere());
        yes("silicon has no catalyst", silicon.catalysts().isEmpty());
        yes("silicon has no milestone", silicon.milestone() == null);
        yes("silicon has no slag", silicon.itemOutputs().stream().noneMatch(output -> output.id().contains("slag")));
        ConditionBand siliconBand = ConditionBand.relative(ProcessLogic.SILICON_TEMPERATURE);
        yes("furnace hold admits 1900 C", siliconBand.admits(ProcessLogic.REDUCE_TEMPERATURE));
        no("furnace hold is not optimal for 1900 C", siliconBand.isOptimal(ProcessLogic.REDUCE_TEMPERATURE));
        eq("the furnace still holds 1500 C", 1500.0, ProcessLogic.REDUCE_TEMPERATURE);
        yes("1900 C is inside the arc furnace envelope",
                siliconBand.reachableWithin(
                        MachineEnvelopes.ARC_FURNACE.minTemperature(),
                        MachineEnvelopes.ARC_FURNACE.maxTemperature()));
        yes("the arc envelope can hold 1900 C at full speed",
                siliconBand.optimallyReachableWithin(
                        MachineEnvelopes.ARC_FURNACE.minTemperature(),
                        MachineEnvelopes.ARC_FURNACE.maxTemperature()));
        no("1900 C is outside the kiln",
                siliconBand.reachableWithin(
                        MachineEnvelopes.KILN.minTemperature(),
                        MachineEnvelopes.KILN.maxTemperature()));
        no("siemens, wafers and boules are not this slice",
                recipes.stream().anyMatch(recipe -> recipe.id().contains("siemens")
                        || recipe.id().contains("wafer")
                        || recipe.id().contains("boule")
                        || recipe.id().contains("trichlorosilane")));
        ProcessRecipe zone = recipe(recipes, "silicon/zone_refining");
        eq("zone refining is the arc furnace", MachineFamily.ARC_FURNACE, zone.family());
        eq("zone refining takes ten metallurgical silicon", 10, zone.itemInputs().get(0).count());
        eq("zone refining feed is the reagent", "item:grindless:metallurgical_silicon",
                zone.itemInputs().get(0).qualified());
        eq("zone refining makes seven electronic silicon", 7, zone.itemOutputs().get(0).count());
        eq("zone refining output is the reagent", "item:grindless:electronic_silicon",
                zone.itemOutputs().get(0).qualified());
        eq("zone refining is six hundred seconds", 20 * 600, zone.durationTicks());
        eq("zone refining draws F1", 32L, zone.fuPerTick());
        eq("zone refining is 1420 C", 1420.0, zone.temperatureC());
        yes("zone refining names no atmosphere", !zone.namesAtmosphere());
        yes("zone refining has no catalyst", zone.catalysts().isEmpty());
        yes("zone refining has no fluid", zone.fluidInputs().isEmpty() && zone.fluidOutputs().isEmpty());
        yes("zone refining has no milestone", zone.milestone() == null);
        yes("zone refining has no slag",
                zone.itemOutputs().stream().noneMatch(output -> output.id().contains("slag")));
        ConditionBand zoneBand = ConditionBand.relative(ProcessLogic.ZONE_TEMPERATURE);
        yes("furnace hold is optimal for 1420 C", zoneBand.isOptimal(ProcessLogic.REDUCE_TEMPERATURE));
        no("a ±5 band at 1420 C would refuse the hold",
                ConditionBand.absolute(ProcessLogic.ZONE_TEMPERATURE, 5.0)
                        .admits(ProcessLogic.REDUCE_TEMPERATURE));
        eq("the furnace still holds 1500 C", 1500.0, ProcessLogic.REDUCE_TEMPERATURE);
        yes("1420 C is inside the arc furnace envelope",
                zoneBand.reachableWithin(
                        MachineEnvelopes.ARC_FURNACE.minTemperature(),
                        MachineEnvelopes.ARC_FURNACE.maxTemperature()));
        no("1420 C is outside the kiln",
                zoneBand.reachableWithin(
                        MachineEnvelopes.KILN.minTemperature(),
                        MachineEnvelopes.KILN.maxTemperature()));
        no("electronic silicon is not an ingot tag",
                zone.itemOutputs().stream().anyMatch(output -> output.id().contains("forge:ingots/silicon")));
        no("silicon is not a supplied ingot tag",
                silicon.itemOutputs().stream().anyMatch(output -> output.id().contains("forge:ingots/silicon")));
        no("the kiln does not fire the brick",
                recipes.stream().anyMatch(recipe -> recipe.family() == MachineFamily.KILN
                        && recipe.id().contains("refractory")));

        // Steelmaking is the electric arc and nothing else. The gas line (ADR-0077) does make
        // oxygen and hydrogen, so these assertions scope to the alloy route rather than the
        // whole graph, which is what they always meant.
        no("oxygen blow is not this slice",
                recipes.stream().filter(recipe -> recipe.id().startsWith("alloy/"))
                        .anyMatch(recipe -> recipe.itemInputs().stream()
                                .anyMatch(input -> input.id().contains("oxygen"))
                                || recipe.fluidInputs().stream()
                                        .anyMatch(input -> input.id().contains("oxygen"))));
        no("direct reduction is not this slice",
                recipes.stream().anyMatch(recipe -> recipe.id().contains("direct")));
        no("no alloy route reduces with hydrogen",
                recipes.stream().filter(recipe -> recipe.id().startsWith("alloy/"))
                        .anyMatch(recipe -> recipe.fluidInputs().stream()
                                .anyMatch(input -> input.id().contains("hydrogen"))));
        no("steel without a vein has no ore line",
                recipes.stream().anyMatch(recipe -> recipe.id().equals("b0_r1/steel")));
        no("steel without oxide does not roast",
                recipes.stream().anyMatch(recipe -> recipe.id().startsWith("roast") && recipe.id().endsWith("/steel")));
        no("steel without oxide has no R2",
                recipes.stream().anyMatch(recipe -> recipe.id().equals("r2/steel")));
        yes("gold without crushed still presses",
                recipes.stream().anyMatch(recipe -> recipe.id().equals("press/gear/gold")));
        no("mythril without plate/rod/gear does not press",
                recipes.stream().anyMatch(recipe -> recipe.id().startsWith("press/") && recipe.id().endsWith("/mythril")));
        no("the array is not a dyson collector",
                recipes.stream().anyMatch(recipe -> recipe.id().contains("dyson")
                        || recipe.id().contains("kardashev")
                        || recipe.id().contains("stellar_forge")));
    }

    private static void assemblerCrafts(List<ProcessRecipe> recipes) throws IOException {
        List<String> dieNames = List.of("plate_die", "rod_die", "gear_die", "coil_die");
        List<String> machineNames = List.of("pulverizer", "arc_furnace", "press", "assembler");
        yes("the four existing manual Press dies are the only authored die crafts",
                dieNames.equals(T1Recipes.shaped().stream().filter(craft -> craft.name().endsWith("_die"))
                        .map(T1Recipes.Shaped::name).toList()));
        List<String> automaticDies = recipes.stream()
                .filter(row -> row.id().startsWith("assemble/")
                        && row.itemOutputs().stream().anyMatch(output -> output.id().endsWith("_die")))
                .map(row -> row.id().substring("assemble/".length())).toList();
        yes("only the explicitly selected Press dies have automatic routes", dieNames.equals(automaticDies));
        yes("the four directly projected first-factory machines keep their automatic routes", machineNames.equals(
                recipes.stream().filter(row -> machineNames.stream()
                                .anyMatch(name -> row.id().equals("assemble/" + name)))
                        .map(row -> row.id().substring("assemble/".length())).toList()));

        for (String name : List.of("machine_casing", "flux_cell", "plate_die", "rod_die", "gear_die", "coil_die",
                "pulverizer", "arc_furnace", "press", "assembler")) {
            T1Recipes.Shaped craft = T1Recipes.shaped().stream().filter(row -> row.name().equals(name))
                    .findFirst().orElseThrow();
            String id = "assemble/" + name;
            boolean requiresMinimumControl = machineNames.contains(name);
            Map<String, Integer> inputs = shapedInputs(craft.pattern(), craft.key());
            projection(recipes, MachineFamily.ASSEMBLER, id, inputs, craft.result(), craft.resultCount(),
                    FabricationLogic.ASSEMBLE_TICKS, FabricationLogic.FU_PER_TICK);
            ProcessRecipe automatic = recipe(recipes, id);
            eq(id + " keeps exactly the source craft's grouped item inputs", inputs.size(),
                    automatic.itemInputs().size());
            yes(id + " fits the existing four Assembler input slots", automatic.itemInputs().size() <= 4);
            yes(id + " has no fluid input, output or hidden material", automatic.fluidInputs().isEmpty()
                    && automatic.fluidOutputs().isEmpty());
            no(id + " adds no physical condition or permission", automatic.namesTemperature() || automatic.namesPressure()
                    || automatic.namesAtmosphere() || automatic.namesAgitation() || automatic.milestone() != null);
            List<IngredientSpec> minimumControlInputs = automatic.itemInputs().stream()
                    .filter(IngredientSpec::isMinimumControl).toList();
            yes(id + " selects only its explicit minimum-control Relay operand",
                    (requiresMinimumControl
                            ? List.of(IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1))
                            : List.<IngredientSpec>of()).equals(minimumControlInputs));
            yes(id + " keeps all other material operands exact",
                    automatic.itemInputs().stream().filter(input -> !input.isMinimumControl())
                            .allMatch(input -> input.matrixRole() == IngredientSpec.MatrixRole.EXACT));
            yes(id + " adds no unconsumed catalyst", automatic.catalysts().isEmpty());
            no(id + " is not an earlier Bench route", recipes.stream()
                    .filter(row -> row.family() == MachineFamily.ASSEMBLY_BENCH)
                    .flatMap(row -> row.itemOutputs().stream()).anyMatch(output -> output.id().equals(craft.result())));

            JsonObject json = read(name);
            List<String> pattern = new ArrayList<>();
            json.getAsJsonArray("pattern").forEach(row -> pattern.add(row.getAsString()));
            Map<String, String> key = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("key").entrySet()) {
                JsonObject ingredient = entry.getValue().getAsJsonObject();
                yes(name + " JSON keeps an ordinary item or tag ingredient " + entry.getKey(),
                        ingredient.size() == 1 && (ingredient.has("tag") || ingredient.has("item")));
                String kind = ingredient.has("tag") ? "tag" : "item";
                key.put(entry.getKey(), kind + ":" + ingredient.get(kind).getAsString());
            }
            yes(name + " keeps the full shipped crafting pattern", pattern.equals(craft.pattern()));
            yes(name + " keeps the full shipped crafting key", key.equals(craft.key()));
            yes(id + " also matches the exact shipped JSON input multiset",
                    shapedInputs(pattern, key).equals(inputs));
            JsonObject result = json.getAsJsonObject("result");
            int count = result.has("count") ? result.get("count").getAsInt() : 1;
            yes(id + " also matches the exact shipped JSON output multiset", automatic.outputs().equals(
                    List.of(OutputSpec.item(result.get("item").getAsString(), count))));
        }
    }

    private static void thermalGeneratorAssembly(List<ProcessRecipe> recipes) throws IOException {
        T1Recipes.Shaped craft = T1Recipes.shaped().stream()
                .filter(row -> row.name().equals("thermal_generator")).findFirst().orElseThrow();
        Map<String, Integer> manual = shapedInputs(craft.pattern(), craft.key());
        eq("the unchanged manual Thermal craft has five distinct groups", 5, manual.size());
        yes("the manual Thermal bill retains all nine physical units", manual.equals(Map.of(
                T1Recipes.COBBLE, 5, T1Recipes.FURNACE, 1, T1Recipes.CARBON, 1,
                T1Recipes.IRON, 1, T1Recipes.RELAY_MATRIX, 1)));
        ProcessRecipe core = recipe(recipes, "assemble/thermal_generator_core");
        ProcessRecipe finished = recipe(recipes, "assemble/thermal_generator");
        Map<String, Integer> coreInputs = new LinkedHashMap<>();
        for (String input : List.of(T1Recipes.FURNACE, T1Recipes.CARBON, T1Recipes.IRON)) {
            coreInputs.put(input, manual.get(input));
        }
        Map<String, Integer> finalInputs = new LinkedHashMap<>();
        finalInputs.put("item:" + FabricationLogic.THERMAL_GENERATOR_CORE, 1);
        finalInputs.put(T1Recipes.COBBLE, manual.get(T1Recipes.COBBLE));
        finalInputs.put(T1Recipes.RELAY_MATRIX, manual.get(T1Recipes.RELAY_MATRIX));
        projection(recipes, MachineFamily.ASSEMBLER, core.id(), coreInputs,
                FabricationLogic.THERMAL_GENERATOR_CORE, 1,
                FabricationLogic.ASSEMBLE_TICKS, FabricationLogic.FU_PER_TICK);
        projection(recipes, MachineFamily.ASSEMBLER, finished.id(), finalInputs, craft.result(), craft.resultCount(),
                FabricationLogic.ASSEMBLE_TICKS, FabricationLogic.FU_PER_TICK);
        yes("the core takes only furnace, carbon and iron in stable indexed order", core.inputs().equals(List.of(
                IngredientSpec.item("minecraft:furnace", manual.get(T1Recipes.FURNACE)),
                IngredientSpec.tag("grindless:carbon", manual.get(T1Recipes.CARBON)),
                IngredientSpec.tag("forge:ingots/iron", manual.get(T1Recipes.IRON)))));
        yes("the final operation takes core, cobblestone and exactly one minimum-F1 controller",
                finished.inputs().equals(List.of(
                        IngredientSpec.item(FabricationLogic.THERMAL_GENERATOR_CORE, 1),
                        IngredientSpec.tag("minecraft:stone_crafting_materials", manual.get(T1Recipes.COBBLE)),
                        IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1))));
        for (ProcessRecipe stage : List.of(core, finished)) {
            eq(stage.id() + " uses exactly three actual grouped input slots", 3, stage.itemInputs().size());
            no(stage.id() + " introduces no fluid or hidden condition", !stage.fluidInputs().isEmpty()
                    || !stage.fluidOutputs().isEmpty() || stage.namesTemperature() || stage.namesPressure()
                    || stage.namesAtmosphere() || stage.namesAgitation() || stage.milestone() != null);
            no(stage.id() + " has no Bench bootstrap bypass", recipes.stream()
                    .filter(row -> row.family() == MachineFamily.ASSEMBLY_BENCH)
                    .flatMap(row -> row.outputs().stream()).anyMatch(stage.outputs()::contains));
        }
        yes("the core has no Matrix, controller role or hidden duplicate", core.inputs().stream()
                .allMatch(input -> input.matrixRating() == 0 && !input.id().equals(FabricationLogic.RELAY_MATRIX)));
        yes("higher control stock never changes the ordinary Thermal output", finished.outputs().equals(
                List.of(OutputSpec.item("grindless:thermal_generator", 1))));
        Map<String, Integer> composed = new LinkedHashMap<>();
        core.inputs().forEach(input -> composed.merge(input.qualified(), input.count(), Integer::sum));
        finished.inputs().forEach(input -> composed.merge(input.qualified(), input.count(), Integer::sum));
        yes("exactly one manufactured core cancels from the composed bill",
                Integer.valueOf(1).equals(composed.remove("item:" + FabricationLogic.THERMAL_GENERATOR_CORE))
                        && core.outputs().equals(List.of(OutputSpec.item(FabricationLogic.THERMAL_GENERATOR_CORE, 1))));
        yes("cancelling only the one core preserves the exact complete manual multiset", manual.equals(composed));
        JsonObject json = read("thermal_generator");
        List<String> pattern = new ArrayList<>();
        json.getAsJsonArray("pattern").forEach(row -> pattern.add(row.getAsString()));
        Map<String, String> key = new LinkedHashMap<>();
        json.getAsJsonObject("key").entrySet().forEach(entry -> {
            JsonObject ingredient = entry.getValue().getAsJsonObject();
            yes("manual Thermal JSON keeps ordinary material " + entry.getKey(),
                    ingredient.size() == 1 && (ingredient.has("item") || ingredient.has("tag")));
            String kind = ingredient.has("tag") ? "tag" : "item";
            key.put(entry.getKey(), kind + ":" + ingredient.get(kind).getAsString());
        });
        yes("the Thermal catalogue and shipped JSON retain the same complete pattern", craft.pattern().equals(pattern));
        yes("the Thermal catalogue and shipped JSON retain the same complete key", craft.key().equals(key));
        yes("both operations compose to the exact shipped JSON material bill", composed.equals(shapedInputs(pattern, key)));
        JsonObject result = json.getAsJsonObject("result");
        yes("the output remains the ordinary unchanged manual JSON item without state",
                result.size() == 1 && finished.outputs().equals(
                        List.of(OutputSpec.item(result.get("item").getAsString(), 1))));
        eq("Thermal reproduction pays two normal cycles, not one", 2 * FabricationLogic.ASSEMBLE_TICKS,
                Math.addExact(core.durationTicks(), finished.durationTicks()));
        eq("the composed Thermal cycle is forty seconds at full power", 800,
                Math.addExact(core.durationTicks(), finished.durationTicks()));
        long energy = Math.addExact(Math.multiplyExact((long) core.durationTicks(), core.fuPerTick()),
                Math.multiplyExact((long) finished.durationTicks(), finished.fuPerTick()));
        eq("the composed Thermal demand pays each normal operation", 25_600L, energy);
        eq("every generated identity remains unique", (long) recipes.size(),
                recipes.stream().map(ProcessRecipe::id).distinct().count());
    }

    /** Vanilla carriers test index/quantity selection, not mod registration or native Matrix admission. */
    private static void thermalInputSelection(List<ProcessRecipe> recipes) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Map<TagKey<Item>, List<Holder<Item>>> originalTags = new HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(tag ->
                originalTags.put(tag.getFirst(), tag.getSecond().stream().toList()));
        Map<TagKey<Item>, List<Holder<Item>>> tags = new HashMap<>(originalTags);
        tags.put(TagKey.create(Registries.ITEM, new ResourceLocation("minecraft:stone_crafting_materials")),
                List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.COBBLESTONE)));
        tags.put(TagKey.create(Registries.ITEM, new ResourceLocation("grindless:carbon")),
                List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.CHARCOAL)));
        tags.put(TagKey.create(Registries.ITEM, new ResourceLocation("forge:ingots/iron")),
                List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.IRON_INGOT)));
        ProcessRecipe finished = recipe(recipes, "assemble/thermal_generator");
        List<IngredientSpec> boundInputs = List.of(
                IngredientSpec.item("minecraft:heart_of_the_sea", finished.itemInputs().get(0).count()),
                finished.itemInputs().get(1),
                IngredientSpec.item("minecraft:comparator", finished.itemInputs().get(2).count()));
        ProcessRecipe bound = new ProcessRecipe(finished.id(), finished.family(), boundInputs, finished.outputs(),
                finished.temperatureC(), finished.atmosphere(), finished.durationTicks(), finished.fuPerTick());
        List<ProcessRecipe> indexed = new ArrayList<>(recipes);
        indexed.set(indexed.indexOf(finished), bound);
        Field graph = ProcessLookup.class.getDeclaredField("GRAPH");
        graph.setAccessible(true);
        Object original = graph.get(null);
        Method index = original.getClass().getDeclaredMethod("index", List.class);
        index.setAccessible(true);
        try {
            BuiltInRegistries.ITEM.bindTags(tags);
            graph.set(null, index.invoke(null, indexed));
            for (int stock : List.of(1, 64)) {
                ItemStack[] core = {new ItemStack(Items.FURNACE, stock), new ItemStack(Items.CHARCOAL, stock),
                        new ItemStack(Items.IRON_INGOT, stock), ItemStack.EMPTY};
                eq("complete/bulk core stock selects the real furnace-first Thermal route: " + stock,
                        "assemble/thermal_generator_core",
                        ProcessLookup.find(MachineFamily.ASSEMBLER, core).orElseThrow().id());
                core[2] = ItemStack.EMPTY;
                no("a missing core material cannot select a generic alternative: " + stock,
                        ProcessLookup.find(MachineFamily.ASSEMBLER, core).isPresent());
                ItemStack[] finalStock = {new ItemStack(Items.HEART_OF_THE_SEA, stock),
                        new ItemStack(Items.COBBLESTONE, Math.max(5, stock)), new ItemStack(Items.COMPARATOR, stock),
                        ItemStack.EMPTY};
                eq("complete/bulk final stock selects Thermal rather than generic cobblestone (carriers): " + stock,
                        finished.id(), ProcessLookup.find(MachineFamily.ASSEMBLER, finalStock).orElseThrow().id());
                finalStock[1].setCount(4);
                no("insufficient cobblestone cannot start final Thermal (carriers): " + stock,
                        ProcessLookup.find(MachineFamily.ASSEMBLER, finalStock).isPresent());
                finalStock[1].setCount(Math.max(5, stock));
                finalStock[2] = ItemStack.EMPTY;
                no("bulk cobblestone never bypasses the required final controller group (carriers): " + stock,
                        ProcessLookup.find(MachineFamily.ASSEMBLER, finalStock).isPresent());
            }
            eq("generic bulk cobblestone still selects the unchanged furnace construction route",
                    "assemble/bootstrap/furnace", ProcessLookup.find(MachineFamily.ASSEMBLER, new ItemStack[]{
                            new ItemStack(Items.COBBLESTONE, 64), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY})
                            .orElseThrow().id());
        } finally {
            graph.set(null, original);
            BuiltInRegistries.ITEM.bindTags(originalTags);
        }
    }

    private static Map<String, Integer> shapedInputs(List<String> pattern, Map<String, String> key) {
        Map<String, Integer> inputs = new LinkedHashMap<>();
        for (String row : pattern) {
            for (char symbol : row.toCharArray()) {
                if (symbol != ' ') {
                    inputs.merge(key.get(String.valueOf(symbol)), 1, Integer::sum);
                }
            }
        }
        return inputs;
    }

    private static void c3Routes(List<ProcessRecipe> recipes) {
        no("the Assembly Bench never fabricates a Relay Matrix (ADR-0137)",
                recipes.stream().filter(recipe -> recipe.family() == MachineFamily.ASSEMBLY_BENCH)
                        .flatMap(recipe -> recipe.itemOutputs().stream())
                        .anyMatch(output -> output.id().equals("grindless:relay_matrix")));
        List<String> renewableIds = List.of(
                "source/geological_aggregate", "source/biomass", "bootstrap/dry_aggregate",
                "bootstrap/wet_aggregate", "bootstrap/flint_separation", "bootstrap/fibreboard",
                "bootstrap/paper", "bootstrap/sticks", "bootstrap/biocarbon", "bootstrap/glass",
                "bootstrap/reduce_iron", "bootstrap/reduce_copper");
        List<ProcessRecipe> renewable = RenewableProcesses.recipes();
        yes("the twelve C3 source/material routes have explicit provenance",
                renewable.stream().map(ProcessRecipe::id).toList().equals(renewableIds));
        for (ProcessRecipe row : renewable) {
            yes(row.id() + " reaches the graph unchanged from RenewableProcesses", recipes.contains(row));
        }

        // BootstrapAssembly projects 28 shaped crafts, the shapeless core and four vanilla basics.
        List<String> primitiveCrafts = List.of(
                "multitool", "hand_crank_dynamo", "crude_extractor", "research_terminal",
                "quern", "clay_mixer", "firing_chamber", "settling_trough", "settling_channel",
                "settling_screen", "hand_pump", "basic_tank", "clay_conduit", "conveyor_belt",
                "crude_manipulator", "process_atlas", "quest_book", "crank_handle",
                "geological_collector", "biocultivator", "assembly_bench", "biomass_dynamo",
                "workshop_item_input", "workshop_item_output", "settling_item_input", "settling_fluid_input",
                "settling_item_output", "settling_fluid_output",
                "data_core", "chest", "crafting_table", "furnace", "brick_block");
        for (MachineFamily family : List.of(MachineFamily.ASSEMBLY_BENCH, MachineFamily.ASSEMBLER)) {
            boolean bench = family == MachineFamily.ASSEMBLY_BENCH;
            String prefix = bench ? "bench/" : "assemble/bootstrap/";
            List<String> projectedIds = recipes.stream().map(ProcessRecipe::id)
                    .filter(id -> id.startsWith(prefix)).map(id -> id.substring(prefix.length())).toList();
            yes(family + " has exactly the thirty-three primitive construction projections",
                    projectedIds.equals(primitiveCrafts));
            no(family + " cannot manufacture a retired Guide", recipes.stream()
                    .filter(recipe -> recipe.family() == family).flatMap(recipe -> recipe.itemOutputs().stream())
                    .anyMatch(output -> output.id().equals("grindless:field_guide")));
            for (BootstrapRecipes.Shaped craft : BootstrapRecipes.shaped()) {
                projection(recipes, family, prefix + craft.name(), shapedInputs(craft.pattern(), craft.key()),
                        craft.result(), craft.resultCount());
            }
            for (BootstrapRecipes.Shapeless craft : BootstrapRecipes.shapeless()) {
                Map<String, Integer> inputs = new LinkedHashMap<>();
                craft.ingredients().forEach(input -> inputs.merge(input, 1, Integer::sum));
                projection(recipes, family, prefix + craft.name(), inputs, craft.result(), 1);
            }
            projection(recipes, family, prefix + "chest", Map.of(BootstrapRecipes.PLANKS, 8),
                    "minecraft:chest", 1);
            projection(recipes, family, prefix + "crafting_table", Map.of(BootstrapRecipes.PLANKS, 4),
                    "minecraft:crafting_table", 1);
            projection(recipes, family, prefix + "furnace", Map.of(BootstrapRecipes.COBBLE, 8),
                    "minecraft:furnace", 1);
            projection(recipes, family, prefix + "brick_block", Map.of("item:minecraft:brick", 4),
                    "minecraft:bricks", 1);
        }
    }

    private static void projection(List<ProcessRecipe> recipes, MachineFamily family, String id,
                                   Map<String, Integer> inputs, String output, int count) {
        boolean bench = family == MachineFamily.ASSEMBLY_BENCH;
        projection(recipes, family, id, inputs, output, count,
                bench ? SourceLogic.BENCH_TICKS : SourceLogic.INDUSTRIAL_ASSEMBLY_TICKS,
                bench ? FluxTier.F0.nominal() : FluxTier.F1.nominal());
    }

    private static void projection(List<ProcessRecipe> recipes, MachineFamily family, String id,
                                   Map<String, Integer> inputs, String output, int count, int ticks, long power) {
        ProcessRecipe row = recipe(recipes, id);
        Map<String, Integer> actualInputs = new LinkedHashMap<>();
        row.inputs().forEach(input -> actualInputs.merge(input.qualified(), input.count(), Integer::sum));
        yes(id + " preserves every crafting input quantity", actualInputs.equals(inputs));
        yes(id + " preserves the complete crafting output batch",
                row.outputs().equals(List.of(OutputSpec.item(output, count))));
        yes(id + " fits four input slots without a catalyst",
                row.itemInputs().size() <= 4 && row.catalysts().isEmpty());
        eq(id + " runs on its declared station", family, row.family());
        eq(id + " keeps its station's cycle", ticks, row.durationTicks());
        eq(id + " keeps its station's power", power, row.fuPerTick());
    }

    private static void legacyGuideWork(List<ProcessRecipe> recipes) {
        for (MachineFamily family : List.of(MachineFamily.ASSEMBLY_BENCH, MachineFamily.ASSEMBLER)) {
            String id = family == MachineFamily.ASSEMBLY_BENCH ? "bench/field_guide" : "assemble/bootstrap/field_guide";
            no("the retired recipe is not a fresh selectable graph row " + id,
                    recipes.stream().anyMatch(recipe -> recipe.id().equals(id)));
            for (double unpaid : List.of(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
                yes("unpaid or invalid old work cannot start a Guide " + id + "/" + unpaid,
                        LegacyGuideAssembly.resume(family, id, unpaid).isEmpty());
            }
            ProcessRecipe legacy = LegacyGuideAssembly.resume(family, id, 0.5).orElseThrow();
            eq("pending old work keeps its exact recipe identity", id, legacy.id());
            eq("pending old work keeps its exact station", family, legacy.family());
            yes("legacy paper/stick inputs are preserved exactly", legacy.itemInputs().equals(List.of(
                    IngredientSpec.item("minecraft:paper", 2), IngredientSpec.item("minecraft:stick", 1))));
            yes("legacy work can produce only one harmless registered Guide", legacy.itemOutputs()
                    .equals(List.of(OutputSpec.item("grindless:field_guide", 1))));
            eq("legacy work keeps the station's original cycle",
                    family == MachineFamily.ASSEMBLY_BENCH ? SourceLogic.BENCH_TICKS : SourceLogic.INDUSTRIAL_ASSEMBLY_TICKS,
                    legacy.durationTicks());
            eq("legacy work keeps the station's original FU draw",
                    family == MachineFamily.ASSEMBLY_BENCH ? FluxTier.F0.nominal() : FluxTier.F1.nominal(),
                    legacy.fuPerTick());
            yes("a cleared owner cannot restart the legacy recipe", LegacyGuideAssembly.resume(family, "", 0.5).isEmpty());
            yes("a settled legacy job cannot restart itself", LegacyGuideAssembly.resume(family, id, 0.0).isEmpty());
            yes("a similarly named new recipe is not legacy work",
                    LegacyGuideAssembly.resume(family, id + "_copy", 1.0).isEmpty());
        }
        yes("an unrelated family cannot resume legacy Guide work",
                LegacyGuideAssembly.resume(MachineFamily.QUERN, "bench/field_guide", 1.0).isEmpty());
    }

    private static void logic() {
        eq("full power is one tick of work", 1.0, ProcessLogic.work(32L, 32L, 1.0, 1.0));
        eq("half power is half work", 0.5, ProcessLogic.work(16L, 32L, 1.0, 1.0));
        eq("half efficiency is half work", 0.5, ProcessLogic.work(32L, 32L, 0.5, 1.0));
        eq("a 0.5 time multiplier doubles work", 2.0, ProcessLogic.work(32L, 32L, 1.0, 0.5));
        eq("zero draw is zero work", 0.0, ProcessLogic.work(0L, 32L, 1.0, 1.0));
        eq("out of band is zero work", 0.0, ProcessLogic.work(32L, 32L, 0.0, 1.0));

        eq("empty is idle", MachineStatus.IDLE,
                ProcessLogic.status(false, false, false, true, false, false));
        eq("wrong conditions are out of band", MachineStatus.OUT_OF_BAND,
                ProcessLogic.status(true, false, false, false, true, false));
        eq("full output is blocked", MachineStatus.BLOCKED,
                ProcessLogic.status(true, false, true, true, true, true));
        eq("missing carbon is starved", MachineStatus.STARVED,
                ProcessLogic.status(true, true, false, true, true, false));
        eq("no power is starved", MachineStatus.STARVED,
                ProcessLogic.status(true, false, false, true, false, false));
        eq("working is running", MachineStatus.RUNNING,
                ProcessLogic.status(true, false, false, true, true, true));

        eq("a burning generator makes F1", 32L, ThermalLogic.generate(true));
        eq("an idle generator makes nothing", 0L, ThermalLogic.generate(false));
        eq("burning with a sink is running", MachineStatus.RUNNING,
                ThermalLogic.status(true, 32L, 0));
        eq("burning with nowhere to go is blocked", MachineStatus.BLOCKED,
                ThermalLogic.status(true, 32L, 2));
        eq("cold and empty is idle", MachineStatus.IDLE,
                ThermalLogic.status(false, 0L, 0));
    }

    private static void crafts() throws IOException {
        for (T1Recipes.Shaped recipe : T1Recipes.shaped()) {
            JsonObject json = read(recipe.name());
            eq(recipe.name() + " is ordinary shaped", "minecraft:crafting_shaped",
                    json.get("type").getAsString());
            eq(recipe.name() + " result", recipe.result(),
                    json.getAsJsonObject("result").get("item").getAsString());
            int count = json.getAsJsonObject("result").has("count")
                    ? json.getAsJsonObject("result").get("count").getAsInt() : 1;
            eq(recipe.name() + " result count", recipe.resultCount(), count);
            yes(recipe.name() + " pattern matches the catalogue",
                    json.getAsJsonArray("pattern").toString().contains(recipe.pattern().get(0)));
            JsonObject key = json.getAsJsonObject("key");
            for (Map.Entry<String, String> entry : recipe.key().entrySet()) {
                yes(recipe.name() + " key " + entry.getKey(),
                        ingredientEquals(key.getAsJsonObject(entry.getKey()), entry.getValue()));
            }
            no(recipe.name() + " names no iron item id", namesMaterialItem(json));
        }
        eq("T1 exposes twenty-seven physical crafts including the existing Flux Cell", 27,
                T1Recipes.shaped().size());
        T1Recipes.Shaped firstRelay = T1Recipes.shaped().stream()
                .filter(recipe -> recipe.name().equals("relay_matrix")).findFirst().orElseThrow();
        eq("the first Relay Matrix is still hand-crafted", FabricationLogic.RELAY_MATRIX, firstRelay.result());
        eq("the first Relay Matrix is a five-unit hand batch", 5, firstRelay.resultCount());
        yes("the first Relay Matrix keeps its complete manual bill",
                shapedInputs(firstRelay.pattern(), firstRelay.key()).equals(Map.of(
                        T1Recipes.GLASS, 2, T1Recipes.COPPER, 2, T1Recipes.REDSTONE, 2,
                        T1Recipes.CALIBRATED_DATA_CORE, 1)));
        for (String machine : List.of("thermal_generator", "pulverizer", "arc_furnace", "assembler",
                "press", "kiln", "terrestrial_extractor")) {
            yes(machine + " consumes one physical Relay Matrix",
                    T1Recipes.shaped().stream()
                            .filter(recipe -> recipe.name().equals(machine))
                            .anyMatch(recipe -> recipe.key().containsValue(T1Recipes.RELAY_MATRIX)
                                    && recipe.pattern().stream().flatMapToInt(String::chars)
                                    .filter(symbol -> symbol == 'M').count() == 1));
        }
        yes("the pylon is among them",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("flux_pylon_mk1")));
        yes("the assembler is the last crafting-table machine",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("assembler")));
        yes("the press is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("press")));
        yes("the conduit is a hand item",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("flux_conduit")));
        yes("the capacitor is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("capacitor_bank")));
        yes("the transformer is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("flux_transformer")));
        yes("the Factory Portal is hand-crafted after the first Matrix",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("factory_portal")
                        && recipe.key().containsValue(T1Recipes.RELAY_MATRIX)));
        yes("the kiln is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("kiln")));
        yes("the merger is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("merger")));
        yes("the tunnel is a pair",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("tunnel_belt")));
        yes("the overflow is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("overflow_gate")));
        yes("the sorter is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("sorter")));
        yes("the advanced core is hand-crafted",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("advanced_data_core")));
        yes("the atlas is a hand item",
                BootstrapRecipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("process_atlas")));
        no("MK2 has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("flux_pylon_mk2.json")));
        no("the mill has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("wire_mill.json")));
        no("the motor has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("motor.json")));
        no("refractory brick has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("refractory_brick.json")));
        no("metallurgical silicon has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("metallurgical_silicon.json")));
        no("electronic silicon has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("electronic_silicon.json")));
        for (String item : PROTOTYPE_ITEMS) {
            no(item + " has no crafting-table recipe",
                    Files.isRegularFile(RECIPES.resolve(item + ".json")));
        }
        String silica = Files.readString(Path.of(
                "common/src/main/resources/data/grindless/tags/items/silica.json"));
        yes("silica tag accepts sand", silica.contains("\"minecraft:sand\""));
        yes("silica tag accepts quartz", silica.contains("\"minecraft:quartz\""));
        no("silica tag does not name red sand", silica.contains("red_sand"));
        no("silica tag does not name a grindless item", silica.contains("grindless:"));
        no("silica tag does not name glass", silica.contains("glass"));
        no("the reactor has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("chemical_reactor.json")));
        no("the washer has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("chemical_washer.json")));
        no("the cell has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("electrolysis_cell.json")));
        no("the intake has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("atmospheric_intake.json")));
        no("the well has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("fluid_well.json")));
        no("the induction furnace has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("induction_furnace.json")));
        no("the caster has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("caster.json")));
        no("the ingot mould has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("ingot_mould.json")));
        no("the plate mould has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("plate_mould.json")));
        no("the flotation cell has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("flotation_cell.json")));
        no("the magnet has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("magnetic_separator.json")));
        no("the solar array has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("solar_array.json")));
        no("the boiler has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("boiler.json")));
        no("the condenser has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("condenser.json")));
        no("the pressure pipe has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("pressure_pipe.json")));
        no("the electric pump has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("electric_pump.json")));
        no("the industrial tank has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("industrial_tank.json")));
        no("the fluid manipulator has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("fluid_manipulator.json")));
        no("the flux belt has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("flux_belt.json")));
        no("the stack manipulator has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("stack_manipulator.json")));
        no("the filter manipulator has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("filter_manipulator.json")));
        no("the signal cable has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("signal_cable.json")));
        no("the logic controller has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("logic_controller.json")));
        no("the redstone interface has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("redstone_interface.json")));
        no("the flux drill has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("flux_drill.json")));
        no("the drill cell has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("drill_cell.json")));
        no("the blueprint tool has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("blueprint_tool.json")));
        no("the planner has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("deconstruction_planner.json")));
        no("the scanner has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("pattern_scanner.json")));
        no("the deconstructor has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("deconstructor.json")));
        no("matter has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("matter.json")));
        no("the exosuit has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("flux_exosuit_helmet.json")));
        no("the network tap has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("network_tap.json")));
        yes("vanadia is a hand reagent",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("vanadia_pellet")));
    }

    private static ProcessRecipe recipe(List<ProcessRecipe> recipes, String id) {
        return recipes.stream().filter(recipe -> recipe.id().equals(id)).findFirst().orElseThrow();
    }

    private static void pressure() {
        List<IngredientSpec> inputs = List.of(IngredientSpec.item("fixture:feed", 2));
        List<OutputSpec> outputs = List.of(OutputSpec.item("fixture:product", 3));
        List<IngredientSpec> catalysts = List.of(IngredientSpec.item("fixture:die", 1));
        for (ProcessRecipe legacy : List.of(
                new ProcessRecipe("fixture/legacy", MachineFamily.PRESS, inputs, outputs, 20, "AIR", 80, 4),
                new ProcessRecipe("fixture/legacy", MachineFamily.PRESS, inputs, outputs, 20, "AIR", 80, 4, catalysts),
                new ProcessRecipe("fixture/legacy", MachineFamily.PRESS, inputs, outputs, 20, "AIR", 80, 4,
                        catalysts, "fixture_only"),
                new ProcessRecipe("fixture/legacy", MachineFamily.PRESS, inputs, outputs, 20, "AIR", "STATIC",
                        80, 4, catalysts, "fixture_only"))) {
            no("every old constructor leaves pressure unnamed", legacy.namesPressure());
            yes("unnamed pressure has no implicit ambient optimum or band",
                    Double.isNaN(legacy.pressureMPa()) && Double.isNaN(legacy.pressureHalfWidthMPa()));
        }
        ProcessRecipe explicit = new ProcessRecipe("fixture/pressure", MachineFamily.PRESS, inputs, outputs,
                20, 1.0, 0.125, "AIR", "STATIC", 80, 4, catalysts, "fixture_only");
        yes("a physical pressure is named", explicit.namesPressure());
        eq("the optimum uses absolute MPa", 1.0, explicit.pressureMPa());
        eq("the explicit optimal half-width also uses MPa", 0.125, explicit.pressureHalfWidthMPa());
        yes("adding pressure changes no material quantity", explicit.inputs().equals(inputs)
                && explicit.outputs().equals(outputs) && explicit.catalysts().equals(catalysts));
        ProcessRecipe vacuum = pressure(0.0, 0.001);
        yes("zero absolute pressure is a requirement, never absence", vacuum.namesPressure());
        eq("zero has a real explicit band", 0.001, vacuum.pressureHalfWidthMPa());
        for (double optimum : new double[] {-0.001, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            rejects("invalid absolute pressure " + optimum, () -> pressure(optimum, 0.001));
        }
        for (double width : new double[] {0, -0.001, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY}) {
            rejects("invalid pressure half-width " + width, () -> pressure(1, width));
        }
        rejects("a pressure band cannot be orphaned", () -> pressure(Double.NaN, 0.001));
        rejects("overflowed pressure bounds are rejected", () -> pressure(Double.MAX_VALUE, Double.MAX_VALUE));
        no("an explicitly absent pressure pair is still unnamed", pressure(Double.NaN, Double.NaN).namesPressure());
    }

    private static ProcessRecipe pressure(double optimum, double halfWidth) {
        return new ProcessRecipe("fixture/pressure", MachineFamily.PULVERIZER, List.of(), List.of(),
                Double.NaN, optimum, halfWidth, null, null, 80, 4, List.of(), null);
    }

    private static void rejects(String what, Runnable operation) {
        try {
            operation.run();
            fail(what + ": expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            yes(what, true);
        }
    }

    private static boolean namesMaterialItem(ProcessRecipe recipe) {
        return recipe.inputs().stream().anyMatch(spec ->
                IngredientSpec.ITEM.equals(spec.kind()) && spec.id().contains("_ingot"))
                || recipe.outputs().stream().anyMatch(spec ->
                IngredientSpec.ITEM.equals(spec.kind()) && spec.id().contains("_ingot"));
    }

    private static boolean namesMaterialItem(JsonObject json) {
        String text = json.toString();
        return text.contains("\"item\":\"minecraft:iron_ingot\"")
                || text.contains("\"item\":\"grindless:iron_ingot\"");
    }

    private static JsonObject read(String name) throws IOException {
        Path file = RECIPES.resolve(name + ".json");
        yes(name + ".json exists", Files.isRegularFile(file));
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static boolean ingredientEquals(JsonObject json, String spec) {
        int split = spec.indexOf(':');
        String kind = spec.substring(0, split);
        String id = spec.substring(split + 1);
        return json.has(kind) && id.equals(json.get(kind).getAsString());
    }

    private static void eq(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, double expected, double actual) {
        if (Double.isNaN(expected) ? Double.isNaN(actual) : Math.abs(expected - actual) < 1e-9) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, MachineFamily expected, MachineFamily actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, MachineStatus expected, MachineStatus actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        if (!actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
