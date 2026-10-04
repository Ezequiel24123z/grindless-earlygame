package io.github.ezequiel24123z.grindless.logic;

/**
 * Logic numbers, independent of a world (ADR-0083).
 *
 * <p>The cable carries a plain integer. Redstone only appears at the interface, and only
 * there is the value clipped to 0–15. The controller's comparison is the README example:
 * run while the count is below 500.
 */
public final class SignalLogic {

    public static final int THRESHOLD = 500;

    /** Written onto a cable while the machine may run. */
    public static final int RUN = 1;

    public static final int HOLD = 0;

    private SignalLogic() {
    }

    /** The machine runs while {@code count} is strictly below {@code threshold}. */
    public static boolean shouldRun(int count, int threshold) {
        return count < threshold;
    }

    /** What the interface emits. Zero stays off. Anything above 15 is still a full redstone line. */
    public static int toRedstone(int signal) {
        if (signal <= 0) {
            return 0;
        }
        return Math.min(15, signal);
    }

    /** What a redstone input writes onto the cable. */
    public static int fromRedstone(int power) {
        if (power <= 0) {
            return 0;
        }
        return Math.min(15, power);
    }
}
