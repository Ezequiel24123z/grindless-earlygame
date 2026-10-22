package io.github.ezequiel24123z.grindless.recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Behaviour checks for the Process Atlas lookup (ADR-0066, ADR-0115). Not part of the mod.
 */
public final class VerifyAtlas {

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        List<ProcessGraph.MaterialView> materials = List.of(
                new ProcessGraph.MaterialView("iron", true, true, true, true, true, true, true, true, true),
                new ProcessGraph.MaterialView("gold", true, false, false, true, true, true, true, true, false),
                new ProcessGraph.MaterialView("steel", false, false, false, false, true, true, true, true, false),
                new ProcessGraph.MaterialView("mythril", false, true, true, true, true, false, false, false, true));
        List<ProcessRecipe> recipes = ProcessGraph.generate(materials);
        List<AtlasLogic.Entry> rows = AtlasLogic.entries(recipes);

        eq("atlas lists every generated recipe", 205, rows.size());
        eq("rows are sorted by family then id", "ARC_FURNACE", rows.get(0).family().name());
        eq("the first assembler row is the intake", "assemble/atmospheric_intake",
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).get(0).id());
        // The 33 assemble/bootstrap/* projections supplement the 47 other Assembler routes.
        eq("eighty reachable assembler crafts in this set", 80,
                AtlasLogic.family(recipes, MachineFamily.ASSEMBLER).size());

        List<AtlasLogic.Entry> allRows = AtlasLogic.allEntries(recipes);
        yes("old process and supplemental routes acquire no pressure or implicit ambient band",
                allRows.stream().allMatch(row -> Double.isNaN(row.pressureMPa())
                        && Double.isNaN(row.pressureHalfWidthMPa())));
        pressure();
        // The existing Flux Cell JSON now has the same catalogue/Atlas projection as other T1 crafts.
        eq("atlas includes every machine, Bootstrap craft and calibration route", 262, allRows.size());
        yes("the retired Guide has no craft or machine route", allRows.stream().noneMatch(row ->
                row.outputs().contains("item:grindless:field_guide")));
        for (BootstrapRecipes.Shaped craft : BootstrapRecipes.shaped()) {
            AtlasLogic.Entry row = route(allRows, "craft/" + craft.name());
            eq(craft.name() + " exposes its complete output batch", craft.resultCount(), row.outputs().size());
        }
        AtlasLogic.Entry core = route(allRows, "craft/data_core");
        yes("the Atlas exposes the ceramic core rather than the retired stone bypass",
                core.inputs().equals(List.of("item:grindless:ceramic_insulator", "item:minecraft:redstone")));
        eq("the grog sink is visible", 1, AtlasLogic.consuming(recipes, BootstrapProcesses.GROG).size());
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
        eq("relay displays five physical outputs", "5x relay_matrix", AtlasLogic.describe(relay.outputs()));
        AtlasLogic.Entry assembledRelay = route(allRows, "assemble/relay_matrix");
        eq("assembled relay names the Assembler", "ASSEMBLER", assembledRelay.station());
        eq("assembled relay keeps all seven physical input units", 7, assembledRelay.inputs().size());
        eq("assembled relay displays its batch inputs",
                "2x glass + 2x ingots/copper + 2x redstone + calibrated_data_core",
                AtlasLogic.describe(assembledRelay.inputs()));
        eq("assembled relay displays five physical outputs", "5x relay_matrix",
                AtlasLogic.describe(assembledRelay.outputs()));
        AtlasLogic.Entry industrialRelay = route(allRows, "assemble/relay_matrix_f2");
        eq("the Industrial Matrix names the Assembler", "ASSEMBLER", industrialRelay.station());
        eq("the Industrial Matrix retains all seven physical input units", 7, industrialRelay.inputs().size());
        eq("the Industrial Matrix requires an explicit F1 input", "item:grindless:relay_matrix [F1]",
                industrialRelay.inputs().get(0));
        eq("the Industrial Matrix shows its F2 output", "relay_matrix [F2]",
                AtlasLogic.describe(industrialRelay.outputs()));
        yes("the compact line retains both physical ratings",
                AtlasLogic.line(industrialRelay).contains("relay_matrix [F1]")
                        && AtlasLogic.line(industrialRelay).contains("relay_matrix [F2]"));
        AtlasLogic.Entry fluxCell = route(allRows, "craft/flux_cell");
        eq("the physical power-coupling craft is visible in the same Atlas", "CRAFTING_TABLE", fluxCell.station());
        eq("the Flux Cell row matches its existing shaped ingredient grouping",
                "8x ingots/copper + ingots/iron", AtlasLogic.describe(fluxCell.inputs()));
        yes("the Flux Cell row produces its actual existing item",
                fluxCell.outputs().equals(List.of("item:grindless:flux_cell")));
        for (String component : List.of("machine_casing", "flux_cell")) {
            AtlasLogic.Entry manual = route(allRows, "craft/" + component);
            AtlasLogic.Entry automatic = route(allRows, "assemble/" + component);
            eq(component + " keeps its manual bootstrap/compatibility recipe", "CRAFTING_TABLE", manual.station());
            eq(component + " exposes its automatic Assembler route", "ASSEMBLER", automatic.station());
            yes(component + " projects the exact crafting input multiset",
                    manual.inputs().stream().sorted().toList().equals(automatic.inputs().stream().sorted().toList()));
            yes(component + " projects the exact ordinary output batch", manual.outputs().equals(automatic.outputs()));
            yes(component + " has no invented die or catalyst", automatic.catalysts().isEmpty());
            eq(component + " exposes the actual shared Assembler duration",
                    FabricationLogic.ASSEMBLE_TICKS, automatic.durationTicks());
            eq(component + " exposes the actual shared Assembler draw",
                    FabricationLogic.FU_PER_TICK, automatic.fuPerTick());
            yes(component + " is discoverable through ordinary output lookup",
                    ids(AtlasLogic.producing(recipes, "grindless:" + component))
                            .equals(List.of("assemble/" + component)));
        }
        for (String die : List.of("plate_die", "rod_die", "gear_die", "coil_die")) {
            AtlasLogic.Entry manual = route(allRows, "craft/" + die);
            AtlasLogic.Entry automatic = route(allRows, "assemble/" + die);
            ProcessRecipe automaticProcess = recipes.stream()
                    .filter(recipe -> recipe.id().equals(automatic.id())).findFirst().orElseThrow();
            eq(die + " remains available from its crafting-table bootstrap", "CRAFTING_TABLE", manual.station());
            eq(die + " is reproduced by the existing Assembler", "ASSEMBLER", automatic.station());
            yes(die + " preserves the exact authored ingredient multiset",
                    manual.inputs().stream().sorted().toList().equals(automatic.inputs().stream().sorted().toList()));
            yes(die + " preserves the authored output batch", manual.outputs().equals(automatic.outputs()));
            yes(die + " fits the existing four Assembler input slots",
                    automaticProcess.itemInputs().size() <= 4);
            yes(die + " adds no consumed-recipe catalyst", automatic.catalysts().isEmpty());
            eq(die + " uses the existing Assembler duration",
                    FabricationLogic.ASSEMBLE_TICKS, automatic.durationTicks());
            eq(die + " uses the existing F1 draw", FabricationLogic.FU_PER_TICK, automatic.fuPerTick());
            yes(die + " is discoverable through ordinary output lookup",
                    ids(AtlasLogic.producing(recipes, "grindless:" + die)).equals(List.of("assemble/" + die)));
        }
        yes("the detail pane distinguishes the bootstrap and Industrial outputs",
                !AtlasLogic.describe(assembledRelay.outputs())
                        .equals(AtlasLogic.describe(industrialRelay.outputs())));
        eq("bare Matrix lookup still finds both generated production routes", 2,
                AtlasLogic.producing(recipes, "grindless:relay_matrix").size());
        eq("kind-qualified Matrix lookup still finds both generated production routes", 2,
                AtlasLogic.producing(recipes, "item:grindless:relay_matrix").size());
        yes("an explicit F2 lookup finds only the upgrade",
                ids(AtlasLogic.producing(recipes, "grindless:relay_matrix [F2]"))
                        .equals(List.of("assemble/relay_matrix_f2")));
        yes("bare Matrix consumption still finds the upgrade",
                ids(AtlasLogic.consuming(recipes, "item:grindless:relay_matrix"))
                        .contains("assemble/relay_matrix_f2"));
        yes("bare Matrix consumption discovers the mill's physical operand",
                ids(AtlasLogic.consuming(recipes, "grindless:relay_matrix")).contains("assemble/wire_mill"));
        yes("rated F1 consumption discovers the base mill",
                ids(AtlasLogic.consuming(recipes, "item:grindless:relay_matrix [F1]"))
                        .contains("assemble/wire_mill"));
        yes("the minimum-control mill is discoverable for a stronger Relay",
                ids(AtlasLogic.consuming(recipes, "grindless:relay_matrix [F2]"))
                        .contains("assemble/wire_mill"));
        no("stronger-controller lookup never relaxes the exact F2 predecessor",
                ids(AtlasLogic.consuming(recipes, "grindless:relay_matrix [F2]"))
                        .contains("assemble/relay_matrix_f2"));
        no("the minimum capacity label cannot admit F0",
                AtlasLogic.names("item:grindless:relay_matrix [F1+]", "grindless:relay_matrix [F0]"));
        no("an exact output never becomes a minimum-capacity output",
                AtlasLogic.names("item:grindless:relay_matrix [F2]", "grindless:relay_matrix [F1+]"));
        no("an F1 query never matches an F2 output",
                AtlasLogic.names("item:grindless:relay_matrix [F2]", "grindless:relay_matrix [F1]"));
        no("rating display does not turn another item into a Matrix",
                AtlasLogic.names("item:grindless:relay_matrix [F2]", "grindless:flux_cell"));
        no("blank lookup still matches nothing", AtlasLogic.names(industrialRelay.outputs().get(0), ""));
        AtlasLogic.Entry factoryPortal = route(allRows, "craft/factory_portal");
        eq("factory portal is a crafting-table route", "CRAFTING_TABLE", factoryPortal.station());
        yes("factory portal consumes its physical Matrix",
                factoryPortal.inputs().contains("item:grindless:relay_matrix"));

        List<AtlasLogic.Entry> ironIngot = AtlasLogic.producing(recipes, "forge:ingots/iron");
        // RenewableProcesses bootstrap/reduce_iron adds primitive reduction to the seven old routes.
        eq("eight routes make an iron ingot", 8, ironIngot.size());
        yes("B0 makes iron", ids(ironIngot).contains("b0_r1/iron"));
        yes("crushed R1 makes iron", ids(ironIngot).contains("b1_r1/iron"));
        yes("washed R1 makes iron", ids(ironIngot).contains("b2_r1/iron"));
        yes("R2 makes iron", ids(ironIngot).contains("r2/iron"));
        yes("the caster makes iron", ids(ironIngot).contains("cast/ingot/iron"));
        yes("concentrate makes iron", ids(ironIngot).contains("b3_r1/iron"));
        yes("tailings make a trace of iron", ids(ironIngot).contains("tailings/r1/iron"));
        yes("primitive firing makes iron", ids(ironIngot).contains("bootstrap/reduce_iron"));
        yes("qualified kind also matches",
                ids(AtlasLogic.producing(recipes, "tag:forge:ingots/iron")).equals(ids(ironIngot)));

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
        AtlasLogic.Entry assembledMill = route(allRows, "assemble/wire_mill");
        eq("the mill names the Assembler", "ASSEMBLER", assembledMill.station());
        eq("the mill exposes all eight consumed units", 8, assembledMill.inputs().size());
        eq("the base mill displays its complete live bill and minimum F1 capacity",
                "machine_casing + 2x copper_coil + 4x plates/iron + relay_matrix [F1+]",
                AtlasLogic.describe(assembledMill.inputs()));
        yes("the mill displays one ordinary block output",
                assembledMill.outputs().equals(List.of("item:grindless:wire_mill")));
        eq("the mill displays its unchanged twenty-second cycle", 400, assembledMill.durationTicks());
        eq("the mill displays its unchanged F1 draw", 32L, assembledMill.fuPerTick());
        yes("the mill displays no unconsumed catalyst", assembledMill.catalysts().isEmpty());
        yes("the mill's compact line distinguishes capacity from exact manufacturing",
                AtlasLogic.line(assembledMill).contains("relay_matrix [F1+]"));
        eq("the motor is an assembler output", 1,
                AtlasLogic.producing(recipes, "grindless:motor").size());
        eq("the component motor retains its Matrix-free displayed bill",
                "machine_casing + 2x copper_coil + rods/iron",
                AtlasLogic.describe(route(allRows, "assemble/motor").inputs()));
        for (String item : List.of("wire_mill", "motor")) {
            no(item + " has no handcraft route in either catalogue", allRows.stream().anyMatch(row ->
                    row.station().equals("CRAFTING_TABLE") && row.outputs().contains("item:grindless:" + item)));
        }
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
        // RenewableProcesses bootstrap/wet_aggregate adds a second settling water-recovery route.
        eq("recombination, condensation and both settling recoveries make water", 4,
                AtlasLogic.producing(recipes, "minecraft:water").size());
        yes("aggregate settling exposes its recovered water",
                ids(AtlasLogic.producing(recipes, "minecraft:water")).contains("bootstrap/wet_aggregate"));
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
        // RenewableProcesses bootstrap/flint_separation adds wet gravel separation, not milling.
        yes("sand exposes industrial milling, primitive milling, unfired recovery and flint separation",
                new java.util.HashSet<>(ids(AtlasLogic.producing(recipes, "minecraft:sand")))
                        .equals(java.util.Set.of("renew/sand", "bootstrap/sand", "bootstrap/unfired_recovery",
                                "bootstrap/flint_separation")));
        // RenewableProcesses bootstrap/glass supplies primitive firing alongside renew/glass.
        eq("primitive firing and industrial vitrification make glass", 2,
                AtlasLogic.producing(recipes, "minecraft:glass").size());
        yes("glass exposes exactly the primitive and industrial routes",
                new java.util.HashSet<>(ids(AtlasLogic.producing(recipes, "minecraft:glass")))
                        .equals(java.util.Set.of("bootstrap/glass", "renew/glass")));
        yes("the renewable glass chain begins at the pulverizer",
                ids(AtlasLogic.consuming(recipes, "minecraft:cobblestone")).contains("renew/sand"));
        yes("the renewable glass chain ends at the arc furnace",
                ids(AtlasLogic.consuming(recipes, "minecraft:sand")).contains("renew/glass"));
        // RenewableProcesses bootstrap/wet_aggregate recovers redstone; it is not another synthesis.
        eq("redstone has one synthesis and one aggregate recovery route", 2,
                AtlasLogic.producing(recipes, "minecraft:redstone").size());
        yes("redstone exposes exactly synthesis and aggregate recovery",
                new java.util.HashSet<>(ids(AtlasLogic.producing(recipes, "minecraft:redstone")))
                        .equals(java.util.Set.of("synthesise/redstone", "bootstrap/wet_aggregate")));
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
        for (String[] pressDie : List.of(
                new String[]{"plate_die", "press/plate/iron"},
                new String[]{"rod_die", "press/rod/iron"},
                new String[]{"gear_die", "press/gear/iron"},
                new String[]{"coil_die", "press/coil/copper"})) {
            yes(pressDie[0] + " remains a retained Press catalyst, not a consumed input",
                    ids(AtlasLogic.consuming(recipes, "grindless:" + pressDie[0])).contains(pressDie[1]));
        }

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

        jeiProjection();
        System.out.println(failures == 0
                ? "ALL ATLAS CHECKS PASSED"
                : failures + " ATLAS CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void jeiProjection() throws IOException {
        Path jei = Path.of("forge/src/main/java/io/github/ezequiel24123z/grindless/forge/jei");
        String plugin = Files.readString(jei.resolve("GrindlessJeiPlugin.java"));
        String category = Files.readString(jei.resolve("GrindlessProcessCategory.java"));
        yes("JEI projects the live recipe graph (source contract)",
                plugin.contains("ProcessLookup.recipes().stream()"));
        yes("JEI projects grouped inputs into the existing physical slots (source contract)",
                category.contains("new ArrayList<>(recipe.itemInputs())")
                        && category.contains("MachineMenuLayout.itemSlots(menuKind)")
                        && category.contains("addInput(slot, inputs.get(index))"));
        yes("JEI uses the shared registered Matrix alternatives (source contract, not rendering)",
                category.contains("ProcessLookup.matrixInputs(spec)"));
        yes("JEI explains minimum control separately from exact physical evidence (source contract)",
                category.contains("spec.isMinimumControl()")
                        && category.contains("\"recipe.grindless.minimum_control\""));
    }

    private static List<String> ids(List<AtlasLogic.Entry> rows) {
        return rows.stream().map(AtlasLogic.Entry::id).toList();
    }

    private static void pressure() {
        AtlasLogic.Entry legacy = new AtlasLogic.Entry("fixture/legacy", "PRESS", MachineFamily.PRESS,
                List.of(), List.of(), List.of(), Double.NaN, null, null, 80, 4);
        yes("the old Atlas row constructor leaves pressure unnamed", Double.isNaN(legacy.pressureMPa())
                && Double.isNaN(legacy.pressureHalfWidthMPa()));
        no("an unnamed pressure is not printed as ambient", AtlasLogic.line(legacy).contains("MPa"));
        for (double optimum : new double[] {0.0, 1.25, 0.000001}) {
            ProcessRecipe recipe = new ProcessRecipe("fixture/pressure", MachineFamily.PRESS,
                    List.of(IngredientSpec.item("fixture:feed", 2)),
                    List.of(OutputSpec.item("fixture:product", 3)), Double.NaN, optimum, 0.0000001,
                    null, null, 80, 4, List.of(), null);
            AtlasLogic.Entry row = AtlasLogic.entry(recipe);
            yes("Atlas retains the exact pressure optimum " + optimum, row.pressureMPa() == optimum);
            yes("Atlas retains the exact pressure band " + optimum, row.pressureHalfWidthMPa() == 0.0000001);
            yes("Atlas labels absolute MPa and the optimal, not hard-cutoff, band " + optimum,
                    AtlasLogic.line(row).contains(Double.toString(optimum) + " MPa absolute (optimal +/- "
                            + Double.toString(recipe.pressureHalfWidthMPa()) + " MPa)"));
            yes("output lookup preserves the same pressure projection " + optimum,
                    AtlasLogic.producing(List.of(recipe), "fixture:product").equals(List.of(row)));
            yes("input lookup preserves the same pressure projection " + optimum,
                    AtlasLogic.consuming(List.of(recipe), "fixture:feed").equals(List.of(row)));
        }
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
