package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * Cycle math and status for process machines, independent of a world.
 *
 * <p>B1 is six seconds; R1 is twelve. Both draw F1. Condition efficiency scales progress,
 * never yield (ADR-0040).
 */
public final class ProcessLogic {

    public static final int PULVERIZE_TICKS = 20 * 6;
    public static final int REDUCE_TICKS = 20 * 12;
    public static final long FU_PER_TICK = FluxTier.F1.nominal();

    /** R1 carbothermic reduction. */
    public static final double REDUCE_TEMPERATURE = 1500.0;
    public static final String REDUCE_ATMOSPHERE = "REDUCING";

    public static final String CARBON = "grindless:carbon";
    public static final String SLAG = "grindless:slag";
    public static final String CARBON_MONOXIDE = "grindless:carbon_monoxide";

    private ProcessLogic() {
    }

    /**
     * How many ticks of cycle this game tick is worth.
     *
     * <p>{@code timeMultiplier} is below 1.0 when upgrades speed the machine up, matching
     * {@code MachineBlockEntity#timeMultiplier}.
     */
    public static double work(long drawn, long requested, double conditionEfficiency,
                              double timeMultiplier) {
        if (requested <= 0L || drawn <= 0L || conditionEfficiency <= 0.0) {
            return 0.0;
        }
        double speed = timeMultiplier <= 0.0 ? 1.0 : 1.0 / timeMultiplier;
        return ((double) drawn / (double) requested) * conditionEfficiency * speed;
    }

    /**
     * What a process machine should show.
     *
     * <p>Order is the player's question: nothing loaded, conditions wrong, product stuck,
     * missing feed or power, working.
     */
    public static MachineStatus status(boolean hasRecipe, boolean missingInput, boolean outputStuck,
                                       boolean inBand, boolean powered, boolean working) {
        if (!hasRecipe && !missingInput) {
            return MachineStatus.IDLE;
        }
        if (hasRecipe && !inBand) {
            return MachineStatus.OUT_OF_BAND;
        }
        if (outputStuck) {
            return MachineStatus.BLOCKED;
        }
        if (missingInput || !powered) {
            return MachineStatus.STARVED;
        }
        return working ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }
}
