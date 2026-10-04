package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

/**
 * Fabrication numbers (ADR-0063, ADR-0074). Independent of a world.
 *
 * <p>The Press is the first forming machine: one ingot and a die, four seconds, F1. The die is
 * a catalyst. The Assembler is the last crafting-table machine; from there T2 is manufactured.
 * The Wire Mill draws wire in eight seconds; the motor is ten. The Chemical Reactor
 * oxidises in six and absorbs in four (ADR-0075).
 */
public final class FabricationLogic {

    public static final int PRESS_TICKS = 20 * 4;
    public static final int WIRE_TICKS = 20 * 8;
    public static final int MOTOR_TICKS = 20 * 10;
    public static final int ASSEMBLE_TICKS = 20 * 20;
    public static final long FU_PER_TICK = FluxTier.F1.nominal();

    public static final String PLATE_DIE = "grindless:plate_die";
    public static final String ROD_DIE = "grindless:rod_die";
    public static final String GEAR_DIE = "grindless:gear_die";
    public static final String COIL_DIE = "grindless:coil_die";

    public static final String MACHINE_CASING = "grindless:machine_casing";
    public static final String COPPER_COIL = "grindless:copper_coil";
    public static final String PYLON_MK2 = "grindless:flux_pylon_mk2";
    public static final String WIRE_MILL = "grindless:wire_mill";
    public static final String MOTOR = "grindless:motor";
    public static final String CHEMICAL_REACTOR = "grindless:chemical_reactor";
    public static final String CHEMICAL_WASHER = "grindless:chemical_washer";
    public static final String ELECTROLYSIS_CELL = "grindless:electrolysis_cell";
    public static final String ATMOSPHERIC_INTAKE = "grindless:atmospheric_intake";
    public static final String FLUID_WELL = "grindless:fluid_well";
    public static final String INDUCTION_FURNACE = "grindless:induction_furnace";
    public static final String CASTER = "grindless:caster";
    public static final String INGOT_MOULD = "grindless:ingot_mould";
    public static final String PLATE_MOULD = "grindless:plate_mould";
    public static final String FLOTATION_CELL = "grindless:flotation_cell";
    public static final String MAGNETIC_SEPARATOR = "grindless:magnetic_separator";
    public static final String SOLAR_ARRAY = "grindless:solar_array";
    public static final String BOILER = "grindless:boiler";
    public static final String CONDENSER = "grindless:condenser";
    public static final String VANADIA = "grindless:vanadia_pellet";

    private FabricationLogic() {
    }
}
