package io.github.ezequiel24123z.grindless.process;

/**
 * An electric or magnetic field applied across a process.
 *
 * <p>Named {@code ProcessField} rather than {@code Field} so it never reads ambiguously next to
 * reflection code; the design documents call this dimension simply "field".
 */
public enum ProcessField {

    /** No field. The default. */
    NONE("None"),

    /** A current through the charge. Electrowinning, electrolysis, arc melting. */
    ELECTRIC("Electric"),

    /** Magnetic confinement or separation. Plasma containment and fusion. */
    MAGNETIC("Magnetic");

    private final String displayName;

    ProcessField(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable name, for tooltips and the Process Atlas. */
    public String displayName() {
        return displayName;
    }
}
