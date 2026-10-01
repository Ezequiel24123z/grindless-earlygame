package io.github.ezequiel24123z.grindless.container;

/**
 * What a container with auto-void enabled is allowed to discard.
 *
 * <p>Auto-void is the difference between an ore line that jams overnight and one that runs for a
 * month unattended. It is also the easiest way for a player to silently destroy something they
 * wanted, so it is off by default on every container, always (ADR-0018).
 *
 * <p>It matters more here than in most mods: a system that generates recipes from tags at runtime
 * produces byproducts for materials the player has never heard of, and unwanted byproducts backing
 * up a line is the characteristic failure of that design.
 */
public enum VoidMode {

    /**
     * Discard only what exceeds the threshold. The safe mode, and the default when voiding is
     * switched on.
     *
     * <p>This is the mode that makes "it trims, it never empties" true: the container keeps its
     * buffer and sheds only the surplus.
     */
    OVERFLOW("Overflow only"),

    /**
     * Discard overflow of filtered materials only, leaving anything unfiltered alone.
     *
     * <p>For a container that deliberately accepts a mixed stream and wants to shed one known
     * component of it.
     */
    FILTERED("Filtered materials only"),

    /**
     * Discard everything above the threshold regardless of filter.
     *
     * <p>The dangerous one, and the reason enabling voiding is an explicit confirmation rather
     * than a stray click.
     */
    EVERYTHING("Everything");

    private final String displayName;

    VoidMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
