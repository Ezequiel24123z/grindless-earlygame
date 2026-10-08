package io.github.ezequiel24123z.grindless.recipe;

import java.util.List;

/**
 * Behaviour checks for the Process Atlas lookup (ADR-0066, ADR-0115). Not part of the mod.
 */
public final class VerifyAtlas {

    private static int failures = 0;

    public static void main(String[] args) {
        List<ProcessGraph.MaterialView> materials = List.of(
                new ProcessGraph.MaterialView("iron", true, true, true, true, true, true, true, true, true),
                new ProcessGraph.MaterialView("gold", true, false, false, true, true, true, true, true, false),
                new ProcessGraph.MaterialView("steel", false, false, false, false, true, true, true, true, false),
                new ProcessGraph.MaterialView("mythril", false, true, true, true, true, false, false, false, true));
        List<ProcessRecipe> recipes = ProcessGraph.generate(materials);
        List<AtlasLogic.Entry> rows = AtlasLogic.entries(recipes);

        eq("atlas lists every generated recipe", 110, rows.size());
        eq("rows are sorted by family then id", "ARC_FURNACE", rows.get(0).family().name());
        eq("the first assembler row is the intake", "assemble/atmospheric_intake",
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).get(0).id());
        eq("thirty-nine reachable assembler crafts in this set", 39,
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).size());

        List<AtlasLogic.Entry> allRows = AtlasLogic.allEntries(recipes);
        eq("atlas includes every machine, hand craft and calibration route", 142, allRows.size());
        AtlasLogic.Entry calibration = route(allRows, "calibrate/data_core");
        eq("calibration names its physical station", "RESEARCH_TERMINAL", calibration.station());
        yes("calibration has no generated machine family", calibration.family() == null);
        eq("calibration consumes the raw core", "item:grindless:data_core", calibration.inputs().get(0));
        eq("calibration produces the physical core", "item:grindless:calibrated_data_core",
                calibration.outputs().get(0));
        eq("calibration keeps its F0 duration", 600, calibration.durationTicks());
        eq("calibration keeps its F0 draw", 8L, calibration.fuPerTick());
        AtlasLogic.Entry relay = route(allRows, "craft/relay_matrix");
        eq("relay names the crafting table", "CRAFTING_TABLE", relay.station());
        eq("relay records every shaped input", 7, relay.inputs().size());
        eq("relay displays its batch inputs", "2x glass + 2x ingots/copper + 2x redstone + calibrated_data_core",
                AtlasLogic.describe(relay.inputs()));
        eq("relay displays four physical outputs", "4x relay_matrix", AtlasLogic.describe(relay.outputs()));

        List<AtlasLogic.Entry> ironIngot = AtlasLogic.producing(recipes, "forge:ingots/iron");
        eq("seven routes make an iron ingot", 7, ironIngot.size());
        yes("B0 makes iron", ids(ironIngot).contains("b0_r1/iron"));
        yes("crushed R1 makes iron", ids(ironIngot).contains("b1_r1/iron"));
        yes("washed R1 makes iron", ids(ironIngot).contains("b2_r1/iron"));
        yes("R2 makes iron", ids(ironIngot).contains("r2/iron"));
        yes("the caster makes iron", ids(ironIngot).contains("cast/ingot/iron"));
        yes("concentrate makes iron", ids(ironIngot).contains("b3_r1/iron"));
        yes("tailings make a trace of iron", ids(ironIngot).contains("tailings/r1/iron"));
        yes("qualified kind also matches",
                AtlasLogic.producing(recipes, "tag:forge:ingots/iron").size() == 7);

        List<AtlasLogic.Entry> oxide = AtlasLogic.producing(recipes, "grindless:oxides/iron");
        eq("three roast routes make iron oxide", 3, oxide.size());
        yes("raw roast makes oxide", ids(oxide).contains("roast/iron"));
        yes("crushed roast makes oxide", ids(oxide).contains("roast_crushed/iron"));
        yes("washed roast makes oxide", ids(oxide).contains("roast_washed/iron"));

        List<AtlasLogic.Entry> so2 = AtlasLogic.producing(recipes, "grindless:sulfur_dioxide");
        eq("seven roast routes vent SO2", 7, so2.size());
        eq("MK2 is the assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:flux_pylon_mk2").size());
        eq("the mill is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:wire_mill").size());
        eq("the motor is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:motor").size());
        eq("the reactor is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:chemical_reactor").size());
        eq("the washer is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:chemical_washer").size());
        eq("the cell is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:electrolysis_cell").size());
        eq("the intake is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:atmospheric_intake").size());
        eq("the well is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:fluid_well").size());
        eq("the induction furnace is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:induction_furnace").size());
        eq("the caster is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:caster").size());
        eq("the ingot mould is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:ingot_mould").size());
        eq("the plate mould is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:plate_mould").size());
        eq("the flotation cell is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:flotation_cell").size());
        eq("the magnet is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:magnetic_separator").size());
        eq("the solar array is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:solar_array").size());
        eq("the boiler is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:boiler").size());
        eq("the condenser is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:condenser").size());
        eq("the pressure pipe is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:pressure_pipe").size());
        eq("the electric pump is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:electric_pump").size());
        eq("the industrial tank is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:industrial_tank").size());
        eq("the fluid manipulator is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:fluid_manipulator").size());
        eq("the flux belt is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:flux_belt").size());
        eq("the stack manipulator is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:stack_manipulator").size());
        eq("the filter manipulator is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:filter_manipulator").size());
        eq("the signal cable is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:signal_cable").size());
        eq("the logic controller is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:logic_controller").size());
        eq("the redstone interface is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:redstone_interface").size());
        eq("the flux drill is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:flux_drill").size());
        eq("the drill cell is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:drill_cell").size());
        eq("the blueprint tool is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:blueprint_tool").size());
        eq("the planner is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:deconstruction_planner").size());
        eq("the scanner is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:pattern_scanner").size());
        eq("the deconstructor is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:deconstructor").size());
        eq("matter is not produced", 0,
                AtlasLogic.producing(recipes, "grindless:matter").size());
        eq("the exosuit chest is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:flux_exosuit_chestplate").size());
        eq("the network tap is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:network_tap").size());
        eq("the exoskeleton is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:exoskeleton_legs").size());
        eq("steam has one boiler route", 1,
                AtlasLogic.producing(recipes, "grindless:steam").size());
        eq("molten iron has one melt route", 1,
                AtlasLogic.producing(recipes, "grindless:molten/iron").size());
        eq("hydrogen has one electrolysis route", 1,
                AtlasLogic.producing(recipes, "grindless:hydrogen").size());
        eq("oxygen has the cell and the intake", 2,
                AtlasLogic.producing(recipes, "grindless:oxygen").size());
        eq("recombination and the condenser make water", 2,
                AtlasLogic.producing(recipes, "minecraft:water").size());
        eq("SO3 has one contact route", 1,
                AtlasLogic.producing(recipes, "grindless:sulfur_trioxide").size());
        eq("acid has one contact route", 1,
                AtlasLogic.producing(recipes, "grindless:sulfuric_acid").size());
        eq("cast is a third iron plate route", 3,
                AtlasLogic.producing(recipes, "forge:plates/iron").size());
        eq("iron wire has one mill route", 1,
                AtlasLogic.producing(recipes, "grindless:wires/iron").size());
        eq("unknown product is empty", 0,
                AtlasLogic.producing(recipes, "forge:ingots/unobtainium").size());
        // Steel has no vein, so the arc route is the only way to make it from scratch. The caster
        // is a second route only because it recasts a melt that the arc already produced.
        eq("steel has the arc route and the caster", 2,
                AtlasLogic.producing(recipes, "forge:ingots/steel").size());
        yes("the arc route is electric arc",
                ids(AtlasLogic.producing(recipes, "forge:ingots/steel")).contains("alloy/steel"));
        yes("the other route is the caster",
                ids(AtlasLogic.producing(recipes, "forge:ingots/steel")).contains("cast/ingot/steel"));

        List<AtlasLogic.Entry> kiln = AtlasLogic.family(recipes, MachineFamily.KILN);
        eq("seven kiln routes in this set", 7, kiln.size());
        no("steel does not roast", ids(kiln).stream().anyMatch(id -> id.endsWith("/steel")));

        List<AtlasLogic.Entry> mill = AtlasLogic.family(recipes, MachineFamily.WIRE_MILL);
        eq("five mill routes in this set", 5, mill.size());
        yes("iron wire is milled", ids(mill).contains("mill/wire/iron"));
        yes("the mill coil is copper", ids(mill).contains("mill/coil/copper"));

        List<AtlasLogic.Entry> reactor = AtlasLogic.family(recipes, MachineFamily.CHEMICAL_REACTOR);
        eq("six reactor routes in this set", 6, reactor.size());
        yes("oxidation is contact", ids(reactor).contains("contact/so3"));
        yes("absorption is contact", ids(reactor).contains("contact/acid"));
        yes("pickle spends acid", ids(reactor).contains("pickle/plate/iron"));
        yes("recombination spends oxygen", ids(reactor).contains("recombine/water"));
        yes("surfactant is a reactor route", ids(reactor).contains("reagent/surfactant"));
        yes("renewable redstone is a reactor route", ids(reactor).contains("synthesise/redstone"));

        List<AtlasLogic.Entry> flotation = AtlasLogic.family(recipes, MachineFamily.FLOTATION);
        eq("two flotation routes in this set", 2, flotation.size());
        yes("iron floats", ids(flotation).contains("b3/iron"));
        yes("mythril floats", ids(flotation).contains("b3/mythril"));

        List<AtlasLogic.Entry> cell = AtlasLogic.family(recipes, MachineFamily.ELECTROLYSIS_CELL);
        eq("one electrolysis route", 1, cell.size());
        yes("water splits", ids(cell).contains("electrolysis/water"));
        List<AtlasLogic.Entry> intake = AtlasLogic.family(recipes, MachineFamily.ATMOSPHERIC_INTAKE);
        eq("one air route", 1, intake.size());
        yes("air yields oxygen", ids(intake).contains("air/oxygen"));

        List<AtlasLogic.Entry> washer = AtlasLogic.family(recipes, MachineFamily.CHEMICAL_WASHER);
        eq("two wash routes in this set", 2, washer.size());
        yes("iron washes", ids(washer).contains("b2/iron"));
        yes("mythril washes", ids(washer).contains("b2/mythril"));

        List<AtlasLogic.Entry> carbon = AtlasLogic.consuming(recipes, "grindless:carbon");
        yes("R2 consumes carbon", ids(carbon).contains("r2/iron"));
        yes("steel consumes carbon", ids(carbon).contains("alloy/steel"));
        yes("silicon consumes carbon", ids(carbon).contains("silicon/metallurgical"));
        eq("metallurgical silicon has one route", 1,
                AtlasLogic.producing(recipes, "grindless:metallurgical_silicon").size());
        eq("electronic silicon has one route", 1,
                AtlasLogic.producing(recipes, "grindless:electronic_silicon").size());
        yes("zone refining consumes metallurgical silicon",
                ids(AtlasLogic.consuming(recipes, "grindless:metallurgical_silicon"))
                        .contains("silicon/zone_refining"));
        yes("silica reduces to that silicon",
                ids(AtlasLogic.consuming(recipes, "grindless:silica")).contains("silicon/metallurgical"));
        eq("renewable cobblestone milling makes sand", 1,
                AtlasLogic.producing(recipes, "minecraft:sand").size());
        eq("renewable sand vitrification makes glass", 1,
                AtlasLogic.producing(recipes, "minecraft:glass").size());
        yes("the renewable glass chain begins at the pulverizer",
                ids(AtlasLogic.consuming(recipes, "minecraft:cobblestone")).contains("renew/sand"));
        yes("the renewable glass chain ends at the arc furnace",
                ids(AtlasLogic.consuming(recipes, "minecraft:sand")).contains("renew/glass"));
        eq("renewable redstone has one synthesis route", 1,
                AtlasLogic.producing(recipes, "minecraft:redstone").size());
        yes("renewable redstone consumes metallurgical silicon",
                ids(AtlasLogic.consuming(recipes, "grindless:metallurgical_silicon"))
                        .contains("synthesise/redstone"));
        yes("renewable redstone consumes water",
                ids(AtlasLogic.consuming(recipes, "minecraft:water")).contains("synthesise/redstone"));
        yes("that route vents CO",
                ids(AtlasLogic.producing(recipes, "grindless:carbon_monoxide")).contains("silicon/metallurgical"));
        eq("refractory brick has one route", 1,
                AtlasLogic.producing(recipes, "grindless:refractory_brick").size());
        for (String item : List.of(
                "array_casing", "ground_array", "lunar_link", "starward_link",
                "launch_pad", "survey_rocket", "station_berth", "supraluminal_station")) {
            eq(item + " is absent from the survival atlas", 0,
                    AtlasLogic.producing(recipes, "grindless:" + item).size());
        }
        yes("slag fires into that brick",
                ids(AtlasLogic.consuming(recipes, "grindless:slag")).contains("ceramic/refractory_brick"));
        yes("the plate die is a catalyst, not an input",
                ids(AtlasLogic.consuming(recipes, "grindless:plate_die")).contains("press/plate/iron"));

        AtlasLogic.Entry roast = kiln.stream()
                .filter(row -> row.id().equals("roast/iron"))
                .findFirst()
                .orElseThrow();
        String line = AtlasLogic.line(roast);
        yes("the roast line names the family", line.startsWith("KILN"));
        yes("the roast line names the id", line.contains("roast/iron"));
        yes("the roast line names SO2", line.contains("sulfur_dioxide"));
        yes("the roast line names eight seconds", line.contains("8s"));
        yes("the roast line names 700 C", line.contains("700 C"));
        yes("the roast line names oxidising", line.contains("OXIDISING"));
        no("the stub line does not quote a target rate", line.contains("/min") || line.contains("machines"));

        System.out.println(failures == 0
                ? "ALL ATLAS CHECKS PASSED"
                : failures + " ATLAS CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static List<String> ids(List<AtlasLogic.Entry> rows) {
        return rows.stream().map(AtlasLogic.Entry::id).toList();
    }

    private static AtlasLogic.Entry route(List<AtlasLogic.Entry> rows, String id) {
        return rows.stream().filter(row -> row.id().equals(id)).findFirst().orElseThrow();
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
