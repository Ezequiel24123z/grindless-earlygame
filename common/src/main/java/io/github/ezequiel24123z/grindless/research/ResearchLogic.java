package io.github.ezequiel24123z.grindless.research;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * The Research Terminal's physical calibration cycle, independent of a world.
 *
 * <p>Thirty seconds at F0 turns one Data Core into one Calibrated Data Core. The result is an
 * ordinary item used by recipes; this class never records a player or world permission.
 */
public final class ResearchLogic {

    /** Duration of one calibration at full power, in ticks. Thirty seconds. */
    public static final int CYCLE_TICKS = 20 * 30;

    /** Draw while working: F0, the same as the extractor, so the first decision is what to power. */
    public static final long FU_PER_TICK = 8L;

    public static final String DATA_CORE = "grindless:data_core";
    public static final String CALIBRATED_DATA_CORE = "grindless:calibrated_data_core";

    private ResearchLogic() {
    }

    /**
     * How many ticks of cycle this game tick is worth.
     *
     * <p>Brownouts slow calibration; they do not consume the core early.
     */
    public static double work(long drawn, long requested) {
        if (requested <= 0L || drawn <= 0L) {
            return 0.0;
        }
        return (double) drawn / (double) requested;
    }

    /**
     * What the terminal should show.
     *
     * <p>Order is the player's question: output waiting, no core, no power, working.
     */
    public static MachineStatus status(boolean hasCore, boolean hasOutput, boolean powered,
                                       boolean working) {
        if (hasOutput) {
            return MachineStatus.BLOCKED;
        }
        if (!hasCore) {
            return MachineStatus.IDLE;
        }
        if (!powered) {
            return MachineStatus.STARVED;
        }
        return working ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }
}
