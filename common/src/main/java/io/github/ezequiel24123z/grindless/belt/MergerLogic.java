package io.github.ezequiel24123z.grindless.belt;

/**
 * How a merger picks an inlet, independent of a world.
 *
 * <p>Three inlets (back, left, right) feed one outlet. Equal inlets round-robin from the last
 * accepted face so two belts actually take turns rather than the first face starving the others.
 */
public final class MergerLogic {

    public static final int INLETS = 3;
    public static final int BACK = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private MergerLogic() {
    }

    /**
     * Which ready inlet should insert next, or {@code -1} if none is ready.
     *
     * @param ready     length {@link #INLETS}; true when that face has an item waiting
     * @param lastIndex the inlet that last succeeded, or {@code -1} at the start
     */
    public static int pick(boolean[] ready, int lastIndex) {
        if (ready == null || ready.length != INLETS) {
            return -1;
        }
        for (int step = 1; step <= INLETS; step++) {
            int index = Math.floorMod(lastIndex + step, INLETS);
            if (ready[index]) {
                return index;
            }
        }
        return -1;
    }
}
