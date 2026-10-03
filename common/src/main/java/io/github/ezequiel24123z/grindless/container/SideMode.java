package io.github.ezequiel24123z.grindless.container;

/**
 * What a container face allows, as seen from outside.
 *
 * <p>Per-face I/O is part of the shared container contract, so it works identically on a T1 crate
 * and a T6 colony module.
 */
public enum SideMode {

    /** Nothing may enter or leave through this face. */
    NONE("None", false, false),

    /** Outside may push in, but not pull out. */
    INSERT("Insert", true, false),

    /** Outside may pull out, but not push in. */
    EXTRACT("Extract", false, true),

    /** Both. The default on a plain container. */
    BOTH("Both", true, true);

    private final String displayName;
    private final boolean insert;
    private final boolean extract;

    SideMode(String displayName, boolean insert, boolean extract) {
        this.displayName = displayName;
        this.insert = insert;
        this.extract = extract;
    }

    public String displayName() {
        return displayName;
    }

    public boolean allowsInsert() {
        return insert;
    }

    public boolean allowsExtract() {
        return extract;
    }

    /** Cycles to the next mode, for click-through configuration in a UI. */
    public SideMode cycle() {
        return values()[(ordinal() + 1) % values().length];
    }
}
