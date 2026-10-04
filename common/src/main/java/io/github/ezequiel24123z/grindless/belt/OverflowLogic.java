package io.github.ezequiel24123z.grindless.belt;

/**
 * How an overflow gate picks an outlet, independent of a world.
 *
 * <p>Front is preferred. The side (clockwise from facing) only takes the item when the front is
 * backed up. That is the whole gate: a belt that does not stall because a chest filled.
 */
public final class OverflowLogic {

    public static final int FRONT = 0;
    public static final int SIDE = 1;

    private OverflowLogic() {
    }

    /**
     * {@link #FRONT} if the front can take the item, {@link #SIDE} if only the side can,
     * otherwise {@code -1}.
     */
    public static int route(boolean frontOpen, boolean sideOpen) {
        if (frontOpen) {
            return FRONT;
        }
        if (sideOpen) {
            return SIDE;
        }
        return -1;
    }
}
