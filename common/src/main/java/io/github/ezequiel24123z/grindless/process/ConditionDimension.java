package io.github.ezequiel24123z.grindless.process;

/**
 * The six physical parameters a process can be sensitive to (ADR-0020).
 *
 * <p>This is the vocabulary shared by recipes and machines: a recipe <em>names</em> the dimensions
 * it requires, and a machine <em>supplies</em> the ones it can hold. The two meet in
 * {@link ProcessConditions#checkEnvelope(ConditionEnvelope)}.
 *
 * <p>Most processes name one or two of these. An unnamed dimension is not a requirement at all —
 * see the omission rule on {@link ProcessConditions}.
 */
public enum ConditionDimension {

    TEMPERATURE("Temperature"),
    PRESSURE("Pressure"),
    ATMOSPHERE("Atmosphere"),
    CATALYST("Catalyst"),
    FIELD("Field"),
    AGITATION("Agitation");

    private final String displayName;

    ConditionDimension(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable name, for tooltips, status readouts and the Process Atlas. */
    public String displayName() {
        return displayName;
    }
}
