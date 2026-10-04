package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * Daylight power, independent of a world (ADR-0081).
 *
 * <p>One panel matches the Thermal Generator. F2 waits for the turbine.
 */
public final class SolarLogic {

    public static final long FU_PER_TICK = ThermalLogic.FU_PER_TICK;

    private SolarLogic() {
    }

    public static long generate(boolean daylight) {
        return daylight ? FU_PER_TICK : 0L;
    }

    public static MachineStatus status(boolean daylight, long stored, int fruitlessPushes) {
        return ThermalLogic.status(daylight, stored, fruitlessPushes);
    }
}
