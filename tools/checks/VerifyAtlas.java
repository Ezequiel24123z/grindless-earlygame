package io.github.ezequiel24123z.grindless.recipe;

import java.util.List;

/**
 * Behaviour checks for the Process Atlas lookup (ADR-0066). Not part of the mod.
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

        eq("atlas lists every generated recipe", 94, rows.size());
        eq("rows are sorted by family then id", "ARC_FURNACE", rows.get(0).family().name());
        eq("the first assembler row is the intake", "assemble/atmospheric_intake",
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).get(0).id());
        eq("thirty assembler crafts in this set", 30,
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).size());

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

        List<AtlasLogic.Entry> kiln = AtlasLogic.family(recipes, MachineFamily.KILN);
        eq("seven kiln routes in this set", 7, kiln.size());
        no("steel does not roast", ids(kiln).stream().anyMatch(id -> id.endsWith("/steel")));

        List<AtlasLogic.Entry> mill = AtlasLogic.family(recipes, MachineFamily.WIRE_MILL);
        eq("five mill routes in this set", 5, mill.size());
        yes("iron wire is milled", ids(mill).contains("mill/wire/iron"));
        yes("the mill coil is copper", ids(mill).contains("mill/coil/copper"));

        List<AtlasLogic.Entry> reactor = AtlasLogic.family(recipes, MachineFamily.CHEMICAL_REACTOR);
        eq("five reactor routes in this set", 5, reactor.size());
        yes("oxidation is contact", ids(reactor).contains("contact/so3"));
        yes("absorption is contact", ids(reactor).contains("contact/acid"));
        yes("pickle spends acid", ids(reactor).contains("pickle/plate/iron"));
        yes("recombination spends oxygen", ids(reactor).contains("recombine/water"));
        yes("surfactant is a reactor route", ids(reactor).contains("reagent/surfactant"));

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
