package io.github.ezequiel24123z.grindless.machine;

/**
 * Stops a status from flickering.
 *
 * <p>A machine's raw state changes every few ticks: a dynamo that pushes every fifth tick is
 * "moving power" on one tick and "not" on the next. Writing each change to the world would send a
 * block update to every nearby client several times a second for no information. This holds a
 * status until a different one has been observed for long enough.
 *
 * <p>The holds are asymmetric on purpose. A fault is shown at once, because the player is
 * watching to find out what went wrong. Recovery and going quiet take longer, because showing
 * "fine" for one tick of a flapping fault is worse than showing the fault a moment too long.
 */
public final class StatusDebounce {

    /** Ticks of consistent observation before the machine is shown as idle. */
    public static final int IDLE_HOLD = 40;

    /** Ticks of consistent observation before a fault is cleared. */
    public static final int RECOVER_HOLD = 20;

    private MachineStatus shown = MachineStatus.IDLE;
    private MachineStatus candidate = MachineStatus.IDLE;
    private int streak;

    /** The status currently shown. */
    public MachineStatus shown() {
        return shown;
    }

    /**
     * Feeds one tick's observation.
     *
     * @return whether the shown status changed on this tick
     */
    public boolean observe(MachineStatus observed) {
        if (observed == shown) {
            candidate = shown;
            streak = 0;
            return false;
        }
        if (observed != candidate) {
            candidate = observed;
            streak = 0;
        }
        streak++;
        if (streak >= holdFor(shown, observed)) {
            shown = observed;
            streak = 0;
            return true;
        }
        return false;
    }

    private static int holdFor(MachineStatus from, MachineStatus to) {
        if (to == MachineStatus.IDLE) {
            return IDLE_HOLD;
        }
        if (isFault(from) && !isFault(to)) {
            return RECOVER_HOLD;
        }
        return 1;
    }

    private static boolean isFault(MachineStatus status) {
        return status == MachineStatus.BLOCKED
                || status == MachineStatus.STARVED
                || status == MachineStatus.OUT_OF_BAND;
    }
}
