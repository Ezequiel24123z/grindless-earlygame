package io.github.ezequiel24123z.grindless.research;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;

import java.util.Set;

/**
 * The Research Terminal's numbers and status, independent of a world.
 *
 * <p>Thirty seconds at F0 and one Data Core for Voltaic. Sixty seconds at F0 and one Advanced
 * Data Core for Industrial. Same terminal; you cannot skip (ADR-0057, ADR-0073).
 */
public final class ResearchLogic {

    /** Duration of one Voltaic unlock at full power, in ticks. Thirty seconds. */
    public static final int CYCLE_TICKS = 20 * 30;

    /** Duration of one Industrial unlock at full power, in ticks. Sixty seconds. */
    public static final int INDUSTRIAL_TICKS = 20 * 60;

    /** Draw while working: F0, the same as the extractor, so the first decision is what to power. */
    public static final long FU_PER_TICK = 8L;

    public static final String DATA_CORE = "grindless:data_core";
    public static final String ADVANCED_DATA_CORE = "grindless:advanced_data_core";

    private ResearchLogic() {
    }

    /** Next locked blueprint in enum order, or {@code null} when the tree is done. */
    public static Blueprint next(Set<Blueprint> unlocked) {
        for (Blueprint blueprint : Blueprint.values()) {
            if (unlocked == null || !unlocked.contains(blueprint)) {
                return blueprint;
            }
        }
        return null;
    }

    public static int cycleTicks(Blueprint blueprint) {
        return blueprint == Blueprint.INDUSTRIAL ? INDUSTRIAL_TICKS : CYCLE_TICKS;
    }

    public static String coreId(Blueprint blueprint) {
        return blueprint == Blueprint.INDUSTRIAL ? ADVANCED_DATA_CORE : DATA_CORE;
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
