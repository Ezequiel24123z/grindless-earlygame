package io.github.ezequiel24123z.grindless.machine;

/**
 * The Terrestrial Extractor's numbers and status, independent of a world.
 *
 * <p>T1 workhorse: five times the Crude Extractor's rate, at F1, and only in a surveyed
 * chunk. T0 extraction already stops the player mining by hand; this is the throughput
 * upgrade that feeds a belt line (ADR-0058).
 */
public final class TerrestrialLogic {

    /** Five seconds per unit at richness 1.0 and full power — the rate {@code VeinGenerator} sizes reserve against. */
    public static final int CYCLE_TICKS = 5 * 20;

    /** Draw while working: F1, the Thermal Generator's whole output. */
    public static final long FU_PER_TICK = 32L;

    private TerrestrialLogic() {
    }

    /**
     * What the extractor should show.
     *
     * <p>An unsurveyed chunk is out of band — the machine is fine, the player has not looked
     * yet. After that the Crude Extractor's order applies: stuck, starved, running.
     */
    public static MachineStatus status(boolean surveyed, boolean hasVein, boolean outputStuck,
                                       boolean powered, boolean working) {
        if (!surveyed) {
            return MachineStatus.OUT_OF_BAND;
        }
        return ExtractorLogic.status(hasVein, outputStuck, powered, working);
    }
}
