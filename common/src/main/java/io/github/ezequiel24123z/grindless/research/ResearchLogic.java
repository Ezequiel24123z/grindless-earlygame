package io.github.ezequiel24123z.grindless.research;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * The Research Terminal's numbers and status, independent of a world.
 *
 * <p>Thirty seconds at F0 and one Data Core for the first blueprint. Long enough that cranking
 * the dynamo is a decision, short enough that the first unlock is the next thing a player does
 * after the extractor starts, not a new grind (ADR-0057).
 */
public final class ResearchLogic {

    /** Duration of one Voltaic unlock at full power, in ticks. Thirty seconds. */
    public static final int CYCLE_TICKS = 20 * 30;

    /** Draw while working: F0, the same as the extractor, so the first decision is what to power. */
    public static final long FU_PER_TICK = 8L;

    private ResearchLogic() {
    }

    /**
     * How many ticks of cycle this game tick is worth.
     *
     * <p>Brownouts slow research; they do not stall it and they do not consume the core early.
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
     * <p>Order is the player's question: nothing to research, already done, no power, working.
     */
    public static MachineStatus status(boolean hasCore, boolean alreadyUnlocked, boolean powered,
                                       boolean working) {
        if (alreadyUnlocked) {
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
