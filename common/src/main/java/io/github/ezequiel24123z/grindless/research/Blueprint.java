package io.github.ezequiel24123z.grindless.research;

/**
 * A researched blueprint. Permanent once unlocked (ADR-0017).
 *
 * <p>T0 machines are never gated. The first research is Voltaic: it will gate T1 crafting when
 * those recipes exist. Unlocking it now is still the real loop — a Data Core and F0 in, a flag
 * out — so a later session attaches recipes to a switch that already flips.
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
