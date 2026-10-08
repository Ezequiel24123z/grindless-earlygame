package io.github.ezequiel24123z.grindless.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.process.ConditionBand;
import io.github.ezequiel24123z.grindless.process.MachineEnvelopes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

    public static void main(String[] args) throws IOException {
        graph();
        logic();
        crafts();

        System.out.println(failures == 0
                ? "ALL RECIPE CHECKS PASSED"
                : failures + " RECIPE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void graph() {
        List<ProcessGraph.MaterialView> materials = List.of(
                new ProcessGraph.MaterialView("iron", true, true, true, true, true, true, true, true, true),
                new ProcessGraph.MaterialView("gold", true, false, false, true, true, true, true, true, false),
                new ProcessGraph.MaterialView("steel", false, false, false, false, true, true, true, true, false),
                new ProcessGraph.MaterialView("mythril", false, true, true, true, true, false, false, false, true));

        List<ProcessRecipe> recipes = ProcessGraph.generate(materials);
        eq("the reachable ore, chemistry, fabrication, logistics, tools, armour, renewable glass, steel and silicon graph",
                109, recipes.size());

        ProcessRecipe ironB0 = recipe(recipes, "b0_r1/iron");
        ProcessRecipe ironB1 = recipe(recipes, "b1/iron");
        ProcessRecipe ironWet = recipe(recipes, "b1_wet/iron");
        ProcessRecipe ironR1 = recipe(recipes, "b1_r1/iron");
        ProcessRecipe goldB0 = recipe(recipes, "b0_r1/gold");
        ProcessRecipe mythrilB0 = recipe(recipes, "b0_r1/mythril");
        ProcessRecipe renewableSand = recipe(recipes, "renew/sand");
        ProcessRecipe renewableGlass = recipe(recipes, "renew/glass");

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
        eq("coil takes copper", "tag:forge:ingots/copper", coil.itemInputs().get(0).qualified());
        eq("coil uses the coil die", "item:grindless:coil_die", coil.catalysts().get(0).qualified());
        eq("coil is a reagent", "item:grindless:copper_coil", coil.itemOutputs().get(0).qualified());

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
        eq("the mill takes a casing", "item:grindless:machine_casing", mill.itemInputs().get(0).qualified());
        eq("the mill takes two coils", 2, mill.itemInputs().get(1).count());
        eq("the mill coils are the reagent", "item:grindless:copper_coil", mill.itemInputs().get(1).qualified());
        eq("the mill takes four plates", 4, mill.itemInputs().get(2).count());
        eq("the mill is twenty seconds", 20 * 20, mill.durationTicks());
        eq("the mill carries the Industrial milestone", "industrial", mill.milestone());
        eq("the mill makes the block", "item:grindless:wire_mill", mill.itemOutputs().get(0).qualified());

        eq("motor is the assembler", MachineFamily.ASSEMBLER, motor.family());
        eq("motor takes a casing", "item:grindless:machine_casing", motor.itemInputs().get(0).qualified());
        eq("motor takes two coils", 2, motor.itemInputs().get(1).count());
        eq("motor takes a rod", "tag:forge:rods/iron", motor.itemInputs().get(2).qualified());
        eq("motor is ten seconds", 20 * 10, motor.durationTicks());
        eq("motor carries the Industrial milestone", "industrial", motor.milestone());
        eq("motor makes the reagent", "item:grindless:motor", motor.itemOutputs().get(0).qualified());

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
        eq("T1 ships thirty-one physical crafts", 31, T1Recipes.shaped().size());
        yes("the first Relay Matrix is a four-unit hand batch",
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("relay_matrix")
                        && recipe.resultCount() == 4));
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
                T1Recipes.shaped().stream().anyMatch(recipe -> recipe.name().equals("process_atlas")));
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
