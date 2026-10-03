package io.github.ezequiel24123z.grindless.process;

/**
 * The outcome of checking a recipe's conditions against a machine.
 *
 * <p>Carries enough to drive both the simulation and the status readout: whether the process runs,
 * how fast, and — when it is not running at full speed — which single dimension is holding it back.
 *
 * @param fault      why the process will not run, or {@link ConditionFault#NONE}
 * @param limiting   the dimension setting the speed, or {@code null} when running optimally
 * @param efficiency speed multiplier in {@code [0, 1]}; zero exactly when {@code fault} is set
 */
public record ConditionReport(ConditionFault fault, ConditionDimension limiting, double efficiency) {

    /** Everything is within its optimal band. */
    public static final ConditionReport OPTIMAL =
            new ConditionReport(ConditionFault.NONE, null, 1.0);

    /** A machine that cannot reach the recipe's conditions at any setting. */
    public static ConditionReport outsideEnvelope(ConditionDimension dimension) {
        return new ConditionReport(ConditionFault.OUTSIDE_ENVELOPE, dimension, 0.0);
    }

    /** A process stopped by {@code fault}. */
    public static ConditionReport failed(ConditionFault fault) {
        return new ConditionReport(fault, fault.dimension(), 0.0);
    }

    /** A process running below full speed because of {@code limiting}. */
    public static ConditionReport degraded(ConditionDimension limiting, double efficiency) {
        return new ConditionReport(ConditionFault.NONE, limiting, efficiency);
    }

    /** Whether the process runs at all. */
    public boolean canRun() {
        return !fault.isFault();
    }

    /** Whether the process runs at full speed. */
    public boolean isOptimal() {
        return canRun() && limiting == null;
    }

    /**
     * A one-line status, the kind a machine face or a logic signal would carry.
     *
     * <p>Deliberately specific in the degraded case: a player seeing "running at 62 % — limited by
     * Temperature" knows which dial to touch, which is the entire point of reporting a limiting
     * dimension instead of just a number.
     */
    public String describe() {
        if (fault.isFault()) {
            return fault.message();
        }
        if (isOptimal()) {
            return "Running at full speed";
        }
        return String.format("Running at %.0f %% — limited by %s",
                efficiency * 100.0, limiting.displayName());
    }
}
