package io.github.ezequiel24123z.grindless.research;

/**
 * A researched blueprint. Permanent once unlocked (ADR-0017).
 *
 * <p>T0 machines are never gated. Voltaic gates the T1 crafting-table recipes — including
 * Press, dies, Machine Casing and Assembler — via {@code GatedShapedRecipe}. Industrial gates
 * Pylon MK2 on the Assembler (ADR-0073). Pylon MK2 has no crafting-table recipe (ADR-0063).
 */
public enum Blueprint {

    VOLTAIC("voltaic"),
    INDUSTRIAL("industrial");

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
