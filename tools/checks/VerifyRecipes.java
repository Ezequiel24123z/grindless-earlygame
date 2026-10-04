package io.github.ezequiel24123z.grindless.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;

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
        eq("ore line plus roast plus wash plus press forms plus mill, coil, mill coil, mill, motor, contact, pickle, washer, gas line and well", 52, recipes.size());

        ProcessRecipe ironB0 = recipe(recipes, "b0_r1/iron");
        ProcessRecipe ironB1 = recipe(recipes, "b1/iron");
        ProcessRecipe ironWet = recipe(recipes, "b1_wet/iron");
        ProcessRecipe ironR1 = recipe(recipes, "b1_r1/iron");
        ProcessRecipe goldB0 = recipe(recipes, "b0_r1/gold");
        ProcessRecipe mythrilB0 = recipe(recipes, "b0_r1/mythril");

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
        eq("MK2 needs Industrial", "industrial", mk2.blueprint());

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
        yes("wire names no blueprint", ironWire.blueprint() == null);
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
        eq("the mill needs Industrial", "industrial", mill.blueprint());
        eq("the mill makes the block", "item:grindless:wire_mill", mill.itemOutputs().get(0).qualified());

        eq("motor is the assembler", MachineFamily.ASSEMBLER, motor.family());
        eq("motor takes a casing", "item:grindless:machine_casing", motor.itemInputs().get(0).qualified());
        eq("motor takes two coils", 2, motor.itemInputs().get(1).count());
        eq("motor takes a rod", "tag:forge:rods/iron", motor.itemInputs().get(2).qualified());
        eq("motor is ten seconds", 20 * 10, motor.durationTicks());
        eq("motor needs Industrial", "industrial", motor.blueprint());
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
        eq("the reactor needs Industrial", "industrial", reactor.blueprint());
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
        eq("washed is the grindless tag", "tag:grindless:washed_crushed/iron",
                ironWash.itemOutputs().get(0).qualified());
        eq("wash byproduct is the next metal", "tag:grindless:crushed_materials/mythril",
                ironWash.itemOutputs().get(1).qualified());
        eq("wash byproduct is one", 1, ironWash.itemOutputs().get(1).count());
        eq("mythril's byproduct wraps to iron", "tag:grindless:crushed_materials/iron",
                mythrilWash.itemOutputs().get(1).qualified());
        yes("the byproduct has an arc furnace sink",
                recipes.stream().anyMatch(candidate -> candidate.id().equals("b1_r1/mythril")
                        && candidate.itemInputs().get(0).id().equals("grindless:crushed_materials/mythril")));
        eq("wash is twenty seconds", 20 * 20, ironWash.durationTicks());
        eq("wash draws F1", 32L, ironWash.fuPerTick());
        yes("wash names no temperature", !ironWash.namesTemperature());
        yes("wash names no atmosphere", !ironWash.namesAtmosphere());
        yes("gold without washed does not wash",
                recipes.stream().noneMatch(candidate -> candidate.id().equals("b2/gold")));

        eq("washed reduction is the arc furnace", MachineFamily.ARC_FURNACE, ironWashedReduce.family());
        eq("washed reduction feeds washed", "tag:grindless:washed_crushed/iron",
                ironWashedReduce.itemInputs().get(0).qualified());
        eq("washed reduction makes one ingot", 1, ironWashedReduce.itemOutputs().get(0).count());

        eq("washed roast is the kiln", MachineFamily.KILN, ironWashedRoast.family());
        eq("washed roast feeds washed", "tag:grindless:washed_crushed/iron",
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
        eq("the washer needs Industrial", "industrial", washer.blueprint());
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
        eq("the cell needs Industrial", "industrial", cell.blueprint());
        eq("the cell makes the block", "item:grindless:electrolysis_cell",
                cell.itemOutputs().get(0).qualified());
        eq("the intake is the assembler", MachineFamily.ASSEMBLER, intake.family());
        eq("the intake makes the block", "item:grindless:atmospheric_intake",
                intake.itemOutputs().get(0).qualified());
        eq("the intake needs Industrial", "industrial", intake.blueprint());

        ProcessRecipe well = recipe(recipes, "assemble/fluid_well");
        eq("the well is the assembler", MachineFamily.ASSEMBLER, well.family());
        eq("the well takes a casing", "item:grindless:machine_casing",
                well.itemInputs().get(0).qualified());
        eq("the well takes two motors", 2, well.itemInputs().get(1).count());
        eq("the well takes four plates", 4, well.itemInputs().get(2).count());
        eq("the well is twenty seconds", 20 * 20, well.durationTicks());
        eq("the well needs Industrial", "industrial", well.blueprint());
        eq("the well makes the block", "item:grindless:fluid_well",
                well.itemOutputs().get(0).qualified());
        no("brine is not emitted",
                recipes.stream().anyMatch(candidate -> candidate.outputs().stream()
                        .anyMatch(spec -> spec.id().contains("brine"))));
        no("geothermal is not emitted",
                recipes.stream().anyMatch(candidate -> candidate.outputs().stream()
                        .anyMatch(spec -> spec.id().contains("geothermal"))));

        yes("steel with an ingot still presses",
                recipes.stream().anyMatch(recipe -> recipe.id().equals("press/plate/steel")));
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
        for (T1Recipes.Gated recipe : T1Recipes.gated()) {
            JsonObject json = read(recipe.name());
            eq(recipe.name() + " is gated shaped", "grindless:gated_shaped",
                    json.get("type").getAsString());
            eq(recipe.name() + " is voltaic-gated", recipe.blueprint(),
                    json.get("blueprint").getAsString());
            eq(recipe.name() + " result", recipe.result(),
                    json.getAsJsonObject("result").get("item").getAsString());
            yes(recipe.name() + " pattern matches the catalogue",
                    json.getAsJsonArray("pattern").toString().contains(recipe.pattern().get(0)));
            JsonObject key = json.getAsJsonObject("key");
            for (Map.Entry<String, String> entry : recipe.key().entrySet()) {
                yes(recipe.name() + " key " + entry.getKey(),
                        ingredientEquals(key.getAsJsonObject(entry.getKey()), entry.getValue()));
            }
            no(recipe.name() + " names no iron item id", namesMaterialItem(json));
        }
        eq("T1 ships thirty gated crafts", 30, T1Recipes.gated().size());
        yes("the pylon is among them",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("flux_pylon_mk1")));
        yes("the assembler is the last crafting-table machine",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("assembler")));
        yes("the press is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("press")));
        yes("the conduit is a hand item",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("flux_conduit")));
        yes("the capacitor is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("capacitor_bank")));
        yes("the transformer is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("flux_transformer")));
        yes("the kiln is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("kiln")));
        yes("the merger is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("merger")));
        yes("the tunnel is a pair",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("tunnel_belt")));
        yes("the overflow is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("overflow_gate")));
        yes("the sorter is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("sorter")));
        yes("the advanced core is hand-crafted",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("advanced_data_core")));
        yes("the atlas is a hand item",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("process_atlas")));
        no("MK2 has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("flux_pylon_mk2.json")));
        no("the mill has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("wire_mill.json")));
        no("the motor has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("motor.json")));
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
        yes("vanadia is a hand reagent",
                T1Recipes.gated().stream().anyMatch(recipe -> recipe.name().equals("vanadia_pellet")));
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
