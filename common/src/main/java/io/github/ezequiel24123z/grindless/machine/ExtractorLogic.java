package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.material.MaterialForm;

/**
 * The Crude Extractor's numbers and status, independent of a world.
 *
 * <p>Twenty seconds per unit at F0 is the T0 bootstrap in {@code PROCESSES.md}: slow enough that
 * a hand crank is a decision, fast enough that the grind of mining by hand is already over.
 * Vein richness and depletion scale that duration; they never stop a vein outright
 * (ADR-0047, ADR-0053).
 */
public final class ExtractorLogic {

    /** Duration of one unit at richness 1.0 and full power, in ticks. Twenty seconds. */
    public static final int CYCLE_TICKS = 20 * 20;

    /** Draw while working: F0, which is also the dynamo's whole output. */
    public static final long FU_PER_TICK = 8L;

    private ExtractorLogic() {
    }

    /**
     * How many ticks of cycle this game tick is worth.
     *
     * <p>At rate 1 and a full draw this is 1, so a cycle takes {@link #CYCLE_TICKS}. Fractional
     * rates accumulate: a vein at 0.5 richness takes twice as long rather than rounding up to
     * full speed.
     */
    public static double work(double veinRate, long drawn, long requested) {
        if (requested <= 0L || drawn <= 0L || veinRate <= 0.0) {
            return 0.0;
        }
        return veinRate * ((double) drawn / (double) requested);
    }

    /**
     * What the extractor should show.
     *
     * <p>Order is the player's question: nothing to mine, product stuck, no power, working. A
     * completed cycle that cannot leave the machine is {@link MachineStatus#BLOCKED} even if the
     * power has since gone, because the fix is to empty it, not to crank more.
     */
    public static MachineStatus status(boolean hasVein, boolean outputStuck, boolean powered,
                                       boolean working) {
        if (!hasVein) {
            return MachineStatus.IDLE;
        }
        if (outputStuck) {
            return MachineStatus.BLOCKED;
        }
        if (!powered) {
            return MachineStatus.STARVED;
        }
        return working ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }

    /**
     * The form a vein yields. Raw if the pack has it, otherwise the ore block, which is what
     * "1 u raw &lt;chunk material&gt;" means when the pack has no raw form.
     */
    public static MaterialForm outputForm(boolean hasRaw, boolean hasOre) {
        return hasRaw ? MaterialForm.RAW : MaterialForm.ORE;
    }
}
