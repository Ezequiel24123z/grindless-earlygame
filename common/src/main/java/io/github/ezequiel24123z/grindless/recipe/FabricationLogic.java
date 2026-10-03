package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

/**
 * T1 fabrication numbers (ADR-0063). Independent of a world.
 *
 * <p>The Press is the first forming machine: one ingot and a die, four seconds, F1. The die is
 * a catalyst. The Assembler is the last crafting-table machine; from there T2 is manufactured.
 */
public final class FabricationLogic {

    public static final int PRESS_TICKS = 20 * 4;
    public static final int ASSEMBLE_TICKS = 20 * 20;
    public static final long FU_PER_TICK = FluxTier.F1.nominal();

    public static final String PLATE_DIE = "grindless:plate_die";
    public static final String ROD_DIE = "grindless:rod_die";
    public static final String GEAR_DIE = "grindless:gear_die";
    public static final String COIL_DIE = "grindless:coil_die";

    public static final String MACHINE_CASING = "grindless:machine_casing";
    public static final String COPPER_COIL = "grindless:copper_coil";
    public static final String PYLON_MK2 = "grindless:flux_pylon_mk2";

    private FabricationLogic() {
    }
}
