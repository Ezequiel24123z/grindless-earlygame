package io.github.ezequiel24123z.grindless.belt;

/**
 * How a sorter peels matching items off a mixed line, independent of a world.
 *
 * <p>Front is the passthrough. Left and right only take an item whose id matches that face's
 * filter. An empty filter does not steal. A matching face that is backed up holds the item —
 * it does not dump onto the front. That is the difference from the splitter (ADR-0072).
 */
public final class SorterLogic {

    public static final int FRONT = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private SorterLogic() {
    }

    /** Only an explicit id matches. Empty is closed, not "accept all". */
    public static boolean matches(String itemId, String filterId) {
        return filterId != null && !filterId.isEmpty() && filterId.equals(itemId);
    }

    /**
     * Which outlet should take the item, or {@code -1} if every legal outlet is backed up.
     *
     * @param lastIndex the outlet that last succeeded, used only when both sides match
     */
    public static int route(String itemId, String leftFilter, String rightFilter,
                            boolean frontOpen, boolean leftOpen, boolean rightOpen,
                            int lastIndex) {
        boolean leftMatch = matches(itemId, leftFilter);
        boolean rightMatch = matches(itemId, rightFilter);
        if (leftMatch || rightMatch) {
            boolean leftReady = leftMatch && leftOpen;
            boolean rightReady = rightMatch && rightOpen;
            if (!leftReady && !rightReady) {
                return -1;
            }
            if (leftReady && rightReady) {
                return lastIndex == LEFT ? RIGHT : LEFT;
            }
            return leftReady ? LEFT : RIGHT;
        }
        return frontOpen ? FRONT : -1;
    }
}
