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
                new ProcessGraph.MaterialView("iron", true, true, true, true, true, true, true, true),
                new ProcessGraph.MaterialView("gold", true, false, false, true, true, true, true, true),
                new ProcessGraph.MaterialView("steel", false, false, false, false, true, true, true, true),
                new ProcessGraph.MaterialView("mythril", false, true, true, true, true, false, false, false));

        List<ProcessRecipe> recipes = ProcessGraph.generate(materials);
        eq("ore line plus roast plus press forms plus mill, coil, mill coil, mill, motor, contact, pickle, steel, refractory, silicon, zone refining, the ground array and the lunar link", 46, recipes.size());

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
        yes("steel has no blueprint", steel.blueprint() == null);
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
        yes("refractory brick has no blueprint", brick.blueprint() == null);
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
        yes("silicon has no blueprint", silicon.blueprint() == null);
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
        yes("zone refining has no blueprint", zone.blueprint() == null);
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

        no("oxygen blow is not this slice",
                recipes.stream().anyMatch(recipe -> recipe.id().contains("oxygen")));
        no("direct reduction is not this slice",
                recipes.stream().anyMatch(recipe -> recipe.id().contains("hydrogen")
                        || recipe.id().contains("direct")));
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
        ProcessRecipe casing = recipe(recipes, "assemble/array_casing");
        eq("the casing is the assembler", MachineFamily.ASSEMBLER, casing.family());
        eq("the casing takes four bricks", 4, casing.itemInputs().get(0).count());
        eq("the casing brick is the reagent", "item:grindless:refractory_brick",
                casing.itemInputs().get(0).qualified());
        eq("the casing takes one steel ingot", 1, casing.itemInputs().get(1).count());
        eq("the casing steel is the ingot tag", "tag:forge:ingots/steel",
                casing.itemInputs().get(1).qualified());
        eq("the casing makes one casing", 1, casing.itemOutputs().get(0).count());
        eq("the casing output is the block", "item:grindless:array_casing",
                casing.itemOutputs().get(0).qualified());
        eq("the casing is twenty seconds", 20 * 20, casing.durationTicks());
        eq("the casing draws F1", 32L, casing.fuPerTick());
        eq("the casing needs Industrial", "industrial", casing.blueprint());
        yes("the casing names no temperature", Double.isNaN(casing.temperatureC()));
        ProcessRecipe array = recipe(recipes, "assemble/ground_array");
        eq("the array is the assembler", MachineFamily.ASSEMBLER, array.family());
        eq("the array takes one machine casing", 1, array.itemInputs().get(0).count());
        eq("the array casing input is the reagent", "item:grindless:machine_casing",
                array.itemInputs().get(0).qualified());
        eq("the array takes four steel plates", 4, array.itemInputs().get(1).count());
        eq("the array plates are the tag", "tag:forge:plates/steel",
                array.itemInputs().get(1).qualified());
        eq("the array takes four bricks", 4, array.itemInputs().get(2).count());
        eq("the array makes one controller", 1, array.itemOutputs().get(0).count());
        eq("the array output is the block", "item:grindless:ground_array",
                array.itemOutputs().get(0).qualified());
        eq("the array is twenty seconds", 20 * 20, array.durationTicks());
        eq("the array draws F1", 32L, array.fuPerTick());
        eq("the array needs Industrial", "industrial", array.blueprint());
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
        no("refractory brick has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("refractory_brick.json")));
        no("metallurgical silicon has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("metallurgical_silicon.json")));
        no("electronic silicon has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("electronic_silicon.json")));
        no("the array has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("ground_array.json")));
        no("the casing has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("array_casing.json")));
        String silica = Files.readString(Path.of(
                "common/src/main/resources/data/grindless/tags/items/silica.json"));
        yes("silica tag accepts sand", silica.contains("\"minecraft:sand\""));
        yes("silica tag accepts quartz", silica.contains("\"minecraft:quartz\""));
        no("silica tag does not name red sand", silica.contains("red_sand"));
        no("silica tag does not name a grindless item", silica.contains("grindless:"));
        no("silica tag does not name glass", silica.contains("glass"));
        no("the reactor has no crafting-table recipe",
                Files.isRegularFile(RECIPES.resolve("chemical_reactor.json")));
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
