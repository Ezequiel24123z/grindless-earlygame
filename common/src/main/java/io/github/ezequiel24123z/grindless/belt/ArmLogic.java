package io.github.ezequiel24123z.grindless.belt;

/**
 * Stack and filter arm numbers, independent of a world (ADR-0082).
 *
 * <p>The stack arm moves twelve items in the same one-second cycle the crude arm uses for
 * one. An empty filter passes every item. A set filter is a whitelist of one id. Blacklist
 * is not this slice.
 */
public final class ArmLogic {

    public static final int CYCLE_TICKS = ManipulatorLogic.CYCLE_TICKS;

    /** Up to twelve at once, not twelve times faster. */
    public static final int STACK = 12;

    public static final int FILTER_STACK = 1;

    private ArmLogic() {
    }

    /**
     * Whether {@code itemId} may be picked up.
     *
     * <p>A blank filter is "everything". A set filter matches that id and nothing else.
     */
    public static boolean allows(String filter, String itemId) {
        return filter == null || filter.isBlank() || filter.equals(itemId);
    }
}
