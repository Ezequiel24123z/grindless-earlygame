package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * The Thermal Generator's numbers, independent of a world.
 *
 * <p>F1, 32 FU/t, from furnace fuel. One coal (1600 ticks) is fifty seconds of F1 — enough to
 * walk away from the crank (ADR-0058).
 */
public final class ThermalLogic {

    public static final long FU_PER_TICK = FluxTier.F1.nominal();

    private ThermalLogic() {
    }

    /** Flux generated this tick while a fuel is burning. */
    public static long generate(boolean burning) {
        return burning ? FU_PER_TICK : 0L;
    }

    /**
     * What the generator should show. Charge with nowhere to go is blocked, matching the dynamo.
     */
    public static MachineStatus status(boolean burning, long stored, int fruitlessPushes) {
        if (burning) {
            return fruitlessPushes >= 2 ? MachineStatus.BLOCKED : MachineStatus.RUNNING;
        }
        return stored > 0L && fruitlessPushes >= 2 ? MachineStatus.BLOCKED : MachineStatus.IDLE;
    }
}
