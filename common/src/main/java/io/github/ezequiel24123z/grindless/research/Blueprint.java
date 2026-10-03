package io.github.ezequiel24123z.grindless.research;

/**
 * A researched blueprint. Permanent once unlocked (ADR-0017).
 *
 * <p>T0 machines are never gated. Voltaic gates the T1 crafting-table recipes — Thermal
 * Generator, Pulverizer, Arc Furnace and Pylon MK1 — via {@code GatedShapedRecipe}.
 */
public enum Blueprint {

    VOLTAIC("voltaic");

    private final String id;

    Blueprint(String id) {
        this.id = id;
    }

    /** The saved and translated name. */
    public String id() {
        return id;
    }

    public static Blueprint byId(String id) {
        for (Blueprint blueprint : values()) {
            if (blueprint.id.equals(id)) {
                return blueprint;
            }
        }
        return null;
    }
}
