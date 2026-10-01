package io.github.ezequiel24123z.grindless.process;

/**
 * How much the charge is kept moving during a process.
 *
 * <p>Agitation is what makes leaching work at a temperature where a still tank would stall, which
 * is why the hydrometallurgical routes name it and the furnace routes do not.
 */
public enum Agitation {

    /** Undisturbed. The default, and what crystal growth requires. */
    STATIC("Static"),

    /** Mechanically stirred. Liquid-phase chemistry. */
    STIRRED("Stirred"),

    /** Suspended in an upward gas flow. Maximum contact area; roasting and calcining. */
    FLUIDISED("Fluidised");

    private final String displayName;

    Agitation(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable name, for tooltips and the Process Atlas. */
    public String displayName() {
        return displayName;
    }
}
