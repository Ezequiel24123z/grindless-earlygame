package io.github.ezequiel24123z.grindless.process;

/**
 * Why a process will not run.
 *
 * <p>A condition check returns one of these rather than a boolean (ADR-0041). "My factory stopped
 * and I do not know why" is the single most common failure in complex packs, and the fix is better
 * telemetry rather than less complexity — so a failure always names a dimension <em>and</em> a
 * direction. "Too cold" is actionable; "cannot run" is not.
 */
public enum ConditionFault {

    /** No fault; the process runs. */
    NONE(null, "Running"),

    TOO_COLD(ConditionDimension.TEMPERATURE, "Too cold — the reaction will not start"),
    TOO_HOT(ConditionDimension.TEMPERATURE, "Too hot — the product decomposes"),

    PRESSURE_TOO_LOW(ConditionDimension.PRESSURE, "Pressure too low"),
    PRESSURE_TOO_HIGH(ConditionDimension.PRESSURE, "Pressure too high"),

    WRONG_ATMOSPHERE(ConditionDimension.ATMOSPHERE, "Wrong atmosphere"),
    MISSING_CATALYST(ConditionDimension.CATALYST, "Required catalyst is missing"),
    WRONG_FIELD(ConditionDimension.FIELD, "Wrong field"),
    WRONG_AGITATION(ConditionDimension.AGITATION, "Wrong agitation"),

    /** The machine cannot reach this condition at all, at any setting. Distinct from the faults
     * above, which mean the machine <em>could</em> but currently is not — the fix for this one is a
     * chassis upgrade or a different machine, not a dial. */
    OUTSIDE_ENVELOPE(null, "This machine cannot reach the required conditions");

    private final ConditionDimension dimension;
    private final String message;

    ConditionFault(ConditionDimension dimension, String message) {
        this.dimension = dimension;
        this.message = message;
    }

    /** The dimension at fault, or {@code null} for {@link #NONE} and {@link #OUTSIDE_ENVELOPE}. */
    public ConditionDimension dimension() {
        return dimension;
    }

    /** A short explanation suitable for a machine status readout. */
    public String message() {
        return message;
    }

    /** Whether this fault stops the process. */
    public boolean isFault() {
        return this != NONE;
    }
}
