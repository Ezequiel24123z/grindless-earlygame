package io.github.ezequiel24123z.grindless.recipe;

import java.util.List;

/**
 * Behaviour checks for the Process Atlas lookup (ADR-0066). Not part of the mod.
 */
public final class VerifyAtlas {

    private static int failures = 0;

    public static void main(String[] args) {
        List<ProcessGraph.MaterialView> materials = List.of(
                new ProcessGraph.MaterialView("iron", true, true, true, true, true, true, true, true),
                new ProcessGraph.MaterialView("gold", true, false, false, true, true, true, true, true),
                new ProcessGraph.MaterialView("steel", false, false, false, false, true, true, true, true),
                new ProcessGraph.MaterialView("mythril", false, true, true, true, true, false, false, false));
        List<ProcessRecipe> recipes = ProcessGraph.generate(materials);
        List<AtlasLogic.Entry> rows = AtlasLogic.entries(recipes);

        eq("atlas lists every generated recipe", 51, rows.size());
        eq("rows are sorted by family then id", "ARC_FURNACE", rows.get(0).family().name());
        eq("the first assembler row is the casing", "assemble/array_casing",
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).get(0).id());
        eq("twelve assembler crafts in this set, including the supraluminal station", 12,
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).size());

        List<AtlasLogic.Entry> ironIngot = AtlasLogic.producing(recipes, "forge:ingots/iron");
        eq("three routes make an iron ingot", 3, ironIngot.size());
        yes("B0 makes iron", ids(ironIngot).contains("b0_r1/iron"));
        yes("crushed R1 makes iron", ids(ironIngot).contains("b1_r1/iron"));
        yes("R2 makes iron", ids(ironIngot).contains("r2/iron"));
        yes("qualified kind also matches",
                AtlasLogic.producing(recipes, "tag:forge:ingots/iron").size() == 3);

        List<AtlasLogic.Entry> oxide = AtlasLogic.producing(recipes, "grindless:oxides/iron");
        eq("two roast routes make iron oxide", 2, oxide.size());
        yes("raw roast makes oxide", ids(oxide).contains("roast/iron"));
        yes("crushed roast makes oxide", ids(oxide).contains("roast_crushed/iron"));

        List<AtlasLogic.Entry> so2 = AtlasLogic.producing(recipes, "grindless:sulfur_dioxide");
        eq("five roast routes vent SO2", 5, so2.size());
        eq("MK2 is the assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:flux_pylon_mk2").size());
        eq("the mill is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:wire_mill").size());
        eq("the motor is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:motor").size());
        eq("the reactor is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:chemical_reactor").size());
        eq("SO3 has one contact route", 1,
                AtlasLogic.producing(recipes, "grindless:sulfur_trioxide").size());
        eq("acid has one contact route", 1,
                AtlasLogic.producing(recipes, "grindless:sulfuric_acid").size());
        eq("pickle is a second iron plate route", 2,
                AtlasLogic.producing(recipes, "forge:plates/iron").size());
        eq("iron wire has one mill route", 1,
                AtlasLogic.producing(recipes, "grindless:wires/iron").size());
        eq("unknown product is empty", 0,
                AtlasLogic.producing(recipes, "forge:ingots/unobtainium").size());
        eq("steel has one arc route", 1,
                AtlasLogic.producing(recipes, "forge:ingots/steel").size());
        yes("that route is electric arc",
                ids(AtlasLogic.producing(recipes, "forge:ingots/steel")).contains("alloy/steel"));

        List<AtlasLogic.Entry> kiln = AtlasLogic.family(recipes, MachineFamily.KILN);
        eq("five kiln routes in this set", 5, kiln.size());
        no("steel does not roast", ids(kiln).stream().anyMatch(id -> id.endsWith("/steel")));

        List<AtlasLogic.Entry> mill = AtlasLogic.family(recipes, MachineFamily.WIRE_MILL);
        eq("five mill routes in this set", 5, mill.size());
        yes("iron wire is milled", ids(mill).contains("mill/wire/iron"));
        yes("the mill coil is copper", ids(mill).contains("mill/coil/copper"));

        List<AtlasLogic.Entry> reactor = AtlasLogic.family(recipes, MachineFamily.CHEMICAL_REACTOR);
        eq("three reactor routes in this set", 3, reactor.size());
        yes("oxidation is contact", ids(reactor).contains("contact/so3"));
        yes("absorption is contact", ids(reactor).contains("contact/acid"));
        yes("pickle spends acid", ids(reactor).contains("pickle/plate/iron"));

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
        yes("that route vents CO",
                ids(AtlasLogic.producing(recipes, "grindless:carbon_monoxide")).contains("silicon/metallurgical"));
        eq("refractory brick has one route", 1,
                AtlasLogic.producing(recipes, "grindless:refractory_brick").size());
        eq("the ground array has one route", 1,
                AtlasLogic.producing(recipes, "grindless:ground_array").size());
        eq("the array casing has one route", 1,
                AtlasLogic.producing(recipes, "grindless:array_casing").size());
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
