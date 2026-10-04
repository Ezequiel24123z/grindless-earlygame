package io.github.ezequiel24123z.grindless.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates the T1 processing and fabrication line from a material set (ADR-0005, ADR-0063,
 * ADR-0065, ADR-0074, ADR-0075).
 *
 * <p>Ore line: B0×R1, dry B1×R1, wet B1, roast, R2 reduce. Forming: Press recipes keyed by die.
 * Fabrication: Assembler recipes that manufacture Pylon MK2, the Wire Mill, the motor, the
 * Chemical Reactor and the Chemical Washer once Industrial is researched, with no
 * crafting-table JSON (ADR-0073, ADR-0074, ADR-0075, ADR-0076). Contact: SO₂ → SO₃ →
 * sulfuric acid, plus pickle. Wash: eight crushed and water become washed crushed plus the
 * next metal. Gases: water splits to hydrogen and oxygen; free air yields oxygen; hydrogen
 * and oxygen recombine to water (ADR-0077). No Minecraft imports: {@code VerifyRecipes}
 * dumps this graph without booting the game.
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
     * @param washed  {@code grindless:washed_crushed/<name>}
     */
    public record MaterialView(String name, boolean raw, boolean ore, boolean crushed, boolean oxide,
                               boolean ingot, boolean plate, boolean rod, boolean gear, boolean washed) {
    }

    private ProcessGraph() {
    }

    /**
     * Ore-line, roast, press, mill, contact, wash, gas and assembler recipes the given materials support.
     *
     * <p>A material without an ingot is skipped for reduction and forming. A material without
     * a raw or ore form is skipped for the ore line, not for the Press. Missing crushed drops
     * B1 and crushed roast, not B0. Missing oxide drops roast and R2, not R1.
     */
    public static List<ProcessRecipe> generate(List<MaterialView> materials) {
        List<String> washCycle = washCycle(materials);
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
                if (material.crushed() && material.washed()) {
                    recipes.add(wash(material.name(), byproduct(washCycle, material.name())));
                    recipes.add(reduce(material.name(), "b2_r1", washedTag(material.name())));
                }
            }
            if (feed != null && material.oxide()) {
                recipes.add(roast(material.name(), "roast", feed));
                if (material.crushed()) {
                    recipes.add(roast(material.name(), "roast_crushed", crushedTag(material.name())));
                }
                if (material.crushed() && material.washed() && material.ingot()) {
                    recipes.add(roast(material.name(), "roast_washed", washedTag(material.name())));
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
            if (material.ingot()) {
                recipes.add(wire(material.name()));
                recipes.add(melt(material.name()));
                recipes.add(cast(material.name(), "ingot", ingotTag(material.name()),
                        FabricationLogic.INGOT_MOULD));
                if (material.plate()) {
                    recipes.add(cast(material.name(), "plate", "forge:plates/" + material.name(),
                            FabricationLogic.PLATE_MOULD));
                }
            }
        }
        recipes.add(coilPress());
        recipes.add(coilMill());
        recipes.add(pylonMk2());
        recipes.add(wireMill());
        recipes.add(motor());
        recipes.add(contactOxidation());
        recipes.add(contactAbsorption());
        recipes.add(pickleIron());
        recipes.add(chemicalReactor());
        recipes.add(chemicalWasher());
        recipes.add(waterElectrolysis());
        recipes.add(recombineWater());
        recipes.add(airOxygen());
        recipes.add(electrolysisCell());
        recipes.add(atmosphericIntake());
        recipes.add(fluidWell());
        recipes.add(inductionFurnace());
        recipes.add(caster());
        recipes.add(ingotMould());
        recipes.add(plateMould());
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

    public static String washedTag(String material) {
        return "grindless:washed_crushed/" + material;
    }

    public static String oxideTag(String material) {
        return "grindless:oxides/" + material;
    }

    public static String ingotTag(String material) {
        return "forge:ingots/" + material;
    }

    public static String wireTag(String material) {
        return "grindless:wires/" + material;
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

    /** 1 ingot → 2 wire. Every material with an ingot; fine wire waits (ADR-0074). */
    private static ProcessRecipe wire(String material) {
        return new ProcessRecipe(
                "mill/wire/" + material,
                MachineFamily.WIRE_MILL,
                List.of(IngredientSpec.tag(ingotTag(material), 1)),
                List.of(OutputSpec.tag(wireTag(material), 2)),
                Double.NaN,
                null,
                FabricationLogic.WIRE_TICKS,
                FabricationLogic.FU_PER_TICK);
    }

    /** T2 coil: two copper wire, no die. The Press route stays for T1 (ADR-0063). */
    private static ProcessRecipe coilMill() {
        return new ProcessRecipe(
                "mill/coil/copper",
                MachineFamily.WIRE_MILL,
                List.of(IngredientSpec.tag(wireTag("copper"), 2)),
                List.of(OutputSpec.item(FabricationLogic.COPPER_COIL, 1)),
                Double.NaN,
                null,
                FabricationLogic.WIRE_TICKS,
                FabricationLogic.FU_PER_TICK);
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
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /**
     * The first T2 process machine (ADR-0074). Casing, two coils, four plates. Circuit
     * board waits on acid.
     */
    private static ProcessRecipe wireMill() {
        return new ProcessRecipe(
                "assemble/wire_mill",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.COPPER_COIL, 2),
                        IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(FabricationLogic.WIRE_MILL, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /**
     * Contact oxidation. Air is the oxidiser; bottled oxygen waits (ADR-0075). Temperature is
     * unnamed so the held 450 °C also covers absorption.
     */
    private static ProcessRecipe contactOxidation() {
        return new ProcessRecipe(
                "contact/so3",
                MachineFamily.CHEMICAL_REACTOR,
                List.of(IngredientSpec.fluid(ProcessLogic.SULFUR_DIOXIDE, ProcessLogic.SO2_MB)),
                List.of(OutputSpec.fluid(ProcessLogic.SULFUR_TRIOXIDE, ProcessLogic.SO3_MB)),
                Double.NaN,
                ProcessLogic.CONTACT_ATMOSPHERE,
                ProcessLogic.CONTACT_OXIDE_TICKS,
                ProcessLogic.FU_PER_TICK,
                List.of(IngredientSpec.item(FabricationLogic.VANADIA, 1)));
    }

    /**
     * Contact absorption. Water stays in a neighbouring tank and is taken at finish
     * (ADR-0075).
     */
    private static ProcessRecipe contactAbsorption() {
        return new ProcessRecipe(
                "contact/acid",
                MachineFamily.CHEMICAL_REACTOR,
                List.of(
                        IngredientSpec.fluid(ProcessLogic.SULFUR_TRIOXIDE, ProcessLogic.SO3_MB),
                        IngredientSpec.fluid(ProcessLogic.WATER, ProcessLogic.ABSORB_WATER_MB)),
                List.of(OutputSpec.fluid(ProcessLogic.SULFURIC_ACID, ProcessLogic.ACID_MB)),
                Double.NaN,
                null,
                ProcessLogic.CONTACT_ACID_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /** Named sulfuric spend. The Press plate die remains (ADR-0075). */
    private static ProcessRecipe pickleIron() {
        return new ProcessRecipe(
                "pickle/plate/iron",
                MachineFamily.CHEMICAL_REACTOR,
                List.of(
                        IngredientSpec.tag(ingotTag("iron"), 1),
                        IngredientSpec.fluid(ProcessLogic.SULFURIC_ACID, ProcessLogic.PICKLE_ACID_MB)),
                List.of(OutputSpec.tag("forge:plates/iron", 1)),
                Double.NaN,
                null,
                ProcessLogic.PICKLE_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /**
     * The second T2 process machine (ADR-0075). Casing, two motors, four plates. Circuit
     * board waits on etching.
     */
    private static ProcessRecipe chemicalReactor() {
        return new ProcessRecipe(
                "assemble/chemical_reactor",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.MOTOR, 2),
                        IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(FabricationLogic.CHEMICAL_REACTOR, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /**
     * B2. Eight crushed is four raw, so the 0.25 u byproduct is one whole item (ADR-0076).
     * {@code byproduct} is null when the pack has no second washable metal.
     */
    private static ProcessRecipe wash(String material, String byproduct) {
        List<OutputSpec> outputs = new ArrayList<>();
        outputs.add(OutputSpec.tag(washedTag(material), ProcessLogic.WASH_CRUSHED));
        if (byproduct != null) {
            outputs.add(OutputSpec.tag(crushedTag(byproduct), 1));
        }
        return new ProcessRecipe(
                "b2/" + material,
                MachineFamily.CHEMICAL_WASHER,
                List.of(
                        IngredientSpec.tag(crushedTag(material), ProcessLogic.WASH_CRUSHED),
                        IngredientSpec.fluid(ProcessLogic.WATER, ProcessLogic.WASH_WATER_MB)),
                outputs,
                Double.NaN,
                null,
                ProcessLogic.WASH_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /** Materials whose crushed form can be washed and whose ingot line can sink a byproduct. */
    private static List<String> washCycle(List<MaterialView> materials) {
        List<String> cycle = new ArrayList<>();
        for (MaterialView material : materials) {
            if (material.crushed() && material.ingot() && material.washed() && feedTag(material) != null) {
                cycle.add(material.name());
            }
        }
        return cycle;
    }

    /** The next washable metal, or {@code null} when there is no secondary. */
    private static String byproduct(List<String> cycle, String name) {
        if (cycle.size() < 2) {
            return null;
        }
        int index = cycle.indexOf(name);
        if (index < 0) {
            return null;
        }
        return cycle.get((index + 1) % cycle.size());
    }

    /**
     * The wet line's machine (ADR-0076). Casing, two motors, four plates. Circuit board waits
     * on etching.
     */
    private static ProcessRecipe chemicalWasher() {
        return new ProcessRecipe(
                "assemble/chemical_washer",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.MOTOR, 2),
                        IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(FabricationLogic.CHEMICAL_WASHER, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /**
     * 2 B water becomes 2 B hydrogen in the buffer and 1 B vented oxygen (ADR-0077).
     */
    private static ProcessRecipe waterElectrolysis() {
        return new ProcessRecipe(
                "electrolysis/water",
                MachineFamily.ELECTROLYSIS_CELL,
                List.of(IngredientSpec.fluid(ProcessLogic.WATER, ProcessLogic.ELECTROLYSIS_WATER_MB)),
                List.of(
                        OutputSpec.fluid(ProcessLogic.HYDROGEN, ProcessLogic.ELECTROLYSIS_HYDROGEN_MB),
                        OutputSpec.ventedFluid(ProcessLogic.OXYGEN, ProcessLogic.ELECTROLYSIS_OXYGEN_MB)),
                Double.NaN,
                null,
                ProcessLogic.ELECTROLYSIS_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /** Named oxygen sink. Hydrogen is primary; oxygen comes from a neighbour. */
    private static ProcessRecipe recombineWater() {
        return new ProcessRecipe(
                "recombine/water",
                MachineFamily.CHEMICAL_REACTOR,
                List.of(
                        IngredientSpec.fluid(ProcessLogic.HYDROGEN, ProcessLogic.RECOMBINE_HYDROGEN_MB),
                        IngredientSpec.fluid(ProcessLogic.OXYGEN, ProcessLogic.RECOMBINE_OXYGEN_MB)),
                List.of(OutputSpec.fluid(ProcessLogic.WATER, ProcessLogic.RECOMBINE_WATER_MB)),
                Double.NaN,
                null,
                ProcessLogic.RECOMBINE_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /** Free air, stored oxygen. No nitrogen and no argon until each has a sink. */
    private static ProcessRecipe airOxygen() {
        return new ProcessRecipe(
                "air/oxygen",
                MachineFamily.ATMOSPHERIC_INTAKE,
                List.of(),
                List.of(OutputSpec.fluid(ProcessLogic.OXYGEN, ProcessLogic.AIR_OXYGEN_MB)),
                Double.NaN,
                null,
                ProcessLogic.AIR_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /** The gas line's cell (ADR-0077). Casing, two motors, four plates. */
    private static ProcessRecipe electrolysisCell() {
        return new ProcessRecipe(
                "assemble/electrolysis_cell",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.MOTOR, 2),
                        IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(FabricationLogic.ELECTROLYSIS_CELL, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /** The gas line's intake (ADR-0077). Same craft as the cell. */
    private static ProcessRecipe atmosphericIntake() {
        return new ProcessRecipe(
                "assemble/atmospheric_intake",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.MOTOR, 2),
                        IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(FabricationLogic.ATMOSPHERIC_INTAKE, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /** 1 ingot becomes 144 mB of melt. No slag (ADR-0079). */
    private static ProcessRecipe melt(String material) {
        return new ProcessRecipe(
                "melt/" + material,
                MachineFamily.INDUCTION_FURNACE,
                List.of(IngredientSpec.tag(ingotTag(material), 1)),
                List.of(OutputSpec.fluid(ProcessLogic.moltenId(material), ProcessLogic.MOLTEN_MB)),
                ProcessLogic.MELT_TEMPERATURE,
                ProcessLogic.MELT_ATMOSPHERE,
                ProcessLogic.MELT_TICKS,
                ProcessLogic.FU_PER_TICK);
    }

    /** 144 mB of melt and a mould become one solid unit. The mould is not consumed. */
    private static ProcessRecipe cast(String material, String form, String tag, String mould) {
        return new ProcessRecipe(
                "cast/" + form + "/" + material,
                MachineFamily.CASTER,
                List.of(IngredientSpec.fluid(ProcessLogic.moltenId(material), ProcessLogic.MOLTEN_MB)),
                List.of(OutputSpec.tag(tag, 1)),
                Double.NaN,
                null,
                ProcessLogic.CAST_TICKS,
                ProcessLogic.FU_PER_TICK,
                List.of(IngredientSpec.item(mould, 1)));
    }

    private static ProcessRecipe inductionFurnace() {
        return machineCraft("assemble/induction_furnace", FabricationLogic.INDUCTION_FURNACE);
    }

    private static ProcessRecipe caster() {
        return machineCraft("assemble/caster", FabricationLogic.CASTER);
    }

    private static ProcessRecipe machineCraft(String id, String result) {
        return new ProcessRecipe(
                id,
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.MOTOR, 2),
                        IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(result, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    private static ProcessRecipe ingotMould() {
        return mould("assemble/ingot_mould", FabricationLogic.INGOT_MOULD);
    }

    private static ProcessRecipe plateMould() {
        return mould("assemble/plate_mould", FabricationLogic.PLATE_MOULD);
    }

    private static ProcessRecipe mould(String id, String result) {
        return new ProcessRecipe(
                id,
                MachineFamily.ASSEMBLER,
                List.of(IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(result, 1)),
                Double.NaN,
                null,
                FabricationLogic.PRESS_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /** Powered chunk water (ADR-0078). Same craft as the cell. */
    private static ProcessRecipe fluidWell() {
        return new ProcessRecipe(
                "assemble/fluid_well",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.MOTOR, 2),
                        IngredientSpec.tag("forge:plates/iron", 4)),
                List.of(OutputSpec.item(FabricationLogic.FLUID_WELL, 1)),
                Double.NaN,
                null,
                FabricationLogic.ASSEMBLE_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
    }

    /** Fabricated T2 component. No fluid gate (ADR-0074). */
    private static ProcessRecipe motor() {
        return new ProcessRecipe(
                "assemble/motor",
                MachineFamily.ASSEMBLER,
                List.of(
                        IngredientSpec.item(FabricationLogic.MACHINE_CASING, 1),
                        IngredientSpec.item(FabricationLogic.COPPER_COIL, 2),
                        IngredientSpec.tag("forge:rods/iron", 1)),
                List.of(OutputSpec.item(FabricationLogic.MOTOR, 1)),
                Double.NaN,
                null,
                FabricationLogic.MOTOR_TICKS,
                FabricationLogic.FU_PER_TICK,
                List.of(),
                "industrial");
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
