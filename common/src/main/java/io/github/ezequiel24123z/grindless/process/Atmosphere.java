package io.github.ezequiel24123z.grindless.process;

/**
 * The gas filling a process chamber.
 *
 * <p>Atmosphere is what separates roasting from reduction at the same temperature, and it is the
 * cheapest source of alternative routes in the whole recipe graph: the same ore at 700 °C in
 * {@link #OXIDISING} gives an oxide and SO₂, while at 1200 °C in {@link #REDUCING} it gives metal.
 */
public enum Atmosphere {

    /** Ordinary air. The default, and free. */
    AIR("Air"),

    /** Nitrogen or argon. Prevents oxidation without taking part in the reaction. */
    INERT("Inert"),

    /** Carbon monoxide or hydrogen. Strips oxygen from a compound. */
    REDUCING("Reducing"),

    /** Enriched oxygen. Adds oxygen; the roasting atmosphere. */
    OXIDISING("Oxidising"),

    /** No atmosphere at all. The highest purity available, and its own condition rather than a
     * pressure of zero — a vacuum process needs a chamber that can hold one. */
    VACUUM("Vacuum");

    private final String displayName;

    Atmosphere(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable name, for tooltips and the Process Atlas. */
    public String displayName() {
        return displayName;
    }
}
