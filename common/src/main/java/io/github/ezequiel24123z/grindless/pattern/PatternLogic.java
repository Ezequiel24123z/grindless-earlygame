package io.github.ezequiel24123z.grindless.pattern;

/**
 * What may be scanned, and what a smash yields (ADR-0088).
 *
 * <p>No Minecraft types. The blacklist tag is applied by the scanner block; this class only
 * refuses a blank id and Matter itself, so Matter cannot be a pattern. Deconstruction is a
 * flat yield, not the inverse of {@link io.github.ezequiel24123z.grindless.recipe.ReplicationCost}.
 */
public final class PatternLogic {

    /** The fungible intermediate. One smash, whatever went in. */
    public static final String MATTER = "grindless:matter";

    /** Items a datapack can keep unique. Empty in the mod; the scanner still refuses Matter. */
    public static final String BLACKLIST = "grindless:replication_blacklist";

    /** Matter produced by one successful smash. */
    public static final int YIELD = 1;

    private PatternLogic() {
    }

    /** {@code true} when the scanner must refuse this id before the tag is consulted. */
    public static boolean refused(String id) {
        return id == null || id.isBlank() || MATTER.equals(id);
    }
}
