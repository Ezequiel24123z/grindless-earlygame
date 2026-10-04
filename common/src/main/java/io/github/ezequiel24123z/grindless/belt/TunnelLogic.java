package io.github.ezequiel24123z.grindless.belt;

/**
 * T1 tunnel pairing, independent of a world.
 *
 * <p>A Conveyor tunnel skips one to five empty blocks (README: range 5). The two tiles face the
 * same way; items always travel in that direction. Adjacent tiles are a belt, not a tunnel.
 */
public final class TunnelLogic {

    /** Empty blocks a T1 pair may skip, inclusive. */
    public static final int RANGE = 5;

    private TunnelLogic() {
    }

    /**
     * Whether {@code steps} from the entrance to the exit (exclusive of the entrance, inclusive
     * of the exit) is a legal pair.
     */
    public static boolean inRange(int steps) {
        int gap = steps - 1;
        return gap >= 1 && gap <= RANGE;
    }

    /** Furthest step along facing to look for a partner, inclusive. */
    public static int maxSteps() {
        return RANGE + 1;
    }
}
